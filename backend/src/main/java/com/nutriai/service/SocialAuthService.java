package com.nutriai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriai.dto.auth.LoginResponse;
import com.nutriai.dto.auth.SocialBindInfo;
import com.nutriai.entity.User;
import com.nutriai.exception.BusinessException;
import com.nutriai.repository.UserRepository;
import com.nutriai.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

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

    // 微信开放平台（网页应用，区别于小程序）
    @Value("${social.wechat.app-id:}")
    private String wxWebAppId;

    @Value("${social.wechat.app-secret:}")
    private String wxWebAppSecret;

    // QQ互联
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

    /**
     * 获取QQ授权URL
     */
    public String getQqAuthUrl(String state) {
        if (qqAppId == null || qqAppId.isBlank()) {
            throw new BusinessException(500, "QQ互联配置未完成");
        }
        return String.format(
            "https://graph.qq.com/oauth2.0/authorize?response_type=code&client_id=%s&redirect_uri=%s&state=%s&scope=get_user_info",
            qqAppId, qqRedirectUri, state
        );
    }

    /**
     * QQ登录：用code换取用户信息并登录/注册
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public LoginResponse qqLogin(String code, String ipAddress) {
        if (qqAppId == null || qqAppId.isBlank()) {
            throw new BusinessException(500, "QQ互联配置未完成，请联系管理员");
        }

        // 1. code换取access_token
        String tokenUrl = String.format(
            "https://graph.qq.com/oauth2.0/token?grant_type=authorization_code&client_id=%s&client_secret=%s&code=%s&redirect_uri=%s&fmt=json",
            qqAppId, qqAppKey, code, qqRedirectUri
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
            accessToken, qqAppId, openId
        );
        Map<String, Object> userInfoResp = callApi(userInfoUrl, "QQ用户信息");
        String nickname = (String) userInfoResp.getOrDefault("nickname", "QQ用户");
        String avatar = (String) userInfoResp.getOrDefault("figureurl_qq_2", "");

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
            log.info("QQ用户自动注册: openId={}, username={}", openId, autoUsername);
        }

        return buildLoginResponse(user, ipAddress);
    }

    // ==================== 账号绑定 ====================

    /**
     * 获取当前用户的社交账号绑定状态
     */
    public SocialBindInfo getBindInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(BusinessException.Auth::userNotFound);
        return SocialBindInfo.builder()
                .wechatBound(user.getWxOpenId() != null && !user.getWxOpenId().isBlank())
                .wechatNickname(user.getWxOpenId() != null ? "已绑定" : null)
                .qqBound(user.getQqOpenId() != null && !user.getQqOpenId().isBlank())
                .qqNickname(user.getQqOpenId() != null ? "已绑定" : null)
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
    public void bindQq(Long userId, String code) {
        if (qqAppId == null || qqAppId.isBlank()) {
            throw new BusinessException(500, "QQ互联配置未完成");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(BusinessException.Auth::userNotFound);

        if (user.getQqOpenId() != null && !user.getQqOpenId().isBlank()) {
            throw new BusinessException(400, "已绑定QQ账号，请先解绑");
        }

        // 用code换取access_token
        String tokenUrl = String.format(
            "https://graph.qq.com/oauth2.0/token?grant_type=authorization_code&client_id=%s&client_secret=%s&code=%s&redirect_uri=%s&fmt=json",
            qqAppId, qqAppKey, code, qqRedirectUri
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

        if (userRepository.findByQqOpenId(openId).isPresent()) {
            throw new BusinessException(400, "该QQ号已绑定其他账号，请先注销该账号后再绑定");
        }

        user.setQqOpenId(openId);
        userRepository.save(user);
        log.info("用户绑定QQ成功: userId={}, openId={}", userId, openId);
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
        boolean hasQq = !"qq".equals(unbinding) && user.getQqOpenId() != null && !user.getQqOpenId().isBlank();

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
}
