package com.nutriai.config;

import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI配置类 - 基于 OpenAI 兼容协议，支持任意大模型供应商
 * 通过环境变量 AI_BASE_URL / AI_API_KEY / AI_MODEL_NAME 切换供应商：
 *   - 阿里云 DashScope: https://dashscope.aliyuncs.com/compatible-mode/v1
 *   - 火山引擎(豆包):    https://ark.cn-beijing.volces.com/api/v3
 *   - DeepSeek:         https://api.deepseek.com/v1
 *   - Moonshot (Kimi):  https://api.moonshot.cn/v1
 *   - 硅基流动:          https://api.siliconflow.cn/v1
 *   - 其他 OpenAI 兼容服务
 */
@Slf4j
@Configuration
public class AIConfig {

    @Value("${ai.api-key:}")
    @Getter
    private String apiKey;

    @Value("${ai.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    @Getter
    private String apiUrl;

    @Value("${ai.model-name:qwen-turbo}")
    @Getter
    private String modelName;

    @Value("${ai.max-tokens:2000}")
    @Getter
    private Integer maxTokens;

    @Value("${ai.temperature:0.7}")
    @Getter
    private Double temperature;

    @Value("${ai.top-p:0.9}")
    @Getter
    private Double topP;

    @Value("${ai.timeout:180}")
    @Getter
    private Integer timeout;

    private final ConcurrentHashMap<String, ChatLanguageModel> chatModelCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, StreamingChatLanguageModel> streamingModelCache = new ConcurrentHashMap<>();

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        log.info("初始化 ChatLanguageModel (OpenAI兼容) - 模型: {}, API URL: {}, Timeout: {}秒", modelName, apiUrl, timeout);
        return buildChatModel(modelName, temperature, maxTokens);
    }

    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        log.info("初始化 StreamingChatLanguageModel (OpenAI兼容) - 模型: {}, API URL: {}", modelName, apiUrl);
        return buildStreamingChatModel(modelName, temperature, maxTokens);
    }

    public ChatLanguageModel getChatModel(String model, Double temp, Integer tokens) {
        String actualModel = model != null ? model : modelName;
        Double actualTemp = temp != null ? temp : temperature;
        Integer actualTokens = tokens != null ? tokens : maxTokens;
        String cacheKey = actualModel + ":" + actualTemp + ":" + actualTokens;
        return chatModelCache.computeIfAbsent(cacheKey, k -> {
            log.info("创建ChatLanguageModel: model={}, temp={}, tokens={}", actualModel, actualTemp, actualTokens);
            return buildChatModel(actualModel, actualTemp, actualTokens);
        });
    }

    public StreamingChatLanguageModel getStreamingChatModel(String model, Double temp, Integer tokens) {
        String actualModel = model != null ? model : modelName;
        Double actualTemp = temp != null ? temp : temperature;
        Integer actualTokens = tokens != null ? tokens : maxTokens;
        String cacheKey = actualModel + ":" + actualTemp + ":" + actualTokens;
        return streamingModelCache.computeIfAbsent(cacheKey, k -> {
            log.info("创建StreamingChatLanguageModel: model={}, temp={}, tokens={}", actualModel, actualTemp, actualTokens);
            return buildStreamingChatModel(actualModel, actualTemp, actualTokens);
        });
    }

    private ChatLanguageModel buildChatModel(String model, Double temp, Integer tokens) {
        return OpenAiChatModel.builder()
                .baseUrl(getEffectiveUrl())
                .apiKey(getEffectiveKey())
                .modelName(model)
                .temperature(temp)
                .topP(topP)
                .maxTokens(tokens)
                .timeout(Duration.ofSeconds(timeout))
                .tokenizer(new SimpleEstimateTokenizer())
                .build();
    }

    private StreamingChatLanguageModel buildStreamingChatModel(String model, Double temp, Integer tokens) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(getEffectiveUrl())
                .apiKey(getEffectiveKey())
                .modelName(model)
                .temperature(temp)
                .topP(topP)
                .maxTokens(tokens)
                .timeout(Duration.ofSeconds(timeout))
                .tokenizer(new SimpleEstimateTokenizer())
                .build();
    }

    private String getEffectiveKey() {
        String effectiveKey = (apiKey != null && !apiKey.isEmpty()) ? apiKey : "not-configured";
        if ("not-configured".equals(effectiveKey)) {
            log.warn("⚠️ AI API Key未配置！请设置环境变量: AI_API_KEY");
        }
        return effectiveKey;
    }

    private String getEffectiveUrl() {
        if (apiUrl != null && !apiUrl.isEmpty()) {
            return apiUrl;
        }
        return "https://dashscope.aliyuncs.com/compatible-mode/v1";
    }

    /**
     * 简单token估算器，避免jtokkit不识别非OpenAI模型名称
     * 按字符数粗略估算（中文约1.5 token/字，英文约0.25 token/字）
     */
    private static class SimpleEstimateTokenizer implements Tokenizer {
        @Override
        public int estimateTokenCountInText(String text) {
            if (text == null || text.isEmpty()) return 0;
            return (int) (text.length() * 0.6);
        }

        @Override
        public int estimateTokenCountInMessage(dev.langchain4j.data.message.ChatMessage message) {
            return estimateTokenCountInText(message.text()) + 4;
        }

        @Override
        public int estimateTokenCountInMessages(Iterable<dev.langchain4j.data.message.ChatMessage> messages) {
            int total = 3;
            for (dev.langchain4j.data.message.ChatMessage msg : messages) {
                total += estimateTokenCountInMessage(msg);
            }
            return total;
        }

        @Override
        public int estimateTokenCountInToolExecutionRequests(Iterable<dev.langchain4j.agent.tool.ToolExecutionRequest> requests) {
            return 0;
        }

        @Override
        public int estimateTokenCountInToolSpecifications(Iterable<dev.langchain4j.agent.tool.ToolSpecification> specs) {
            return 0;
        }
    }
}
