package com.nutriai.config;

import com.nutriai.filter.JwtAuthenticationFilter;
import com.nutriai.security.JwtAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
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
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    
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
                                "/ws/**",  // WebSocket端点（WebSocket自己会验证token）
                                "/announcements/**",  // 公告接口（公开访问）
                                "/public/**",  // 公开配置接口
                                "/vip/plans",  // VIP套餐列表（未登录可查看）
                                "/vip/alipay/notify"  // 支付宝回调（服务端无Token）
                        ).permitAll()
                        
                        // 其他所有请求都需要认证
                        .anyRequest().authenticated()
                )
                
                // 异常处理：未认证返回401 JSON
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                
                // 安全响应头
                .headers(headers -> headers
                        .contentTypeOptions(withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .cacheControl(withDefaults())
                )
                
                // 添加JWT认证过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        log.info("✅ Security配置完成");
        return http.build();
    }
}
