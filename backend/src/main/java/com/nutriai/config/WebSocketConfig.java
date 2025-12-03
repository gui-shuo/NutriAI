package com.nutriai.config;

import com.nutriai.websocket.AIWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置类
 * 配置WebSocket端点和拦截器
 * 
 * @author NutriAI Team
 * @date 2025-12-03
 */
@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    
    private final AIWebSocketHandler aiWebSocketHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        log.info("注册WebSocket处理器...");
        
        // 注册AI聊天WebSocket端点 - 原生WebSocket
        registry.addHandler(aiWebSocketHandler, "/ws/ai/chat")
                .setAllowedOriginPatterns("*")
                .setAllowedOrigins("http://localhost:3000", "http://localhost:5173", "http://localhost:8080");
        
        // 注册SockJS降级支持（可选）
        registry.addHandler(aiWebSocketHandler, "/ws/ai/chat-sockjs")
                .setAllowedOriginPatterns("*")
                .setAllowedOrigins("http://localhost:3000", "http://localhost:5173", "http://localhost:8080")
                .withSockJS();
        
        log.info("✅ WebSocket处理器注册完成:");
        log.info("   - /ws/ai/chat (原生WebSocket)");
        log.info("   - /ws/ai/chat-sockjs (SockJS降级支持)");
    }
}
