package com.nutriai.service.sms;

import lombok.extern.slf4j.Slf4j;

/**
 * 本地Mock短信提供者（开发环境使用）
 * 仅在日志中输出验证码，不实际发送
 */
@Slf4j
public class MockSmsProvider implements SmsProvider {

    @Override
    public boolean sendVerificationCode(String phone, String code) {
        log.info("【Mock短信】手机号: {}, 验证码: {} (开发环境，未实际发送)", phone, code);
        return true;
    }

    @Override
    public String getProviderName() {
        return "Mock";
    }
}
