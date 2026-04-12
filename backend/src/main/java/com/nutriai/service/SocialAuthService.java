package com.nutriai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriai.dto.auth.LoginResponse;
import com.nutriai.dto.auth.SocialBindInfo;
import com.nutriai.entity.User;
import com.nutriai.exception.BusinessException;
import com.nutriai.repository.UserRepository;
import com.nutriai.service.DynamicConfigService;
import com.nutriai.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 第三方社交登录服务
 * 支持微信开放平台(网页/H5)和QQ互联登录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialAuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final DynamicConfigService dynamicConfigService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final EmailService emailService;

    private static final String MERGE_CODE_PREFIX = "merge:email:";
    private static final String MERGE_RATE_PREFIX = "merge:rate:";
    private static final int MERGE_CODE_EXPIRE_MINUTES = 5;

    // 微信开放平台（网页应用，区别于小程序）
    @Value("${social.wechat.app-id:}")
    private String wxWebAppId;

    @Value("${social.wechat.app-secret:}")
    private String wxWebAppSecret;

    // QQ互联 - Web端 (网站应用)
    @Value("${social.qq.web-id:}")
    private String qqWebId;

    @Value("${social.qq.web-key:}")
    private String qqWebKey;

    // QQ互联 - APP端 (移动应用)
    @Value("${social.qq.app-id:}")
    private String qqAppId;

    @Value("${social.qq.app-key:}")
    private String qqAppKey;

    @Value("${social.qq.redirect-uri:}")
    private String qqRedirectUri;

    @Value("${social.wechat.redirect-uri:}")
    private String wxRedirectUri;

    private final RestTemplate restTemplate = new RestTemplate();

    // ==================== 微信网页OAuth2登录 ====================

    /**
     * 获取微信网页授权URL（前端跳转用）
     */
    public String getWechatAuthUrl(String state) {
        if (wxWebAppId == null || wxWebAppId.isBlank()) {
            throw new BusinessException(500, "微信开放平台配置未完成");
        }
        return String.format(
            "https://open.weixin.qq.com/connect/qrconnect?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_login&state=%s#wechat_redirect",
            wxWebAppId, wxRedirectUri, state
        );
    }

    /**
     * 微信网页登录：用code换取用户信息并登录/注册
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public LoginResponse wechatLogin(String code, String ipAddress) {
        if (wxWebAppId == null || wxWebAppId.isBlank()) {
            throw new BusinessException(500, "微信开放平台配置未完成，请联系管理员");
        }

        // 1. code换取access_token
        String tokenUrl = String.format(
            "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code",
            wxWebAppId, wxWebAppSecret, code
        );
        Map<String, Object> tokenResp = callApi(tokenUrl, "微信");

        String accessToken = (String) tokenResp.get("access_token");
        String openId = (String) tokenResp.get("openid");
        if (openId == null || openId.isBlank()) {
            throw new BusinessException(500, "微信登录失败: 无法获取openid");
        }

        // 2. 获取用户信息
        String userInfoUrl = String.format(
            "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s&lang=zh_CN",
            accessToken, openId
        );
        Map<String, Object> userInfoResp = callApi(userInfoUrl, "微信用户信息");
        String nickname = (String) userInfoResp.getOrDefault("nickname", "微信用户");
        String avatar = (String) userInfoResp.getOrDefault("headimgurl", "");

        // 3. 查找或创建用户
        User user = userRepository.findByWxOpenId(openId).orElse(null);
        if (user == null) {
            String autoUsername = "wx_" + UUID.randomUUID().toString().substring(0, 12);
            user = User.builder()
                    .username(autoUsername)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .wxOpenId(openId)
                    .nickname(nickname)
                    .avatar(avatar.isEmpty() ? null : avatar)
                    .role("USER")
                    .status("ACTIVE")
                    .build();
            userRepository.save(user);
            log.info("微信用户自动注册: openId={}, username={}", openId, autoUsername);
        }

        return buildLoginResponse(user, ipAddress);
    }

    // ==================== QQ OAuth2登录 ====================

    /** Web端使用网站应用凭据 */
    private String getQqWebId() {
        return dynamicConfigService.getString("QQ_WEB_ID", qqWebId);
    }

    private String getQqWebKey() {
        return dynamicConfigService.getString("QQ_WEB_KEY", qqWebKey);
    }

    /** APP端使用移动应用凭据 */
    private String getQqAppId() {
        return dynamicConfigService.getString("QQ_APP_ID", qqAppId);
    }

    private String getQqAppKey() {
        return dynamicConfigService.getString("QQ_APP_KEY", qqAppKey);
    }

    /**
     * 获取QQ授权URL
     */
    public String getQqAuthUrl(String state) {
        String clientId = getQqWebId();
        if (clientId == null || clientId.isBlank()) {
            throw new BusinessException(500, "QQ互联配置未完成");
        }
        String encodedRedirect = URLEncoder.encode(qqRedirectUri, StandardCharsets.UTF_8);
        // display=pc for web, display=mobile for H5/APP
        String display = (state != null && (state.startsWith("h5_") || state.startsWith("app_"))) ? "mobile" : "pc";
        return String.format(
            "https://graph.qq.com/oauth2.0/authorize?response_type=code&client_id=%s&redirect_uri=%s&state=%s&scope=get_user_info&display=%s",
            clientId, encodedRedirect, state, display
        );
    }

    /**
     * QQ登录：用code换取用户信息并登录/注册
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public LoginResponse qqLogin(String code, String ipAddress, String state) {
        String clientId = getQqWebId();
        String clientSecret = getQqWebKey();
        if (clientId == null || clientId.isBlank()) {
            throw new BusinessException(500, "QQ互联配置未完成，请联系管理员");
        }

        // 1. code换取access_token
        String tokenUrl = String.format(
            "https://graph.qq.com/oauth2.0/token?grant_type=authorization_code&client_id=%s&client_secret=%s&code=%s&redirect_uri=%s&fmt=json",
            clientId, clientSecret, code, URLEncoder.encode(qqRedirectUri, StandardCharsets.UTF_8)
        );
        Map<String, Object> tokenResp = callApi(tokenUrl, "QQ");
        String accessToken = (String) tokenResp.get("access_token");
        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException(500, "QQ登录失败: 无法获取access_token");
        }

        // 2. 获取OpenID
        String openIdUrl = String.format(
            "https://graph.qq.com/oauth2.0/me?access_token=%s&fmt=json", accessToken
        );
        Map<String, Object> openIdResp = callApi(openIdUrl, "QQ OpenID");
        String openId = (String) openIdResp.get("openid");
        if (openId == null || openId.isBlank()) {
            throw new BusinessException(500, "QQ登录失败: 无法获取openid");
        }

        // 3. 获取用户信息
        String userInfoUrl = String.format(
            "https://graph.qq.com/user/get_user_info?access_token=%s&oauth_consumer_key=%s&openid=%s",
            accessToken, clientId, openId
        );
        Map<String, Object> userInfoResp = callApi(userInfoUrl, "QQ用户信息");
        String nickname = (String) userInfoResp.getOrDefault("nickname", "QQ用户");
        String avatar = (String) userInfoResp.getOrDefault("figureurl_qq_2", "");
        if (avatar != null && avatar.startsWith("http://")) {
            avatar = avatar.replaceFirst("http://", "https://");
        }

        // 4. 查找或创建用户
        User user = userRepository.findByQqOpenId(openId).orElse(null);
        if (user == null) {
            String autoUsername = "qq_" + UUID.randomUUID().toString().substring(0, 12);
            user = User.builder()
                    .username(autoUsername)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .qqOpenId(openId)
                    .nickname(nickname)
                    .avatar(avatar.isEmpty() ? null : avatar)
                    .role("USER")
                    .status("ACTIVE")
                    .build();
            userRepository.save(user);
            log.info("QQ用户自动注册: openId={}, username={}, platform={}", openId, autoUsername, state);
        } else {
            log.info("QQ用户登录: openId={}, userId={}, username={}, platform={}", openId, user.getId(), user.getUsername(), state);
        }

        return buildLoginResponse(user, ipAddress);
    }

    /**
     * QQ APP原生SDK登录（使用access_token而非code）
     * 使用QQ_APP_ID（移动应用），openId存储在qq_app_open_id字段
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public LoginResponse qqTokenLogin(String accessToken, String ipAddress) {
        // 1. 用access_token获取OpenID
        String openIdUrl = String.format(
            "https://graph.qq.com/oauth2.0/me?access_token=%s&fmt=json", accessToken
        );
        Map<String, Object> openIdResp = callApi(openIdUrl, "QQ APP OpenID");
        String openId = (String) openIdResp.get("openid");
        if (openId == null || openId.isBlank()) {
            throw new BusinessException(500, "QQ登录失败: 无法获取openid");
        }
        String clientId = (String) openIdResp.get("client_id");

        // 2. 获取用户信息
        String consumerKey = (clientId != null && !clientId.isBlank()) ? clientId : getQqAppId();
        String userInfoUrl = String.format(
            "https://graph.qq.com/user/get_user_info?access_token=%s&oauth_consumer_key=%s&openid=%s",
            accessToken, consumerKey, openId
        );
        Map<String, Object> userInfoResp = callApi(userInfoUrl, "QQ APP用户信息");
        String nickname = (String) userInfoResp.getOrDefault("nickname", "QQ用户");
        String avatar = (String) userInfoResp.getOrDefault("figureurl_qq_2", "");
        if (avatar != null && avatar.startsWith("http://")) {
            avatar = avatar.replaceFirst("http://", "https://");
        }

        // 3. 先按APP openId查找，再按Web openId查找（双校验）
        User user = userRepository.findByQqAppOpenId(openId).orElse(null);
        if (user != null) {
            log.info("QQ APP登录（APP openId匹配）: openId={}, userId={}, username={}", openId, user.getId(), user.getUsername());
            return buildLoginResponse(user, ipAddress);
        }

        // APP openId未匹配，尝试按web openId查找（不太可能匹配，但作为兜底）
        user = userRepository.findByQqOpenId(openId).orElse(null);
        if (user != null) {
            // 同时存储APP openId
            user.setQqAppOpenId(openId);
            userRepository.save(user);
            log.info("QQ APP登录（Web openId匹配，补充存储APP openId）: openId={}, userId={}", openId, user.getId());
            return buildLoginResponse(user, ipAddress);
        }

        // 4. 未找到用户，注册新用户
        String autoUsername = "qq_" + UUID.randomUUID().toString().substring(0, 12);
        user = User.builder()
                .username(autoUsername)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .qqAppOpenId(openId)
                .nickname(nickname)
                .avatar(avatar.isEmpty() ? null : avatar)
                .role("USER")
                .status("ACTIVE")
                .build();
        userRepository.save(user);
        log.info("QQ APP原生登录注册: appOpenId={}, username={}", openId, autoUsername);

        return buildLoginResponse(user, ipAddress);
    }

    /**
     * QQ APP原生SDK绑定（使用access_token）
     * 存储到qq_app_open_id字段
     * 也处理"补充APP openId"场景（已有web openId，缺app openId）
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public void qqTokenBind(Long userId, String accessToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(BusinessException.Auth::userNotFound);

        if (user.getQqAppOpenId() != null && !user.getQqAppOpenId().isBlank()) {
            throw new BusinessException(400, "APP端QQ已验证，无需重复操作");
        }

        // 用access_token获取OpenID
        String openIdUrl = String.format(
            "https://graph.qq.com/oauth2.0/me?access_token=%s&fmt=json", accessToken
        );
        Map<String, Object> openIdResp = callApi(openIdUrl, "QQ APP绑定OpenID");
        String openId = (String) openIdResp.get("openid");
        if (openId == null || openId.isBlank()) {
            throw new BusinessException(500, "获取QQ openid失败");
        }

        // 检查此APP openId是否已被其他用户使用
        var existingUser = userRepository.findByQqAppOpenId(openId);
        if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
            throw new BusinessException(400, "该QQ号已绑定其他账号，请先注销该账号后再绑定");
        }

        user.setQqAppOpenId(openId);
        userRepository.save(user);

        boolean wasSupplementary = user.getQqOpenId() != null && !user.getQqOpenId().isBlank();
        if (wasSupplementary) {
            log.info("用户补充APP端QQ验证: userId={}, appOpenId={}", userId, openId);
        } else {
            log.info("用户APP端绑定QQ: userId={}, appOpenId={}", userId, openId);
        }
    }

    // ==================== 账号绑定 ====================

    /**
     * 获取当前用户的社交账号绑定状态
     */
    public SocialBindInfo getBindInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(BusinessException.Auth::userNotFound);
        boolean hasWebQq = user.getQqOpenId() != null && !user.getQqOpenId().isBlank();
        boolean hasAppQq = user.getQqAppOpenId() != null && !user.getQqAppOpenId().isBlank();
        boolean qqBound = hasWebQq || hasAppQq;
        // 缺少对应端openId时提示验证，包括两端都没有的情况
        boolean qqNeedAppVerify = !hasAppQq;
        boolean qqNeedWebVerify = !hasWebQq;
        return SocialBindInfo.builder()
                .wechatBound(user.getWxOpenId() != null && !user.getWxOpenId().isBlank())
                .wechatNickname(user.getWxOpenId() != null ? "已绑定" : null)
                .qqBound(qqBound)
                .qqNickname(qqBound ? "已绑定" : null)
                .qqNeedAppVerify(qqNeedAppVerify)
                .qqNeedWebVerify(qqNeedWebVerify)
                .build();
    }

    /**
     * 绑定微信账号
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public void bindWechat(Long userId, String code) {
        if (wxWebAppId == null || wxWebAppId.isBlank()) {
            throw new BusinessException(500, "微信开放平台配置未完成");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(BusinessException.Auth::userNotFound);

        if (user.getWxOpenId() != null && !user.getWxOpenId().isBlank()) {
            throw new BusinessException(400, "已绑定微信账号，请先解绑");
        }

        // 用code换openid
        String tokenUrl = String.format(
            "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code",
            wxWebAppId, wxWebAppSecret, code
        );
        Map<String, Object> tokenResp = callApi(tokenUrl, "微信绑定");
        String openId = (String) tokenResp.get("openid");
        if (openId == null || openId.isBlank()) {
            throw new BusinessException(500, "获取微信openid失败");
        }

        // 检查是否已被其他账号绑定
        if (userRepository.findByWxOpenId(openId).isPresent()) {
            throw new BusinessException(400, "该微信号已绑定其他账号，请先注销该账号后再绑定");
        }

        user.setWxOpenId(openId);
        userRepository.save(user);
        log.info("用户绑定微信成功: userId={}, openId={}", userId, openId);
    }

    /**
     * 绑定QQ账号
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public void bindQq(Long userId, String code, String state) {
        String clientId = getQqWebId();
        String clientSecret = getQqWebKey();
        if (clientId == null || clientId.isBlank()) {
            throw new BusinessException(500, "QQ互联配置未完成");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(BusinessException.Auth::userNotFound);

        if (user.getQqOpenId() != null && !user.getQqOpenId().isBlank()) {
            throw new BusinessException(400, "Web端QQ已验证，无需重复操作");
        }

        // 用code换取access_token
        String tokenUrl = String.format(
            "https://graph.qq.com/oauth2.0/token?grant_type=authorization_code&client_id=%s&client_secret=%s&code=%s&redirect_uri=%s&fmt=json",
            clientId, clientSecret, code, URLEncoder.encode(qqRedirectUri, StandardCharsets.UTF_8)
        );
        Map<String, Object> tokenResp = callApi(tokenUrl, "QQ绑定");
        String accessToken = (String) tokenResp.get("access_token");

        // 获取OpenID
        String openIdUrl = String.format(
            "https://graph.qq.com/oauth2.0/me?access_token=%s&fmt=json", accessToken
        );
        Map<String, Object> openIdResp = callApi(openIdUrl, "QQ绑定OpenID");
        String openId = (String) openIdResp.get("openid");
        if (openId == null || openId.isBlank()) {
            throw new BusinessException(500, "获取QQ openid失败");
        }

        // 检查此web openId是否已被其他用户使用
        var existingUser = userRepository.findByQqOpenId(openId);
        if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
            throw new BusinessException(400, "该QQ号已绑定其他账号，请先注销该账号后再绑定");
        }

        user.setQqOpenId(openId);
        userRepository.save(user);

        boolean wasSupplementary = user.getQqAppOpenId() != null && !user.getQqAppOpenId().isBlank();
        if (wasSupplementary) {
            log.info("用户补充Web端QQ验证: userId={}, webOpenId={}", userId, openId);
        } else {
            log.info("用户绑定QQ成功: userId={}, openId={}", userId, openId);
        }
    }

    /**
     * 解绑微信
     */
    @Transactional
    public void unbindWechat(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(BusinessException.Auth::userNotFound);
        ensureHasOtherLogin(user, "wechat");
        user.setWxOpenId(null);
        userRepository.save(user);
        log.info("用户解绑微信: userId={}", userId);
    }

    /**
     * 解绑QQ
     */
    @Transactional
    public void unbindQq(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(BusinessException.Auth::userNotFound);
        ensureHasOtherLogin(user, "qq");
        user.setQqOpenId(null);
        user.setQqAppOpenId(null);
        userRepository.save(user);
        log.info("用户解绑QQ: userId={}", userId);
    }

    // ==================== 工具方法 ====================

    /**
     * 确保用户至少有一种登录方式（防止全部解绑后无法登录）
     */
    private void ensureHasOtherLogin(User user, String unbinding) {
        boolean hasEmail = user.getEmail() != null && !user.getEmail().isBlank();
        boolean hasWechat = !"wechat".equals(unbinding) && user.getWxOpenId() != null && !user.getWxOpenId().isBlank();
        boolean hasQq = !"qq".equals(unbinding) && (
            (user.getQqOpenId() != null && !user.getQqOpenId().isBlank()) ||
            (user.getQqAppOpenId() != null && !user.getQqAppOpenId().isBlank())
        );

        if (!hasEmail && !hasWechat && !hasQq) {
            throw new BusinessException(400, "解绑失败：至少需要保留一种登录方式（邮箱/微信/QQ）");
        }
    }

    /**
     * 构建登录响应
     */
    private LoginResponse buildLoginResponse(User user, String ipAddress) {
        if ("BANNED".equalsIgnoreCase(user.getStatus())) {
            throw BusinessException.Auth.accountBanned();
        }

        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ipAddress);
        userRepository.save(user);

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpiration() / 1000)
                .userInfo(LoginResponse.UserInfo.fromUser(user))
                .build();
    }

    /**
     * 调用第三方API并解析JSON响应
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> callApi(String url, String label) {
        try {
            String body = restTemplate.getForObject(url, String.class);
            Map<String, Object> result = objectMapper.readValue(body, Map.class);

            // 检查微信错误
            if (result.containsKey("errcode") && !Integer.valueOf(0).equals(result.get("errcode"))) {
                String errMsg = String.valueOf(result.get("errmsg"));
                log.warn("{}接口错误: errcode={}, errmsg={}", label, result.get("errcode"), errMsg);
                throw new BusinessException(500, label + "登录失败: " + errMsg);
            }
            // 检查QQ错误
            if (result.containsKey("error") && !Integer.valueOf(0).equals(result.get("error"))) {
                String errMsg = String.valueOf(result.get("error_description"));
                log.warn("{}接口错误: {}", label, errMsg);
                throw new BusinessException(500, label + "登录失败: " + errMsg);
            }

            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("{}接口调用失败: url={}", label, url, e);
            throw new BusinessException(500, label + "登录服务暂时不可用");
        }
    }

    // ==================== 账号合并 ====================

    /**
     * 发送合并验证码到目标邮箱
     * 当前用户（QQ注册的新账号）要合并到已有邮箱账号
     */
    public void sendMergeCode(Long currentUserId, String targetEmail) {
        // 当前用户必须是QQ注册的（无邮箱）
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(BusinessException.Auth::userNotFound);
        if (currentUser.getEmail() != null && !currentUser.getEmail().isBlank()) {
            throw new BusinessException(400, "您已绑定邮箱，无需合并");
        }

        // 目标邮箱必须已被占用
        var targetOpt = userRepository.findByEmail(targetEmail);
        if (targetOpt.isEmpty()) {
            throw new BusinessException(400, "该邮箱未注册，请直接绑定邮箱");
        }

        // 频率限制
        String rateLimitKey = MERGE_RATE_PREFIX + targetEmail;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(rateLimitKey))) {
            throw new BusinessException(429, "发送过于频繁，请1分钟后重试");
        }

        // 生成验证码
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append((int) (Math.random() * 10));
        }
        String mergeCode = code.toString();

        String redisKey = MERGE_CODE_PREFIX + targetEmail;
        redisTemplate.opsForValue().set(redisKey, mergeCode, MERGE_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(rateLimitKey, "1", 1, TimeUnit.MINUTES);

        try {
            emailService.sendMergeCode(targetEmail, currentUser.getNickname() != null ? currentUser.getNickname() : currentUser.getUsername(), mergeCode);
            log.info("账号合并验证码已发送: targetEmail={}, currentUserId={}", targetEmail, currentUserId);
        } catch (Exception e) {
            log.error("发送合并验证码失败: targetEmail={}", targetEmail, e);
            redisTemplate.delete(redisKey);
            throw new BusinessException(500, "验证码发送失败，请稍后重试");
        }
    }

    /**
     * 执行账号合并：将当前QQ账号的社交绑定迁移到目标邮箱账号
     * 当前账号将被删除
     */
    @Transactional
    public LoginResponse mergeToEmail(Long currentUserId, String targetEmail, String code, String ipAddress) {
        // 验证code
        String redisKey = MERGE_CODE_PREFIX + targetEmail;
        String storedCode = (String) redisTemplate.opsForValue().get(redisKey);
        if (storedCode == null || !storedCode.equals(code)) {
            throw new BusinessException(400, "验证码错误或已过期");
        }
        redisTemplate.delete(redisKey);

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(BusinessException.Auth::userNotFound);
        User targetUser = userRepository.findByEmail(targetEmail)
                .orElseThrow(() -> new BusinessException(400, "目标邮箱账号不存在"));

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new BusinessException(400, "不能合并到自己的账号");
        }

        // 迁移QQ绑定信息到目标账号
        if (currentUser.getQqOpenId() != null && !currentUser.getQqOpenId().isBlank()) {
            if (targetUser.getQqOpenId() == null || targetUser.getQqOpenId().isBlank()) {
                targetUser.setQqOpenId(currentUser.getQqOpenId());
            }
        }
        if (currentUser.getQqAppOpenId() != null && !currentUser.getQqAppOpenId().isBlank()) {
            if (targetUser.getQqAppOpenId() == null || targetUser.getQqAppOpenId().isBlank()) {
                targetUser.setQqAppOpenId(currentUser.getQqAppOpenId());
            }
        }
        if (currentUser.getWxOpenId() != null && !currentUser.getWxOpenId().isBlank()) {
            if (targetUser.getWxOpenId() == null || targetUser.getWxOpenId().isBlank()) {
                targetUser.setWxOpenId(currentUser.getWxOpenId());
            }
        }

        // 清除当前用户的社交绑定再删除（避免唯一索引冲突）
        currentUser.setQqOpenId(null);
        currentUser.setQqAppOpenId(null);
        currentUser.setWxOpenId(null);
        userRepository.save(currentUser);
        userRepository.flush();

        // 保存目标用户
        userRepository.save(targetUser);

        // 删除当前QQ注册的临时账号
        userRepository.delete(currentUser);

        log.info("账号合并成功: sourceUserId={} -> targetUserId={}, targetEmail={}", currentUserId, targetUser.getId(), targetEmail);

        return buildLoginResponse(targetUser, ipAddress);
    }

    /**
     * 绑定邮箱（当前用户没有邮箱，给QQ注册的账号绑定邮箱）
     */
    @Transactional
    public void bindEmail(Long userId, String email, String code) {
        // 验证code (使用注册验证码前缀)
        String redisKey = "register:email:" + email;
        String storedCode = (String) redisTemplate.opsForValue().get(redisKey);
        if (storedCode == null || !storedCode.equals(code)) {
            throw new BusinessException(400, "验证码错误或已过期");
        }
        redisTemplate.delete(redisKey);

        User user = userRepository.findById(userId)
                .orElseThrow(BusinessException.Auth::userNotFound);

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            throw new BusinessException(400, "已绑定邮箱");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException(409, "该邮箱已被其他账号使用，可选择账号合并");
        }

        user.setEmail(email);
        userRepository.save(user);
        log.info("用户绑定邮箱成功: userId={}, email={}", userId, email);
    }
}
