package com.nutriai.config;

import com.baidu.aip.imageclassify.AipImageClassify;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * 百度AI配置
 */
@Slf4j
@Configuration
public class BaiduAiConfig {
    
    @Value("${baidu.ai.app-id:}")
    private String appId;
    
    @Value("${baidu.ai.api-key:}")
    private String apiKey;
    
    @Value("${baidu.ai.secret-key:}")
    private String secretKey;
    
    /**
     * 创建百度图像识别客户端
     */
    @Bean
    @Conditional(BaiduAiCredentialsCondition.class)
    public AipImageClassify aipImageClassify() {
        try {
            AipImageClassify client = new AipImageClassify(appId, apiKey, secretKey);
            
            // 可选：设置网络参数
            client.setConnectionTimeoutInMillis(2000);
            client.setSocketTimeoutInMillis(60000);
            
            log.info("✓ 百度AI图像识别客户端初始化成功");
            log.info("✓ App ID: {}", appId);
            
            return client;
            
        } catch (Exception e) {
            log.error("❌ 百度AI图像识别客户端初始化失败", e);
            throw new RuntimeException("百度AI图像识别客户端初始化失败: " + e.getMessage());
        }
    }

    public static class BaiduAiCredentialsCondition implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String appId = context.getEnvironment().getProperty("baidu.ai.app-id");
            String apiKey = context.getEnvironment().getProperty("baidu.ai.api-key");
            String secretKey = context.getEnvironment().getProperty("baidu.ai.secret-key");
            return StringUtils.hasText(appId)
                && StringUtils.hasText(apiKey)
                && StringUtils.hasText(secretKey);
        }
    }
}
