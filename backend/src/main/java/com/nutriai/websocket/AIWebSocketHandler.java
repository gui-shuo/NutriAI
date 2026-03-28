package com.nutriai.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriai.service.AIStreamingService;
import com.nutriai.service.MemberPermissionService;
import com.nutriai.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

/**
 * AI聊天WebSocket处理器
 * 处理WebSocket连接、消息和流式输出
 * 
 * @author NutriAI Team
 * @date 2025-12-03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AIWebSocketHandler extends TextWebSocketHandler {
    
    private final WebSocketSessionManager sessionManager;
    private final AIStreamingService aiStreamingService;
    private final MemberPermissionService memberPermissionService;
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;
    
    /**
     * WebSocket连接建立后调用
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        log.info("🟢 WebSocket连接已建立: sessionId={}", sessionId);
        
        // 从查询参数或URL中获取token
        String token = extractToken(session);
        
        if (token == null || token.isEmpty()) {
            log.warn("❌ WebSocket连接缺少token: sessionId={}", sessionId);
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Missing authentication token"));
            return;
        }
        
        // 验证token并获取用户ID（这里简化处理，实际应使用JWT工具类）
        try {
            Long userId = validateTokenAndGetUserId(token);
            sessionManager.addSession(userId, session);
            
            // 发送连接成功消息
            sendMessage(session, Map.of(
                "type", "connection",
                "status", "success",
                "message", "WebSocket连接成功",
                "userId", userId,
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            log.warn("Token验证失败: sessionId={}, reason={}", sessionId, e.getMessage());
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Invalid token"));
        }
    }
    
    /**
     * 接收到客户端消息时调用
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        Long userId = sessionManager.getUserIdBySessionId(sessionId);
        
        if (userId == null) {
            log.warn("❌ 未找到用户会话: sessionId={}", sessionId);
            return;
        }
        
        String payload = message.getPayload();
        log.info("📨 收到WebSocket消息: userId={}, 消息长度={}", userId, payload.length());
        
        try {
            // 解析消息
            @SuppressWarnings("unchecked")
            Map<String, Object> request = objectMapper.readValue(payload, Map.class);
            
            String type = (String) request.get("type");
            
            if ("chat".equals(type)) {
                handleChatMessage(session, userId, request);
            } else if ("ping".equals(type)) {
                handlePingMessage(session);
            } else {
                log.warn("⚠️ 未知消息类型: {}", type);
                sendError(session, "未知的消息类型: " + type);
            }
            
        } catch (Exception e) {
            log.error("❌ 处理WebSocket消息失败: userId={}", userId, e);
            sendError(session, "消息处理失败: " + e.getMessage());
        }
    }
    
    /**
     * WebSocket连接关闭后调用
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        Long userId = sessionManager.getUserIdBySessionId(sessionId);
        
        log.info("🔴 WebSocket连接已关闭: sessionId={}, userId={}, status={}", 
            sessionId, userId, status);
        
        if (userId != null) {
            sessionManager.removeSession(userId);
        }
    }
    
    /**
     * WebSocket传输错误时调用
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        Long userId = sessionManager.getUserIdBySessionId(sessionId);
        
        if (exception instanceof java.io.EOFException || exception instanceof java.io.IOException) {
            log.debug("WebSocket连接断开(客户端关闭): sessionId={}, userId={}", sessionId, userId);
        } else {
            log.warn("WebSocket传输错误: sessionId={}, userId={}, error={}", sessionId, userId, exception.getMessage());
        }
        
        if (userId != null) {
            sessionManager.removeSession(userId);
        }
    }
    
    /**
     * 处理聊天消息（流式输出）
     */
    private void handleChatMessage(WebSocketSession session, Long userId, Map<String, Object> request) {
        String userMessage = (String) request.get("message");
        String model = (String) request.get("model");
        Double temperature = request.get("temperature") != null ? 
            ((Number) request.get("temperature")).doubleValue() : null;
        Integer maxTokens = request.get("maxTokens") != null ? 
            ((Number) request.get("maxTokens")).intValue() : null;
        Boolean keepContext = (Boolean) request.get("keepContext");
        
        log.info("💬 处理聊天消息: userId={}, message={}, model={}, temperature={}, maxTokens={}, keepContext={}", 
            userId, 
            userMessage != null ? userMessage.substring(0, Math.min(userMessage.length(), 50)) : null,
            model,
            temperature,
            maxTokens,
            keepContext);
        
        if (userMessage == null || userMessage.trim().isEmpty()) {
            sendError(session, "消息不能为空");
            return;
        }
        
        // 检查AI配额
        if (!memberPermissionService.checkAiQuota(userId)) {
            int quota = memberPermissionService.getDailyQuota(userId);
            sendError(session, "今日AI咨询次数已达上限（" + quota + "次/天），升级会员可享受更多次数");
            return;
        }
        
        // 消耗配额
        memberPermissionService.consumeAiQuota(userId);
        
        // 发送开始消息
        sendMessage(session, Map.of(
            "type", "start",
            "message", "AI正在思考中...",
            "timestamp", System.currentTimeMillis()
        ));
        
        // 调用流式AI服务
        aiStreamingService.chatStreaming(
            userId, 
            userMessage,
            model,
            temperature,
            maxTokens,
            keepContext,
            // 流式回调
            (chunk, isComplete) -> {
                try {
                    if (isComplete) {
                        // 发送完成消息
                        sendMessage(session, Map.of(
                            "type", "complete",
                            "timestamp", System.currentTimeMillis()
                        ));
                    } else {
                        // 发送流式数据块
                        sendMessage(session, Map.of(
                            "type", "chunk",
                            "content", chunk,
                            "timestamp", System.currentTimeMillis()
                        ));
                    }
                } catch (Exception e) {
                    log.error("发送流式数据失败", e);
                }
            },
            // 错误回调
            error -> {
                log.error("AI流式输出错误: userId={}", userId, error);
                sendError(session, "AI服务异常: " + error.getMessage());
            }
        );
    }
    
    /**
     * 处理心跳消息
     */
    private void handlePingMessage(WebSocketSession session) {
        sendMessage(session, Map.of(
            "type", "pong",
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * 发送消息到客户端
     */
    private void sendMessage(WebSocketSession session, Map<String, Object> data) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(data);
                session.sendMessage(new TextMessage(json));
            }
        } catch (Exception e) {
            log.error("发送WebSocket消息失败", e);
        }
    }
    
    /**
     * 发送错误消息
     */
    private void sendError(WebSocketSession session, String errorMessage) {
        sendMessage(session, Map.of(
            "type", "error",
            "message", errorMessage,
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * 从WebSocket会话中提取token
     */
    private String extractToken(WebSocketSession session) {
        // 从查询参数中获取token
        String uri = session.getUri().toString();
        if (uri.contains("token=")) {
            String[] parts = uri.split("token=");
            if (parts.length > 1) {
                String token = parts[1];
                // 移除可能的其他参数
                int ampersandIndex = token.indexOf('&');
                if (ampersandIndex > 0) {
                    token = token.substring(0, ampersandIndex);
                }
                return token;
            }
        }
        return null;
    }
    
    /**
     * 验证token并获取用户ID
     */
    private Long validateTokenAndGetUserId(String token) throws IllegalArgumentException {
        // 移除Bearer前缀（如果有）
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        // 验证token有效性
        if (!jwtUtil.validateToken(token)) {
            throw new IllegalArgumentException("Token无效或已过期");
        }
        
        // 获取用户ID
        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            throw new IllegalArgumentException("无法从 Token 中获取用户ID");
        }
        
        log.debug("Token验证成功: userId={}", userId);
        return userId;
    }
}
