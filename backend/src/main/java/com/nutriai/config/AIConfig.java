package com.nutriai.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.dashscope.QwenStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI配置类 - 配置LangChain4j和通义千问
 * 
 * @author NutriAI Team
 * @date 2025-12-03
 */
@Slf4j
@Configuration
public class AIConfig {
    
    @Value("${tongyi.api-key}")
    private String apiKey;
    
    @Value("${tongyi.model:qwen-max}")
    private String modelName;
    
    @Value("${tongyi.max-tokens:2000}")
    private Integer maxTokens;
    
    @Value("${tongyi.temperature:0.7}")
    private Double temperature;
    
    @Value("${tongyi.top-p:0.9}")
    private Double topP;
    
    /**
     * 配置同步聊天模型
     * 用于需要等待完整响应的场景
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        log.info("初始化ChatLanguageModel - 模型: {}, MaxTokens: {}", modelName, maxTokens);
        
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("通义千问API Key未配置！请在application.yml中设置 tongyi.api-key");
            log.warn("或设置环境变量: TONGYI_API_KEY");
        }
        
        return QwenChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }
    
    /**
     * 配置流式聊天模型
     * 用于实时流式输出场景（WebSocket）
     */
    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        log.info("初始化StreamingChatLanguageModel - 模型: {}, MaxTokens: {}", modelName, maxTokens);
        
        return QwenStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }
}
