package com.nutriai.service;

import com.nutriai.config.RateLimitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 限流服务
 * 使用Redis实现滑动窗口限流
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final RateLimitConfig rateLimitConfig;
    
    private static final String RATE_LIMIT_PREFIX = "nutriai:rate_limit:";
    
    /**
     * 检查是否超过限流
     * 
     * @param key 限流键（如：user_id, ip_address）
     * @param maxRequests 最大请求数
     * @param windowSeconds 时间窗口（秒）
     * @return true-允许请求，false-超过限流
     */
    public boolean isAllowed(String key, int maxRequests, int windowSeconds) {
        if (!rateLimitConfig.isEnabled()) {
            return true;
        }
        
        String redisKey = RATE_LIMIT_PREFIX + key;
        
        try {
            // 获取当前计数
            Long currentCount = redisTemplate.opsForValue().increment(redisKey);
            
            if (currentCount == null) {
                return true;
            }
            
            // 第一次请求，设置过期时间
            if (currentCount == 1) {
                redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
            }
            
            // 检查是否超过限流
            boolean allowed = currentCount <= maxRequests;
            
            if (!allowed) {
                log.warn("Rate limit exceeded: key={}, count={}, max={}", key, currentCount, maxRequests);
            }
            
            return allowed;
            
        } catch (Exception e) {
            log.error("Rate limit check failed: key={}", key, e);
            // 出错时允许请求，避免影响业务
            return true;
        }
    }
    
    /**
     * 检查API限流
     */
    public boolean checkApiLimit(String identifier) {
        return isAllowed("api:" + identifier, rateLimitConfig.getApiRate(), 1);
    }
    
    /**
     * 检查AI接口限流
     */
    public boolean checkAiLimit(String identifier) {
        return isAllowed("ai:" + identifier, rateLimitConfig.getAiRate(), 1);
    }
    
    /**
     * 检查登录限流
     */
    public boolean checkLoginLimit(String identifier) {
        return isAllowed("login:" + identifier, rateLimitConfig.getLoginRate(), 60);
    }
    
    /**
     * 获取剩余请求次数
     */
    public long getRemainingRequests(String key, int maxRequests) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        
        try {
            Object value = redisTemplate.opsForValue().get(redisKey);
            if (value == null) {
                return maxRequests;
            }
            
            long currentCount = Long.parseLong(value.toString());
            return Math.max(0, maxRequests - currentCount);
            
        } catch (Exception e) {
            log.error("Failed to get remaining requests: key={}", key, e);
            return maxRequests;
        }
    }
    
    /**
     * 重置限流计数
     */
    public void reset(String key) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        try {
            redisTemplate.delete(redisKey);
            log.debug("Rate limit reset: key={}", key);
        } catch (Exception e) {
            log.error("Failed to reset rate limit: key={}", key, e);
        }
    }
}
