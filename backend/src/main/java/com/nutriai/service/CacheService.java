package com.nutriai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 缓存服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // 缓存键前缀
    private static final String CACHE_PREFIX = "nutriai:";
    private static final String CONFIG_CACHE_KEY = CACHE_PREFIX + "config:";
    private static final String USER_CACHE_KEY = CACHE_PREFIX + "user:";
    private static final String FOOD_CACHE_KEY = CACHE_PREFIX + "food:";
    
    // 默认过期时间（秒）
    private static final long DEFAULT_EXPIRE = 3600; // 1小时
    private static final long CONFIG_EXPIRE = 1800; // 30分钟
    private static final long USER_EXPIRE = 600; // 10分钟
    
    /**
     * 设置缓存
     */
    public void set(String key, Object value) {
        set(key, value, DEFAULT_EXPIRE);
    }
    
    /**
     * 设置缓存（带过期时间）
     */
    public void set(String key, Object value, long timeout) {
        try {
            redisTemplate.opsForValue().set(CACHE_PREFIX + key, value, timeout, TimeUnit.SECONDS);
            log.debug("Cache set: key={}, timeout={}s", key, timeout);
        } catch (Exception e) {
            log.error("Failed to set cache: key={}", key, e);
        }
    }
    
    /**
     * 获取缓存
     */
    public Object get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(CACHE_PREFIX + key);
            log.debug("Cache get: key={}, hit={}", key, value != null);
            return value;
        } catch (Exception e) {
            log.error("Failed to get cache: key={}", key, e);
            return null;
        }
    }
    
    /**
     * 删除缓存
     */
    public void delete(String key) {
        try {
            redisTemplate.delete(CACHE_PREFIX + key);
            log.debug("Cache deleted: key={}", key);
        } catch (Exception e) {
            log.error("Failed to delete cache: key={}", key, e);
        }
    }
    
    /**
     * 删除匹配的缓存
     */
    public void deletePattern(String pattern) {
        try {
            var keys = redisTemplate.keys(CACHE_PREFIX + pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Cache deleted by pattern: pattern={}, count={}", pattern, keys.size());
            }
        } catch (Exception e) {
            log.error("Failed to delete cache by pattern: pattern={}", pattern, e);
        }
    }
    
    /**
     * 检查缓存是否存在
     */
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(CACHE_PREFIX + key);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("Failed to check cache existence: key={}", key, e);
            return false;
        }
    }
    
    /**
     * 设置过期时间
     */
    public void expire(String key, long timeout) {
        try {
            redisTemplate.expire(CACHE_PREFIX + key, timeout, TimeUnit.SECONDS);
            log.debug("Cache expire set: key={}, timeout={}s", key, timeout);
        } catch (Exception e) {
            log.error("Failed to set cache expire: key={}", key, e);
        }
    }
    
    // ========================================
    // 业务缓存方法
    // ========================================
    
    /**
     * 缓存配置
     */
    public void cacheConfig(String configKey, Object value) {
        set(CONFIG_CACHE_KEY + configKey, value, CONFIG_EXPIRE);
    }
    
    /**
     * 获取配置缓存
     */
    public Object getConfigCache(String configKey) {
        return get(CONFIG_CACHE_KEY + configKey);
    }
    
    /**
     * 删除配置缓存
     */
    public void deleteConfigCache(String configKey) {
        delete(CONFIG_CACHE_KEY + configKey);
    }
    
    /**
     * 删除所有配置缓存
     */
    public void deleteAllConfigCache() {
        deletePattern(CONFIG_CACHE_KEY + "*");
    }
    
    /**
     * 缓存用户信息
     */
    public void cacheUser(Long userId, Object user) {
        set(USER_CACHE_KEY + userId, user, USER_EXPIRE);
    }
    
    /**
     * 获取用户缓存
     */
    public Object getUserCache(Long userId) {
        return get(USER_CACHE_KEY + userId);
    }
    
    /**
     * 删除用户缓存
     */
    public void deleteUserCache(Long userId) {
        delete(USER_CACHE_KEY + userId);
    }
    
    /**
     * 缓存食物信息
     */
    public void cacheFood(String foodCode, Object food) {
        set(FOOD_CACHE_KEY + foodCode, food, DEFAULT_EXPIRE);
    }
    
    /**
     * 获取食物缓存
     */
    public Object getFoodCache(String foodCode) {
        return get(FOOD_CACHE_KEY + foodCode);
    }
    
    /**
     * 增加计数器
     */
    public Long increment(String key) {
        try {
            return redisTemplate.opsForValue().increment(CACHE_PREFIX + key);
        } catch (Exception e) {
            log.error("Failed to increment: key={}", key, e);
            return null;
        }
    }
    
    /**
     * 减少计数器
     */
    public Long decrement(String key) {
        try {
            return redisTemplate.opsForValue().decrement(CACHE_PREFIX + key);
        } catch (Exception e) {
            log.error("Failed to decrement: key={}", key, e);
            return null;
        }
    }
}
