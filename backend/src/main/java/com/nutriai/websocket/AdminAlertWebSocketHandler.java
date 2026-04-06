package com.nutriai.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriai.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理后台告警WebSocket处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAlertWebSocketHandler extends TextWebSocketHandler {
    
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    
    // 存储所有管理员的WebSocket会话
    private final Map<String, WebSocketSession> adminSessions = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            String token = getTokenFromSession(session);
            
            if (token == null) {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Token required"));
                return;
            }
            
            if (!validateAdminToken(token)) {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid token or not admin"));
                return;
            }
            
            Long userId = jwtUtil.getUserIdFromToken(token);
            String sessionKey = "admin_" + userId;
            
            adminSessions.put(sessionKey, session);
            log.info("✅ 管理员WebSocket连接建立成功: userId={}, sessionId={}", userId, session.getId());
            
            // 发送欢迎消息
            sendWelcomeMessage(session);
        } catch (Exception e) {
            log.error("❌ WebSocket连接建立失败", e);
            session.close(CloseStatus.SERVER_ERROR.withReason("Internal error"));
        }
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("收到管理员消息: {}", payload);
        
        // 可以处理管理员发送的消息，例如订阅特定类型的告警
        // 这里暂时只做日志记录
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 从会话列表中移除
        adminSessions.values().remove(session);
        log.info("管理员WebSocket连接关闭: sessionId={}, status={}", session.getId(), status);
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        if (exception instanceof java.io.EOFException || exception instanceof java.io.IOException) {
            log.debug("管理员WebSocket连接断开(客户端关闭): sessionId={}", session.getId());
        } else {
            log.warn("管理员WebSocket传输错误: sessionId={}, error={}", session.getId(), exception.getMessage());
        }
        adminSessions.values().remove(session);
    }
    
    /**
     * 从会话中获取token
     */
    private String getTokenFromSession(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            log.debug("解析URI: {}", uri);
            
            if (uri == null) {
                log.warn("URI为空");
                return null;
            }
            
            String query = uri.getQuery();
            log.debug("查询参数: {}", query);
            
            if (query == null || query.isEmpty()) {
                log.warn("查询参数为空");
                return null;
            }
            
            // 解析查询参数
            String[] params = query.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                    String token = keyValue[1];
                    log.debug("提取到token: {}...", token.substring(0, Math.min(20, token.length())));
                    return token;
                }
            }
            
            log.warn("未找到token参数");
            return null;
        } catch (Exception e) {
            log.error("提取token失败", e);
            return null;
        }
    }
    
    /**
     * 验证管理员token
     */
    private boolean validateAdminToken(String token) {
        try {
            log.debug("开始验证token...");
            
            if (!jwtUtil.validateToken(token)) {
                log.warn("Token验证失败：无效的token");
                return false;
            }
            
            String role = jwtUtil.getRoleFromToken(token);
            log.debug("Token角色: {}", role);
            
            boolean isAdmin = "ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
            if (!isAdmin) {
                log.warn("Token验证失败：非管理员角色 ({})", role);
            }
            
            return isAdmin;
        } catch (Exception e) {
            log.error("Token验证异常", e);
            return false;
        }
    }
    
    /**
     * 发送欢迎消息
     */
    private void sendWelcomeMessage(WebSocketSession session) {
        try {
            Map<String, Object> welcome = Map.of(
                "level", "info",
                "title", "连接成功",
                "content", "管理后台告警系统已连接",
                "time", System.currentTimeMillis()
            );
            
            String message = objectMapper.writeValueAsString(welcome);
            session.sendMessage(new TextMessage(message));
        } catch (IOException e) {
            log.error("发送欢迎消息失败", e);
        }
    }
    
    /**
     * 广播告警消息给所有管理员
     */
    public void broadcastAlert(String level, String title, String content) {
        Map<String, Object> alert = Map.of(
            "level", level,
            "title", title,
            "content", content,
            "time", System.currentTimeMillis()
        );
        
        try {
            String message = objectMapper.writeValueAsString(alert);
            TextMessage textMessage = new TextMessage(message);
            
            adminSessions.values().forEach(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(textMessage);
                    }
                } catch (IOException e) {
                    log.error("发送告警消息失败: sessionId={}", session.getId(), e);
                }
            });
            
            log.info("告警消息已广播: level={}, title={}, 接收者数量={}", 
                    level, title, adminSessions.size());
        } catch (Exception e) {
            log.error("广播告警消息失败", e);
        }
    }
    
    /**
     * 发送告警给特定管理员
     */
    public void sendAlertToAdmin(Long userId, String level, String title, String content) {
        String sessionKey = "admin_" + userId;
        WebSocketSession session = adminSessions.get(sessionKey);
        
        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> alert = Map.of(
                    "level", level,
                    "title", title,
                    "content", content,
                    "time", System.currentTimeMillis()
                );
                
                String message = objectMapper.writeValueAsString(alert);
                session.sendMessage(new TextMessage(message));
                
                log.info("告警消息已发送给管理员: userId={}, level={}, title={}", 
                        userId, level, title);
            } catch (IOException e) {
                log.error("发送告警消息失败: userId={}", userId, e);
            }
        }
    }
    
    /**
     * 获取在线管理员数量
     */
    public int getOnlineAdminCount() {
        return adminSessions.size();
    }
}
