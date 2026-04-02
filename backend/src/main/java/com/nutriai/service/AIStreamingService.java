package com.nutriai.service;

import com.nutriai.ai.AIToolkit;
import com.nutriai.ai.ConversationContextManager;
import com.nutriai.config.AIConfig;
import com.nutriai.entity.AIChatLog;
import com.nutriai.repository.AIChatLogRepository;
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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIStreamingService {
    
    private final StreamingChatLanguageModel streamingChatModel;
    private final AIConfig aiConfig;
    private final ConversationContextManager contextManager;
    private final AIToolkit toolkit;
    private final com.nutriai.ai.NutritionKnowledgeBase knowledgeBase;
    private final AIChatLogRepository chatLogRepository;
    
    public void chatStreaming(Long userId, String userMessage,
                            String model, Double temperature, Integer maxTokens, Boolean keepContext,
                            BiConsumer<String, Boolean> onChunk,
                            Consumer<Throwable> onError) {
        
        String actualModel = model != null ? model : aiConfig.getEffectiveModelName();
        Double actualTemperature = temperature != null ? temperature : 0.7;
        Integer actualMaxTokens = maxTokens != null ? maxTokens : 2000;
        Boolean actualKeepContext = keepContext != null ? keepContext : true;
        long startTime = System.currentTimeMillis();
        
        log.info("🚀 开始流式输出: userId={}, model={}, temperature={}, maxTokens={}, keepContext={}", 
            userId, actualModel, actualTemperature, actualMaxTokens, actualKeepContext);
        
        try {
            if (toolkit.containsSensitiveContent(userMessage)) {
                log.warn("⚠️ 消息包含敏感内容: userId={}", userId);
                String warning = "您的问题涉及医疗诊断，建议咨询专业医生。" + toolkit.getMedicalDisclaimer();
                onChunk.accept(warning, true);
                saveLog(userId, userMessage, warning, actualModel, 0, System.currentTimeMillis() - startTime, "success", null);
                return;
            }
            
            List<ChatMessage> existingHistory = contextManager.getMessageHistory(userId);
            if (existingHistory.isEmpty()) {
                String systemPrompt = knowledgeBase.getEnhancedSystemPrompt(null);
                contextManager.addSystemMessage(userId, systemPrompt);
                log.info("用户 {} 首次流式对话，自动注入知识库系统提示词", userId);
            }
            
            contextManager.addUserMessage(userId, userMessage);
            
            List<ChatMessage> messageHistory;
            if (actualKeepContext) {
                messageHistory = contextManager.getMessageHistory(userId);
            } else {
                messageHistory = contextManager.getMessageHistory(userId);
                if (messageHistory.size() > 2) {
                    messageHistory = List.of(
                        messageHistory.get(0),
                        messageHistory.get(messageHistory.size() - 1)
                    );
                }
            }
            
            StringBuilder fullResponse = new StringBuilder();
            
            StreamingResponseHandler<AiMessage> handler = new StreamingResponseHandler<AiMessage>() {
                @Override
                public void onNext(String token) {
                    fullResponse.append(token);
                    onChunk.accept(token, false);
                }
                
                @Override
                public void onComplete(Response<AiMessage> response) {
                    try {
                        String aiResponse = fullResponse.toString();
                        contextManager.addAiMessage(userId, aiResponse);
                        String formattedResponse = toolkit.formatAIResponse(aiResponse);
                        
                        // 估算token数（中文约1.5token/字，英文约0.25token/字）
                        int estimatedTokens = estimateTokens(userMessage) + estimateTokens(aiResponse);
                        long elapsed = System.currentTimeMillis() - startTime;
                        
                        log.info("✅ 流式输出完成: userId={}, 长度={}, tokens≈{}, 耗时={}ms",
                                userId, formattedResponse.length(), estimatedTokens, elapsed);
                        
                        saveLog(userId, userMessage, aiResponse, actualModel, estimatedTokens, elapsed, "success", null);
                        onChunk.accept("", true);
                    } catch (Exception e) {
                        log.error("❌ 流式输出完成处理失败", e);
                        onError.accept(e);
                    }
                }
                
                @Override
                public void onError(Throwable error) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    log.error("❌ 流式输出错误: userId={}", userId, error);
                    saveLog(userId, userMessage, null, actualModel, 0, elapsed, "error", error.getMessage());
                    onError.accept(error);
                }
            };
            
            StreamingChatLanguageModel targetModel = aiConfig.getStreamingChatModel(actualModel, actualTemperature, actualMaxTokens);
            targetModel.generate(messageHistory, handler);
            
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("❌ 流式聊天失败: userId={}", userId, e);
            saveLog(userId, userMessage, null, actualModel, 0, elapsed, "error", e.getMessage());
            onError.accept(e);
        }
    }
    
    public void cancelStreaming(Long userId) {
        log.info("⏹️ 取消流式输出: userId={}", userId);
    }

    private void saveLog(Long userId, String userMessage, String aiResponse,
                         String model, int tokens, long elapsedMs, String status, String errorMsg) {
        try {
            AIChatLog logEntry = AIChatLog.builder()
                    .userId(userId)
                    .userMessage(userMessage != null && userMessage.length() > 5000 ? userMessage.substring(0, 5000) : userMessage)
                    .aiResponse(aiResponse != null && aiResponse.length() > 10000 ? aiResponse.substring(0, 10000) : aiResponse)
                    .model(model)
                    .tokensUsed(tokens)
                    .responseTime((int) elapsedMs)
                    .status(status)
                    .errorMessage(errorMsg)
                    .build();
            chatLogRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("保存AI日志失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        int cjk = 0, other = 0;
        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) cjk++;
            else other++;
        }
        return (int) (cjk * 1.5 + other * 0.25);
    }
}
