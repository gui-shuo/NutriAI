package com.nutriai.interceptor;

import com.nutriai.config.RateLimitConfig;
import com.nutriai.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * API限流拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private final RateLimitService rateLimitService;
    private final RateLimitConfig rateLimitConfig;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果限流未启用，直接放行
        if (!rateLimitConfig.isEnabled()) {
            return true;
        }
        
        String uri = request.getRequestURI();
        String ip = getClientIP(request);
        String identifier = ip + ":" + uri;
        
        // 根据不同的API路径应用不同的限流策略
        boolean allowed;
        
        if (uri.contains("/auth/login")) {
            // 登录接口：每分钟5次
            allowed = rateLimitService.checkLoginLimit(identifier);
        } else if (uri.contains("/ai/")) {
            // AI接口：每秒10次
            allowed = rateLimitService.checkAiLimit(identifier);
        } else if (uri.contains("/admin/")) {
            // 管理接口：每秒50次
            allowed = rateLimitService.checkApiLimit(identifier);
        } else {
            // 其他接口：每秒50次
            allowed = rateLimitService.checkApiLimit(identifier);
        }
        
        if (!allowed) {
            log.warn("Rate limit exceeded: uri={}, ip={}", uri, ip);
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\",\"data\":null}");
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取客户端真实IP
     */
    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 对于多级代理，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
