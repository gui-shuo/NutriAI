package com.nutriai.config;

import com.nutriai.filter.JwtAuthenticationFilter;
import com.nutriai.security.JwtAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
@EnableMethodSecurity
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
                                "/vip/epay/notify",  // 易支付异步回调（服务端无Token）
                                "/vip/epay/return",  // 易支付同步跳转（浏览器重定向）
                                "/consultation/nutritionists",  // 营养师列表（公开）
                                "/consultation/nutritionists/online",  // 在线营养师列表（公开）
                                "/consultation/nutritionists/*",  // 营养师详情（公开）
                                "/products",  // 产品列表（公开）
                                "/products/search",  // 产品搜索（公开）
                                "/products/recommended",  // 推荐产品（公开）
                                "/products/categories",  // 产品分类（公开）
                                "/products/*",  // 产品详情（公开）
                                "/meals",  // 营养餐列表（公开）
                                "/meals/search",  // 营养餐搜索（公开）
                                "/meals/recommended",  // 推荐营养餐（公开）
                                "/meals/categories",  // 营养餐分类（公开）
                                "/meals/*",  // 营养餐详情（公开）
                                "/wxpay/notify",  // 微信支付回调（服务端无Token）
                                "/community/feed",  // 社区动态列表（公开浏览）
                                "/app-versions/latest",  // APP最新版本（公开）
                                "/app-versions/list",  // APP版本列表（公开）
                                "/app-versions/download/*",  // APP下载（公开）
                                "/auth/social/wechat/auth-url",  // 微信授权URL（公开）
                                "/auth/social/qq/auth-url",  // QQ授权URL（公开）
                                "/auth/social/wechat/login",  // 微信登录（公开）
                                "/auth/social/qq/login",  // QQ登录（公开）
                                "/auth/social/qq/token-login",  // QQ APP原生登录（公开）
                                "/recipes",  // 食谱列表（公开）
                                "/recipes/featured",  // 精选食谱（公开）
                                "/recipes/tags",  // 食谱标签（公开）
                                "/recipes/*",  // 食谱详情（公开）
                                "/recipes/corpus",  // 食谱语料库搜索（公开）
                                "/recipes/corpus/**",  // 食谱语料库详情/分类（公开）
                                "/meal-plans",  // 营养餐列表（公开）
                                "/meal-plans/featured",  // 精选营养餐（公开）
                                "/meal-plans/tags",  // 标签列表（公开）
                                "/meal-plans/by-tag",  // 按标签搜索（公开）
                                "/meal-plans/recommendations",  // 推荐（公开）
                                "/meal-plans/*/ratings",  // 评价列表（公开）
                                "/meal-plans/*",  // 营养餐详情（公开）
                                "/merchants/nearby",  // 附近门店（公开）
                                "/merchants/active",  // 营业门店列表（公开）
                                "/merchants/*"  // 门店详情（公开）
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
