package com.nutriai.service;

import com.nutriai.ai.AIToolkit;
import com.nutriai.ai.ConversationContextManager;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * AI流式输出服务
 * 提供流式响应功能，实现打字机效果
 * 
 * @author NutriAI Team
 * @date 2025-12-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIStreamingService {
    
    private final StreamingChatLanguageModel streamingChatModel;
    private final ConversationContextManager contextManager;
    private final AIToolkit toolkit;
    
    /**
     * 流式聊天
     * 
     * @param userId 用户ID
     * @param userMessage 用户消息
     * @param model 模型名称
     * @param temperature 温度参数
     * @param maxTokens 最大token数
     * @param keepContext 是否保持上下文
     * @param onChunk 流式数据回调 (chunk, isComplete)
     * @param onError 错误回调
     */
    public void chatStreaming(Long userId, String userMessage,
                            String model, Double temperature, Integer maxTokens, Boolean keepContext,
                            BiConsumer<String, Boolean> onChunk,
                            Consumer<Throwable> onError) {
        
        // 使用默认值
        String actualModel = model != null ? model : "qwen-max";
        Double actualTemperature = temperature != null ? temperature : 0.7;
        Integer actualMaxTokens = maxTokens != null ? maxTokens : 2000;
        Boolean actualKeepContext = keepContext != null ? keepContext : true;
        
        log.info("🚀 开始流式输出: userId={}, model={}, temperature={}, maxTokens={}, keepContext={}", 
            userId, actualModel, actualTemperature, actualMaxTokens, actualKeepContext);
        
        try {
            // 检查敏感内容
            if (toolkit.containsSensitiveContent(userMessage)) {
                log.warn("⚠️ 消息包含敏感内容: userId={}", userId);
                String warning = "您的问题涉及医疗诊断，建议咨询专业医生。" + toolkit.getMedicalDisclaimer();
                onChunk.accept(warning, true);
                return;
            }
            
            // 添加用户消息到上下文
            contextManager.addUserMessage(userId, userMessage);
            
            // 获取消息历史
            List<ChatMessage> messageHistory;
            if (actualKeepContext) {
                messageHistory = contextManager.getMessageHistory(userId);
                log.debug("使用上下文记忆，历史消息数: {}", messageHistory.size());
            } else {
                messageHistory = contextManager.getMessageHistory(userId);
                if (messageHistory.size() > 2) {
                    messageHistory = List.of(
                        messageHistory.get(0),
                        messageHistory.get(messageHistory.size() - 1)
                    );
                }
                log.debug("不使用上下文记忆");
            }
            
            // 用于累积完整响应
            StringBuilder fullResponse = new StringBuilder();
            
            // 创建流式响应处理器
            StreamingResponseHandler<AiMessage> handler = new StreamingResponseHandler<AiMessage>() {
                @Override
                public void onNext(String token) {
                    // 每次接收到新token就发送
                    fullResponse.append(token);
                    onChunk.accept(token, false);
                }
                
                @Override
                public void onComplete(Response<AiMessage> response) {
                    try {
                        String aiResponse = fullResponse.toString();
                        
                        // 添加AI响应到上下文
                        contextManager.addAiMessage(userId, aiResponse);
                        
                        // 格式化响应
                        String formattedResponse = toolkit.formatAIResponse(aiResponse);
                        
                        log.info("✅ 流式输出完成: userId={}, 总长度={}", userId, formattedResponse.length());
                        
                        // 发送完成信号
                        onChunk.accept("", true);
                        
                    } catch (Exception e) {
                        log.error("❌ 流式输出完成处理失败", e);
                        onError.accept(e);
                    }
                }
                
                @Override
                public void onError(Throwable error) {
                    log.error("❌ 流式输出错误: userId={}", userId, error);
                    onError.accept(error);
                }
            };
            
            // 调用流式模型
            streamingChatModel.generate(messageHistory, handler);
            
        } catch (Exception e) {
            log.error("❌ 流式聊天失败: userId={}", userId, e);
            onError.accept(e);
        }
    }
    
    /**
     * 取消流式输出
     * TODO: 实现取消功能
     * 
     * @param userId 用户ID
     */
    public void cancelStreaming(Long userId) {
        log.info("⏹️ 取消流式输出: userId={}", userId);
        // TODO: 实现取消逻辑
    }
}
