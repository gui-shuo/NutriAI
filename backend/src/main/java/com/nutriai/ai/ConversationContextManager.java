package com.nutriai.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对话上下文管理器
 * 管理每个用户的对话历史和上下文信息
 * 
 * @author NutriAI Team
 * @date 2025-12-03
 */
@Slf4j
@Component
public class ConversationContextManager {
    
    // 用户ID -> 对话上下文
    private final Map<Long, ConversationContext> contexts = new ConcurrentHashMap<>();
    
    // 默认保留的历史消息数量
    private static final int DEFAULT_MESSAGE_WINDOW = 10;
    
    // 上下文过期时间（毫秒）- 30分钟
    private static final long CONTEXT_EXPIRY_MS = 30 * 60 * 1000;
    
    /**
     * 获取或创建用户的对话上下文
     * 
     * @param userId 用户ID
     * @return 对话上下文
     */
    public ConversationContext getOrCreateContext(Long userId) {
        return contexts.computeIfAbsent(userId, id -> {
            log.info("为用户 {} 创建新的对话上下文", userId);
            ConversationContext context = new ConversationContext();
            context.setUserId(userId);
            context.setCreatedAt(LocalDateTime.now());
            context.setLastAccessAt(LocalDateTime.now());
            context.setChatMemory(MessageWindowChatMemory.withMaxMessages(DEFAULT_MESSAGE_WINDOW));
            return context;
        });
    }
    
    /**
     * 添加系统消息到上下文
     * 
     * @param userId 用户ID
     * @param systemPrompt 系统提示词
     */
    public void addSystemMessage(Long userId, String systemPrompt) {
        ConversationContext context = getOrCreateContext(userId);
        context.getChatMemory().add(SystemMessage.from(systemPrompt));
        context.setLastAccessAt(LocalDateTime.now());
        log.debug("用户 {} 添加系统消息", userId);
    }
    
    /**
     * 添加用户消息到上下文
     * 
     * @param userId 用户ID
     * @param userMessage 用户消息
     */
    public void addUserMessage(Long userId, String userMessage) {
        ConversationContext context = getOrCreateContext(userId);
        context.getChatMemory().add(UserMessage.from(userMessage));
        context.setLastAccessAt(LocalDateTime.now());
        context.setMessageCount(context.getMessageCount() + 1);
        log.debug("用户 {} 添加用户消息", userId);
    }
    
    /**
     * 添加AI响应到上下文
     * 
     * @param userId 用户ID
     * @param aiResponse AI响应
     */
    public void addAiMessage(Long userId, String aiResponse) {
        ConversationContext context = getOrCreateContext(userId);
        context.getChatMemory().add(AiMessage.from(aiResponse));
        context.setLastAccessAt(LocalDateTime.now());
        log.debug("用户 {} 添加AI响应", userId);
    }
    
    /**
     * 获取用户的所有消息历史
     * 
     * @param userId 用户ID
     * @return 消息列表
     */
    public List<ChatMessage> getMessageHistory(Long userId) {
        ConversationContext context = contexts.get(userId);
        if (context == null) {
            return List.of();
        }
        context.setLastAccessAt(LocalDateTime.now());
        return context.getChatMemory().messages();
    }
    
    /**
     * 清除用户的对话上下文
     * 
     * @param userId 用户ID
     */
    public void clearContext(Long userId) {
        ConversationContext removed = contexts.remove(userId);
        if (removed != null) {
            log.info("清除用户 {} 的对话上下文", userId);
        }
    }
    
    /**
     * 设置用户上下文数据
     * 
     * @param userId 用户ID
     * @param key 键
     * @param value 值
     */
    public void setContextData(Long userId, String key, Object value) {
        ConversationContext context = getOrCreateContext(userId);
        context.getContextData().put(key, value);
        log.debug("用户 {} 设置上下文数据: {} = {}", userId, key, value);
    }
    
    /**
     * 获取用户上下文数据
     * 
     * @param userId 用户ID
     * @param key 键
     * @return 值
     */
    public Object getContextData(Long userId, String key) {
        ConversationContext context = contexts.get(userId);
        if (context == null) {
            return null;
        }
        return context.getContextData().get(key);
    }
    
    /**
     * 清理过期的上下文
     * 可以通过定时任务定期调用
     */
    public void cleanupExpiredContexts() {
        long now = System.currentTimeMillis();
        int removed = 0;
        
        for (Map.Entry<Long, ConversationContext> entry : contexts.entrySet()) {
            ConversationContext context = entry.getValue();
            long lastAccess = java.sql.Timestamp.valueOf(context.getLastAccessAt()).getTime();
            
            if (now - lastAccess > CONTEXT_EXPIRY_MS) {
                contexts.remove(entry.getKey());
                removed++;
                log.info("清理用户 {} 的过期上下文（最后访问：{}）", 
                        entry.getKey(), context.getLastAccessAt());
            }
        }
        
        if (removed > 0) {
            log.info("清理了 {} 个过期的对话上下文", removed);
        }
    }
    
    /**
     * 获取当前活跃的上下文数量
     * 
     * @return 上下文数量
     */
    public int getActiveContextCount() {
        return contexts.size();
    }
    
    /**
     * 对话上下文数据类
     */
    @Data
    public static class ConversationContext {
        private Long userId;
        private ChatMemory chatMemory;
        private LocalDateTime createdAt;
        private LocalDateTime lastAccessAt;
        private int messageCount = 0;
        private Map<String, Object> contextData = new ConcurrentHashMap<>();
        
        /**
         * 获取对话持续时长（分钟）
         */
        public long getDurationMinutes() {
            return java.time.Duration.between(createdAt, LocalDateTime.now()).toMinutes();
        }
    }
}
