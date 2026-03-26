package com.nutriai.config;

import com.nutriai.service.sms.MockSmsProvider;
import com.nutriai.service.sms.SmsProvider;
import com.nutriai.service.sms.TencentSmsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 短信服务配置
 * 根据配置自动选择短信提供者：
 * - 配置了腾讯云SMS密钥 → TencentSmsProvider
 * - 未配置 → MockSmsProvider（开发环境）
 */
@Slf4j
@Configuration
public class SmsConfig {

    @Value("${sms.tencent.secret-id:}")
    private String secretId;

    @Value("${sms.tencent.secret-key:}")
    private String secretKey;

    @Value("${sms.tencent.sdk-app-id:}")
    private String sdkAppId;

    @Value("${sms.tencent.sign-name:NutriAI}")
    private String signName;

    @Value("${sms.tencent.template-id:}")
    private String templateId;

    @Bean
    public SmsProvider smsProvider() {
        if (isConfigured()) {
            log.info("SMS Provider: TencentCloud (sdkAppId={})", sdkAppId);
            return new TencentSmsProvider(secretId, secretKey, sdkAppId, signName, templateId);
        } else {
            log.warn("SMS Provider: Mock (腾讯云SMS未配置，使用模拟短信。配置sms.tencent.*启用真实短信)");
            return new MockSmsProvider();
        }
    }

    private boolean isConfigured() {
        return secretId != null && !secretId.isEmpty()
                && secretKey != null && !secretKey.isEmpty()
                && sdkAppId != null && !sdkAppId.isEmpty()
                && templateId != null && !templateId.isEmpty();
    }
}
