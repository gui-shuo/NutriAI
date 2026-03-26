package com.nutriai.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.dashscope.QwenStreamingChatModel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;

/**
 * AI配置类 - 配置LangChain4j和通义千问
 * 支持按请求动态切换模型参数（缓存复用）
 * 
 * @author NutriAI Team
 * @date 2025-12-03
 */
@Slf4j
@Configuration
public class AIConfig {
    
    @Value("${tongyi.api-key}")
    @Getter
    private String apiKey;
    
    @Value("${tongyi.model:qwen-max}")
    @Getter
    private String modelName;
    
    @Value("${tongyi.max-tokens:2000}")
    @Getter
    private Integer maxTokens;
    
    @Value("${tongyi.temperature:0.7}")
    @Getter
    private Double temperature;
    
    @Value("${tongyi.top-p:0.9}")
    @Getter
    private Double topP;
    
    @Value("${tongyi.timeout:180}")
    @Getter
    private Integer timeout;

    // 模型缓存：按配置组合键缓存模型
    private final ConcurrentHashMap<String, ChatLanguageModel> chatModelCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, StreamingChatLanguageModel> streamingModelCache = new ConcurrentHashMap<>();
    
    /**
     * 默认同步聊天模型（Spring Bean）
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        log.info("初始化默认ChatLanguageModel - 模型: {}, Timeout: {}秒", modelName, timeout);
        
        String effectiveKey = getEffectiveKey();
        
        System.setProperty("dashscope.api.connect.timeout", String.valueOf(timeout * 1000));
        System.setProperty("dashscope.api.read.timeout", String.valueOf(timeout * 1000));
        System.setProperty("dashscope.api.write.timeout", String.valueOf(timeout * 1000));
        
        return QwenChatModel.builder()
                .apiKey(effectiveKey)
                .modelName(modelName)
                .build();
    }
    
    /**
     * 默认流式聊天模型（Spring Bean）
     */
    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        log.info("初始化默认StreamingChatLanguageModel - 模型: {}", modelName);
        
        String effectiveKey = getEffectiveKey();
        
        return QwenStreamingChatModel.builder()
                .apiKey(effectiveKey)
                .modelName(modelName)
                .build();
    }

    /**
     * 获取指定参数的同步聊天模型（带缓存）
     */
    public ChatLanguageModel getChatModel(String model, Double temp, Integer tokens) {
        String actualModel = model != null ? model : modelName;
        Double actualTemp = temp != null ? temp : temperature;
        Integer actualTokens = tokens != null ? tokens : maxTokens;
        
        String cacheKey = actualModel + ":" + actualTemp + ":" + actualTokens;
        
        return chatModelCache.computeIfAbsent(cacheKey, k -> {
            log.info("创建ChatLanguageModel: model={}, temp={}, tokens={}", actualModel, actualTemp, actualTokens);
            return QwenChatModel.builder()
                    .apiKey(getEffectiveKey())
                    .modelName(actualModel)
                    .temperature(actualTemp.floatValue())
                    .topP(topP)
                    .build();
        });
    }

    /**
     * 获取指定参数的流式聊天模型（带缓存）
     */
    public StreamingChatLanguageModel getStreamingChatModel(String model, Double temp, Integer tokens) {
        String actualModel = model != null ? model : modelName;
        Double actualTemp = temp != null ? temp : temperature;
        Integer actualTokens = tokens != null ? tokens : maxTokens;
        
        String cacheKey = actualModel + ":" + actualTemp + ":" + actualTokens;
        
        return streamingModelCache.computeIfAbsent(cacheKey, k -> {
            log.info("创建StreamingChatLanguageModel: model={}, temp={}, tokens={}", actualModel, actualTemp, actualTokens);
            return QwenStreamingChatModel.builder()
                    .apiKey(getEffectiveKey())
                    .modelName(actualModel)
                    .temperature(actualTemp.floatValue())
                    .topP(topP)
                    .build();
        });
    }

    private String getEffectiveKey() {
        String effectiveKey = (apiKey != null && !apiKey.isEmpty()) ? apiKey : "not-configured";
        if ("not-configured".equals(effectiveKey)) {
            log.warn("⚠️ 通义千问API Key未配置！请设置环境变量: TONGYI_API_KEY");
        }
        return effectiveKey;
    }
}
