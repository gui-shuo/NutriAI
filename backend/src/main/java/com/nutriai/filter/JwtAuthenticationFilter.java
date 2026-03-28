package com.nutriai.filter;

import com.nutriai.service.AuthService;
import com.nutriai.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT认证过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;
    private final AuthService authService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        try {
            // 从请求头获取Token
            String token = getTokenFromRequest(request);
            
            if (token != null) {
                // 检查Token是否在黑名单中（已退出登录）
                if (authService.isTokenBlacklisted(token)) {
                    request.setAttribute("jwt-error", "blacklisted");
                    log.debug("JWT令牌已被加入黑名单: {}", request.getRequestURI());
                } else if (jwtUtil.validateToken(token)) {
                    // Token有效，设置认证信息
                    Long userId = jwtUtil.getUserIdFromToken(token);
                    String role = jwtUtil.getRoleFromToken(token);
                    
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                        );
                    
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    request.setAttribute("userId", userId);
                    
                    log.debug("JWT认证成功: userId={}, role={}", userId, role);
                } else {
                    // Token无效或过期，标记到request attribute供EntryPoint区分
                    request.setAttribute("jwt-error", "invalid");
                    log.debug("JWT令牌无效: {}", request.getRequestURI());
                }
            }
        } catch (Exception e) {
            log.debug("JWT认证过程异常: {}", e.getMessage());
            request.setAttribute("jwt-error", "error");
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * 从请求头中获取Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
