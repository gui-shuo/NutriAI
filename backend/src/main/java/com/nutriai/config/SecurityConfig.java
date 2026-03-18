package com.nutriai.config;

import com.nutriai.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Spring Security配置
 */
@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("配置Security过滤器链...");
        
        http
                // 禁用CSRF（前后端分离项目）
                .csrf(AbstractHttpConfigurer::disable)
                
                // 启用CORS
                .cors(withDefaults())
                
                // 禁用Session（使用JWT）
                .sessionManagement(session -> 
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                
                // 配置请求授权
                .authorizeHttpRequests(auth -> auth
                        // 公开接口（不需要认证）
                        .requestMatchers(
                                "/auth/**",
                                "/health",
                                "/api/health",
                                "/doc",
                                "/doc/**",
                                "/error",
                                "/uploads/**",
                                "/ws/**",  // WebSocket端点（WebSocket自己会验证token）
                                "/announcements/**",  // 公告接口（公开访问）
                                "/public/**"  // 公开配置接口
                        ).permitAll()
                        
                        // 其他所有请求都需要认证
                        .anyRequest().authenticated()
                )
                
                // 添加JWT认证过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        log.info("✅ Security配置完成");
        return http.build();
    }
}
