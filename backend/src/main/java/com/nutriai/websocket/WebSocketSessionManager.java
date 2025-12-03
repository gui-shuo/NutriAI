package com.nutriai.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket会话管理器
 * 管理所有活跃的WebSocket连接
 * 
 * @author NutriAI Team
 * @date 2025-12-03
 */
@Slf4j
@Component
public class WebSocketSessionManager {
    
    // 存储所有活跃会话：userId -> WebSocketSession
    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    // 存储会话ID到用户ID的映射：sessionId -> userId
    private final Map<String, Long> sessionToUser = new ConcurrentHashMap<>();
    
    /**
     * 添加会话
     * 
     * @param userId 用户ID
     * @param session WebSocket会话
     */
    public void addSession(Long userId, WebSocketSession session) {
        // 移除该用户的旧会话
        removeSession(userId);
        
        sessions.put(userId, session);
        sessionToUser.put(session.getId(), userId);
        
        log.info("✅ WebSocket会话已添加: userId={}, sessionId={}, 当前在线: {}", 
            userId, session.getId(), sessions.size());
    }
    
    /**
     * 移除会话（通过用户ID）
     * 
     * @param userId 用户ID
     */
    public void removeSession(Long userId) {
        WebSocketSession session = sessions.remove(userId);
        if (session != null) {
            sessionToUser.remove(session.getId());
            try {
                if (session.isOpen()) {
                    session.close();
                }
            } catch (IOException e) {
                log.error("关闭WebSocket会话失败: userId={}", userId, e);
            }
            log.info("🔴 WebSocket会话已移除: userId={}, 当前在线: {}", userId, sessions.size());
        }
    }
    
    /**
     * 移除会话（通过会话ID）
     * 
     * @param sessionId 会话ID
     */
    public void removeSessionBySessionId(String sessionId) {
        Long userId = sessionToUser.remove(sessionId);
        if (userId != null) {
            sessions.remove(userId);
            log.info("🔴 WebSocket会话已移除: sessionId={}, userId={}, 当前在线: {}", 
                sessionId, userId, sessions.size());
        }
    }
    
    /**
     * 获取用户的会话
     * 
     * @param userId 用户ID
     * @return WebSocket会话，如果不存在返回null
     */
    public WebSocketSession getSession(Long userId) {
        return sessions.get(userId);
    }
    
    /**
     * 根据会话ID获取用户ID
     * 
     * @param sessionId 会话ID
     * @return 用户ID，如果不存在返回null
     */
    public Long getUserIdBySessionId(String sessionId) {
        return sessionToUser.get(sessionId);
    }
    
    /**
     * 检查用户是否在线
     * 
     * @param userId 用户ID
     * @return true如果在线，否则false
     */
    public boolean isOnline(Long userId) {
        WebSocketSession session = sessions.get(userId);
        return session != null && session.isOpen();
    }
    
    /**
     * 获取在线用户数
     * 
     * @return 在线用户数
     */
    public int getOnlineCount() {
        return sessions.size();
    }
    
    /**
     * 获取所有在线用户ID
     * 
     * @return 在线用户ID集合
     */
    public java.util.Set<Long> getOnlineUserIds() {
        return sessions.keySet();
    }
    
    /**
     * 关闭所有会话
     */
    public void closeAll() {
        log.info("关闭所有WebSocket会话，总数: {}", sessions.size());
        sessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.close();
                }
            } catch (IOException e) {
                log.error("关闭会话失败", e);
            }
        });
        sessions.clear();
        sessionToUser.clear();
        log.info("✅ 所有WebSocket会话已关闭");
    }
}
