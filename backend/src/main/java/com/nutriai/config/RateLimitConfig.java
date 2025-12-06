package com.nutriai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 限流配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitConfig {
    
    /**
     * 是否启用限流
     */
    private boolean enabled = true;
    
    /**
     * 默认限流：每秒请求数
     */
    private int defaultRate = 100;
    
    /**
     * API限流：每秒请求数
     */
    private int apiRate = 50;
    
    /**
     * AI接口限流：每秒请求数
     */
    private int aiRate = 10;
    
    /**
     * 登录接口限流：每分钟请求数
     */
    private int loginRate = 5;
    
    /**
     * 限流时间窗口（秒）
     */
    private int windowSeconds = 60;
}
