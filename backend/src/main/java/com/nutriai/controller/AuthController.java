package com.nutriai.controller;

import com.nutriai.common.ApiResponse;
import com.nutriai.dto.auth.*;
import com.nutriai.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
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
