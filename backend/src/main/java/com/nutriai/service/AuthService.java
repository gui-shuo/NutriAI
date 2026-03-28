package com.nutriai.service;

import com.nutriai.dto.auth.*;
import com.nutriai.entity.User;
import com.nutriai.exception.BusinessException;
import com.nutriai.repository.UserRepository;
import com.nutriai.util.CaptchaUtil;
import com.nutriai.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final CaptchaUtil captchaUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Lazy
    @Autowired
    private MemberService memberService;
    
    @Value("${wechat.miniapp.appid:}")
    private String wxAppId;
    
    @Value("${wechat.miniapp.secret:}")
    private String wxAppSecret;
    
    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final String LOGIN_FAIL_PREFIX = "login:fail:";
    private static final String RESET_CODE_PREFIX = "reset:code:";
    private static final String EMAIL_CODE_PREFIX = "register:email:";
    private static final String EMAIL_RATE_PREFIX = "register:rate:";
    private static final int MAX_LOGIN_FAIL = 3;
    private static final int CAPTCHA_EXPIRE_MINUTES = 5;
    private static final int LOGIN_FAIL_EXPIRE_MINUTES = 30;
    private static final int RESET_CODE_EXPIRE_MINUTES = 15;
    private static final int RESET_CODE_LENGTH = 6;
    private static final int EMAIL_CODE_EXPIRE_MINUTES = 5;
    
    /**
     * 用户注册
     */
    @Transactional
    public void register(RegisterRequest request) {
        // 1. 验证两次密码是否一致
        if (!request.isPasswordMatch()) {
            throw BusinessException.Auth.passwordMismatch();
        }
        
        // 2. 验证码校验
        validateCaptcha(request.getCaptchaKey(), request.getCaptcha());
        
        // 3. 邮箱验证码校验
        validateEmailCode(request.getEmail(), request.getEmailCode());
        
        // 4. 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw BusinessException.Auth.usernameExists();
        }
        
        // 5. 检查邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            throw BusinessException.Auth.emailExists();
        }
        
        // 6. 检查手机号是否已存在（如果提供）
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw BusinessException.Auth.phoneExists();
            }
        }
        
        // 7. 创建用户
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .phone(request.getPhone())
                .nickname(request.getNickname() != null ? request.getNickname() : request.getUsername())
                .role("USER")
                .status("ACTIVE")
                .build();
        
        userRepository.save(user);

        // 处理邀请码（失败不影响注册）
        if (request.getInvitationCode() != null && !request.getInvitationCode().isBlank()) {
            try {
                memberService.processInvitation(request.getInvitationCode(), user.getId());
            } catch (Exception e) {
                log.warn("邀请码处理失败，不影响注册: code={}, error={}", request.getInvitationCode(), e.getMessage());
            }
        }

        log.info("用户注册成功: {}", user.getUsername());
    }
    
    /**
     * 用户登录
     */
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress) {
        String username = request.getUsername();
        
        // 1. 检查登录失败次数
        int failCount = getLoginFailCount(username);
        if (failCount >= MAX_LOGIN_FAIL) {
            // 需要验证码
            if (request.getCaptcha() == null || request.getCaptcha().isEmpty()) {
                throw BusinessException.Auth.captchaRequired();
            }
            validateCaptcha(request.getCaptchaKey(), request.getCaptcha());
        }
        
        // 2. 查找用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    incrementLoginFailCount(username);
                    return BusinessException.Auth.loginFailed();
                });
        
        // 3. 检查账号状态
        if ("BANNED".equalsIgnoreCase(user.getStatus())) {
            throw BusinessException.Auth.accountBanned();
        }
        if ("DISABLED".equalsIgnoreCase(user.getStatus())) {
            throw BusinessException.Auth.accountDisabled();
        }
        
        // 4. 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            incrementLoginFailCount(username);
            throw BusinessException.Auth.loginFailed();
        }
        
        // 5. 登录成功，清除失败计数
        clearLoginFailCount(username);
        
        // 6. 更新最后登录信息
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ipAddress);
        userRepository.save(user);
        
        // 7. 生成令牌
        String accessToken;
        long tokenExpiresIn;
        if (Boolean.TRUE.equals(request.getRememberMe())) {
            accessToken = jwtUtil.generateRememberMeToken(
                    user.getId(), 
                    user.getUsername(), 
                    user.getRole()
            );
            tokenExpiresIn = jwtUtil.getRefreshExpiration() / 1000;
        } else {
            accessToken = jwtUtil.generateAccessToken(
                    user.getId(), 
                    user.getUsername(), 
                    user.getRole()
            );
            tokenExpiresIn = jwtUtil.getExpiration() / 1000;
        }
        
        String refreshToken = jwtUtil.generateRefreshToken(
                user.getId(), 
                user.getUsername()
        );
        
        log.info("用户登录成功: {}", username);
        
        // 8. 构建响应
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenExpiresIn)
                .userInfo(LoginResponse.UserInfo.fromUser(user))
                .build();
    }
    
    /**
     * 退出登录
     */
    public void logout(String token) {
        // 将token加入黑名单
        Long remainingTime = jwtUtil.getTokenRemainingTime(token);
        if (remainingTime > 0) {
            String blacklistKey = "token:blacklist:" + token;
            redisTemplate.opsForValue().set(blacklistKey, "1", remainingTime, TimeUnit.SECONDS);
        }
        
        log.info("用户退出登录");
    }
    
    /**
     * 生成验证码
     */
    public CaptchaResponse generateCaptcha() {
        String code = captchaUtil.generateCode();
        String key = UUID.randomUUID().toString();
        String image = captchaUtil.generateCaptchaImage(code);
        
        // 存储到Redis
        String redisKey = CAPTCHA_PREFIX + key;
        redisTemplate.opsForValue().set(
                redisKey, 
                code.toLowerCase(), 
                CAPTCHA_EXPIRE_MINUTES, 
                TimeUnit.MINUTES
        );
        
        return CaptchaResponse.builder()
                .captchaKey(key)
                .captchaImage(image)
                .expiresIn((long) CAPTCHA_EXPIRE_MINUTES * 60)
                .build();
    }
    
    /**
     * 验证验证码
     */
    private void validateCaptcha(String key, String code) {
        if (key == null || code == null) {
            throw BusinessException.Auth.captchaInvalid();
        }
        
        String redisKey = CAPTCHA_PREFIX + key;
        String correctCode = (String) redisTemplate.opsForValue().get(redisKey);
        
        if (correctCode == null) {
            throw BusinessException.Auth.captchaInvalid();
        }
        
        // 验证后删除
        redisTemplate.delete(redisKey);
        
        if (!captchaUtil.verify(code, correctCode)) {
            throw BusinessException.Auth.captchaInvalid();
        }
    }
    
    /**
     * 获取登录失败次数
     */
    private int getLoginFailCount(String username) {
        String key = LOGIN_FAIL_PREFIX + username;
        Object count = redisTemplate.opsForValue().get(key);
        return count != null ? (Integer) count : 0;
    }
    
    /**
     * 增加登录失败次数
     */
    private void incrementLoginFailCount(String username) {
        String key = LOGIN_FAIL_PREFIX + username;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, LOGIN_FAIL_EXPIRE_MINUTES, TimeUnit.MINUTES);
    }
    
    /**
     * 清除登录失败次数
     */
    private void clearLoginFailCount(String username) {
        String key = LOGIN_FAIL_PREFIX + username;
        redisTemplate.delete(key);
    }
    
    /**
     * 检查token是否在黑名单中
     */
    public boolean isTokenBlacklisted(String token) {
        String blacklistKey = "token:blacklist:" + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
    }
    
    /**
     * 发送注册邮箱验证码
     */
    public void sendRegisterEmailCode(String email) {
        // 检查邮箱是否已注册
        if (userRepository.existsByEmail(email)) {
            throw BusinessException.Auth.emailExists();
        }
        
        // 频率限制：1分钟内只能发一次
        String rateLimitKey = EMAIL_RATE_PREFIX + email;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(rateLimitKey))) {
            throw new BusinessException(429, "发送过于频繁，请1分钟后重试");
        }
        
        // 生成6位数字验证码
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append((int) (Math.random() * 10));
        }
        String emailCode = code.toString();
        
        // 存储到Redis
        String redisKey = EMAIL_CODE_PREFIX + email;
        redisTemplate.opsForValue().set(redisKey, emailCode, EMAIL_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(rateLimitKey, "1", 1, TimeUnit.MINUTES);
        
        // 发送邮件
        try {
            emailService.sendRegisterCode(email, emailCode);
            log.info("注册邮箱验证码已发送: email={}", email);
        } catch (Exception e) {
            log.error("发送注册邮箱验证码失败: email={}", email, e);
            redisTemplate.delete(redisKey);
            throw BusinessException.Auth.emailSendFailed();
        }
    }
    
    /**
     * 验证邮箱验证码
     */
    private void validateEmailCode(String email, String code) {
        if (email == null || code == null) {
            throw new BusinessException(40005, "邮箱验证码错误");
        }
        
        String redisKey = EMAIL_CODE_PREFIX + email;
        String storedCode = (String) redisTemplate.opsForValue().get(redisKey);
        
        if (storedCode == null || !storedCode.equals(code)) {
            throw new BusinessException(40005, "邮箱验证码错误或已过期");
        }
        
        // 验证通过后删除
        redisTemplate.delete(redisKey);
    }
    
    /**
     * 检查用户名是否可用
     */
    public boolean checkUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }
    
    /**
     * 检查邮箱是否可用
     */
    public boolean checkEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }
    
    /**
     * 刷新访问令牌
     */
    public LoginResponse refreshAccessToken(String refreshTokenStr) {
        if (refreshTokenStr == null || refreshTokenStr.isBlank()) {
            throw BusinessException.Auth.tokenInvalid();
        }
        
        // 检查是否在黑名单中
        if (isTokenBlacklisted(refreshTokenStr)) {
            throw BusinessException.Auth.tokenInvalid();
        }
        
        // 验证refresh token
        if (!jwtUtil.validateToken(refreshTokenStr)) {
            throw BusinessException.Auth.tokenInvalid();
        }
        
        Long userId = jwtUtil.getUserIdFromToken(refreshTokenStr);
        String username = jwtUtil.getUsernameFromToken(refreshTokenStr);
        if (userId == null || username == null) {
            throw BusinessException.Auth.tokenInvalid();
        }
        
        // 查找用户确保仍有效
        User user = userRepository.findById(userId)
                .orElseThrow(BusinessException.Auth::userNotFound);
        
        if ("BANNED".equalsIgnoreCase(user.getStatus()) || "DISABLED".equalsIgnoreCase(user.getStatus())) {
            throw BusinessException.Auth.accountDisabled();
        }
        
        // 生成新的access token
        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        
        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshTokenStr)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpiration() / 1000)
                .userInfo(LoginResponse.UserInfo.fromUser(user))
                .build();
    }
    
    /**
     * 发送密码重置验证码到邮箱
     */
    public void sendPasswordResetCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(BusinessException.Auth::userNotFound);
        
        if ("BANNED".equalsIgnoreCase(user.getStatus()) || "DISABLED".equalsIgnoreCase(user.getStatus())) {
            throw BusinessException.Auth.accountDisabled();
        }
        
        // 检查是否频繁发送（1分钟内只能发一次）
        String rateLimitKey = "reset:rate:" + email;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(rateLimitKey))) {
            throw new BusinessException(429, "发送过于频繁，请1分钟后重试");
        }
        
        // 生成6位数字验证码
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < RESET_CODE_LENGTH; i++) {
            code.append((int) (Math.random() * 10));
        }
        String resetCode = code.toString();
        
        // 存储到Redis
        String redisKey = RESET_CODE_PREFIX + email;
        redisTemplate.opsForValue().set(redisKey, resetCode, RESET_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(rateLimitKey, "1", 1, TimeUnit.MINUTES);
        
        // 发送邮件
        try {
            emailService.sendResetCode(email, user.getUsername(), resetCode);
            log.info("密码重置验证码已发送: email={}", email);
        } catch (Exception e) {
            log.error("发送密码重置邮件失败: email={}", email, e);
            redisTemplate.delete(redisKey);
            throw BusinessException.Auth.emailSendFailed();
        }
    }
    
    /**
     * 验证重置码并重置密码
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // 验证两次密码是否一致
        if (!request.isPasswordMatch()) {
            throw BusinessException.Auth.passwordMismatch();
        }
        
        String email = request.getEmail();
        String code = request.getCode();
        String newPassword = request.getNewPassword();
        
        // 验证码校验
        String redisKey = RESET_CODE_PREFIX + email;
        String storedCode = (String) redisTemplate.opsForValue().get(redisKey);
        
        if (storedCode == null || !storedCode.equals(code)) {
            throw BusinessException.Auth.resetCodeInvalid();
        }
        
        // 删除验证码（一次性使用）
        redisTemplate.delete(redisKey);
        
        // 查找用户并更新密码
        User user = userRepository.findByEmail(email)
                .orElseThrow(BusinessException.Auth::userNotFound);
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        // 清除该用户的登录失败计数
        clearLoginFailCount(user.getUsername());
        
        log.info("用户密码重置成功: email={}", email);
    }
    
    /**
     * 微信小程序登录
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public LoginResponse wxLogin(String code, String ipAddress) {
        if (wxAppId == null || wxAppId.isBlank() || wxAppSecret == null || wxAppSecret.isBlank()) {
            throw new BusinessException(500, "微信小程序配置未完成");
        }
        
        // 1. 使用code向微信服务器换取openid
        String url = String.format(
            "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
            wxAppId, wxAppSecret, code
        );
        
        RestTemplate restTemplate = new RestTemplate();
        String responseBody = restTemplate.getForObject(url, String.class);
        
        Map<String, Object> body;
        try {
            ObjectMapper mapper = new ObjectMapper();
            body = mapper.readValue(responseBody, Map.class);
        } catch (Exception e) {
            log.error("微信登录响应解析失败: {}", responseBody);
            throw new BusinessException(500, "微信登录失败: 响应解析错误");
        }
        
        if (body == null || (body.containsKey("errcode") && !Integer.valueOf(0).equals(body.get("errcode")))) {
            String errMsg = body != null ? String.valueOf(body.get("errmsg")) : "unknown";
            log.warn("微信登录失败: errcode={}, errmsg={}", body != null ? body.get("errcode") : "null", errMsg);
            throw new BusinessException(500, "微信登录失败: " + errMsg);
        }
        
        String openId = (String) body.get("openid");
        if (openId == null || openId.isBlank()) {
            throw new BusinessException(500, "微信登录失败: 无法获取openid");
        }
        
        // 2. 查找或创建用户
        User user = userRepository.findByWxOpenId(openId).orElse(null);
        
        if (user == null) {
            // 自动注册新用户
            String autoUsername = "wx_" + openId.substring(0, Math.min(openId.length(), 16));
            String autoNickname = "微信用户" + openId.substring(openId.length() - 4);
            
            user = User.builder()
                    .username(autoUsername)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .wxOpenId(openId)
                    .nickname(autoNickname)
                    .role("USER")
                    .status("ACTIVE")
                    .build();
            userRepository.save(user);
            log.info("微信用户自动注册: openId={}, username={}", openId, autoUsername);
        }
        
        // 3. 检查账号状态
        if ("BANNED".equalsIgnoreCase(user.getStatus())) {
            throw BusinessException.Auth.accountBanned();
        }
        
        // 4. 更新登录信息
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ipAddress);
        userRepository.save(user);
        
        // 5. 生成JWT
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());
        
        log.info("微信用户登录成功: openId={}, userId={}", openId, user.getId());
        
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpiration() / 1000)
                .userInfo(LoginResponse.UserInfo.fromUser(user))
                .build();
    }
}
