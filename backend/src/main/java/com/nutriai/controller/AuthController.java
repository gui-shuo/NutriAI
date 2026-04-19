package com.nutriai.controller;

import com.nutriai.common.ApiResponse;
import com.nutriai.dto.auth.*;
import com.nutriai.entity.Merchant;
import com.nutriai.service.AuthService;
import com.nutriai.service.MerchantService;
import com.nutriai.service.OssService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    private final OssService ossService;
    private final MerchantService merchantService;
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        ApiResponse<Void> response = ApiResponse.success();
        response.setMessage("注册成功");
        return response;
    }
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String ipAddress = getClientIp(httpRequest);
        LoginResponse response = authService.login(request, ipAddress);
        return ApiResponse.success("登录成功", response);
    }
    
    /**
     * 刷新访问令牌
     */
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshAccessToken(request.getRefreshToken());
        return ApiResponse.success("令牌刷新成功", response);
    }
    
    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String authorization) {
        String token = extractToken(authorization);
        authService.logout(token);
        ApiResponse<Void> response = ApiResponse.success();
        response.setMessage("退出成功");
        return response;
    }
    
    /**
     * 获取验证码
     */
    @GetMapping("/captcha")
    public ApiResponse<CaptchaResponse> getCaptcha() {
        CaptchaResponse response = authService.generateCaptcha();
        return ApiResponse.success(response);
    }
    
    /**
     * 检查用户名是否可用
     */
    @GetMapping("/check-username")
    public ApiResponse<Boolean> checkUsername(@RequestParam String username) {
        boolean available = authService.checkUsernameAvailable(username);
        return ApiResponse.success(available ? "用户名可用" : "用户名已被占用", available);
    }
    
    /**
     * 检查邮箱是否可用
     */
    @GetMapping("/check-email")
    public ApiResponse<Boolean> checkEmail(@RequestParam String email) {
        boolean available = authService.checkEmailAvailable(email);
        return ApiResponse.success(available ? "邮箱可用" : "邮箱已被注册", available);
    }
    
    /**
     * 发送注册邮箱验证码
     */
    @PostMapping("/send-email-code")
    public ApiResponse<Void> sendEmailCode(@RequestBody java.util.Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ApiResponse.error(400, "邮箱不能为空");
        }
        authService.sendRegisterEmailCode(email);
        ApiResponse<Void> response = ApiResponse.success();
        response.setMessage("验证码已发送到您的邮箱，5分钟内有效");
        return response;
    }
    
    /**
     * 忘记密码 - 发送重置验证码到邮箱
     */
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.sendPasswordResetCode(request.getEmail());
        ApiResponse<Void> response = ApiResponse.success();
        response.setMessage("验证码已发送到您的邮箱，15分钟内有效");
        return response;
    }
    
    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        ApiResponse<Void> response = ApiResponse.success();
        response.setMessage("密码重置成功，请使用新密码登录");
        return response;
    }
    
    /**
     * 微信小程序登录
     */
    @PostMapping("/wx-login")
    public ApiResponse<LoginResponse> wxLogin(
            @Valid @RequestBody WxLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String ipAddress = getClientIp(httpRequest);
        LoginResponse response = authService.wxLogin(request.getCode(), ipAddress);
        return ApiResponse.success("微信登录成功", response);
    }
    
    /**
     * 营养师注册申请
     */
    @PostMapping("/nutritionist/register")
    public ApiResponse<Void> registerNutritionist(@Valid @RequestBody NutritionistRegisterRequest request) {
        authService.registerNutritionist(request);
        ApiResponse<Void> response = ApiResponse.success();
        response.setMessage("营养师注册申请已提交，请等待管理员审核");
        return response;
    }

    /**
     * 上传资质证书图片（公开接口，用于营养师注册）
     */
    @PostMapping("/upload-certificate")
    public ApiResponse<String> uploadCertificate(@RequestParam("file") MultipartFile file) {
        String url = ossService.uploadCertificate(file);
        return ApiResponse.success("上传成功", url);
    }

    /**
     * 商家登录（复用普通登录，额外校验商家身份并返回商家信息）
     */
    @PostMapping("/merchant-login")
    public ApiResponse<Map<String, Object>> merchantLogin(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        LoginResponse loginResponse = authService.merchantLogin(request, ipAddress);

        // 获取商家信息
        Long userId = loginResponse.getUserInfo().getId();
        Merchant merchant = merchantService.getMerchantByOwnerId(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accessToken", loginResponse.getAccessToken());
        result.put("refreshToken", loginResponse.getRefreshToken());
        result.put("tokenType", loginResponse.getTokenType());
        result.put("expiresIn", loginResponse.getExpiresIn());
        result.put("userInfo", loginResponse.getUserInfo());
        result.put("merchantInfo", Map.of(
                "id", merchant.getId(),
                "name", merchant.getName(),
                "status", merchant.getStatus() != null ? merchant.getStatus() : "ACTIVE"
        ));

        return ApiResponse.success("商家登录成功", result);
    }

    /**
     * 从Authorization头中提取token
     */
    private String extractToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return authorization;
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个IP的情况（取第一个）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
