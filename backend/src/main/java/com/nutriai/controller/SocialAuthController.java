package com.nutriai.controller;

import com.nutriai.common.ApiResponse;
import com.nutriai.dto.auth.LoginResponse;
import com.nutriai.dto.auth.SocialBindInfo;
import com.nutriai.dto.auth.SocialLoginRequest;
import com.nutriai.service.SocialAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 第三方社交登录控制器
 */
@Slf4j
@RestController
@RequestMapping("/auth/social")
@RequiredArgsConstructor
public class SocialAuthController {

    private final SocialAuthService socialAuthService;

    // ==================== 登录接口（公开） ====================

    /**
     * 获取微信授权URL
     */
    @GetMapping("/wechat/auth-url")
    public ApiResponse<String> getWechatAuthUrl(@RequestParam(defaultValue = "login") String state) {
        return ApiResponse.success(socialAuthService.getWechatAuthUrl(state));
    }

    /**
     * 获取QQ授权URL
     */
    @GetMapping("/qq/auth-url")
    public ApiResponse<String> getQqAuthUrl(@RequestParam(defaultValue = "login") String state) {
        return ApiResponse.success(socialAuthService.getQqAuthUrl(state));
    }

    /**
     * 微信登录回调
     */
    @PostMapping("/wechat/login")
    public ApiResponse<LoginResponse> wechatLogin(
            @Valid @RequestBody SocialLoginRequest request,
            HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        LoginResponse response = socialAuthService.wechatLogin(request.getCode(), ip);
        return ApiResponse.success("微信登录成功", response);
    }

    /**
     * QQ登录回调
     */
    @PostMapping("/qq/login")
    public ApiResponse<LoginResponse> qqLogin(
            @Valid @RequestBody SocialLoginRequest request,
            HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        LoginResponse response = socialAuthService.qqLogin(request.getCode(), ip);
        return ApiResponse.success("QQ登录成功", response);
    }

    // ==================== 绑定接口（需登录） ====================

    /**
     * 获取绑定状态
     */
    @GetMapping("/bindinfo")
    public ApiResponse<SocialBindInfo> getBindInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResponse.success(socialAuthService.getBindInfo(userId));
    }

    /**
     * 绑定微信
     */
    @PostMapping("/bind/wechat")
    public ApiResponse<Void> bindWechat(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        socialAuthService.bindWechat(userId, body.get("code"));
        return ApiResponse.success("微信绑定成功", null);
    }

    /**
     * 绑定QQ
     */
    @PostMapping("/bind/qq")
    public ApiResponse<Void> bindQq(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        socialAuthService.bindQq(userId, body.get("code"));
        return ApiResponse.success("QQ绑定成功", null);
    }

    /**
     * 解绑微信
     */
    @DeleteMapping("/unbind/wechat")
    public ApiResponse<Void> unbindWechat(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        socialAuthService.unbindWechat(userId);
        return ApiResponse.success("微信已解绑", null);
    }

    /**
     * 解绑QQ
     */
    @DeleteMapping("/unbind/qq")
    public ApiResponse<Void> unbindQq(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        socialAuthService.unbindQq(userId);
        return ApiResponse.success("QQ已解绑", null);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
