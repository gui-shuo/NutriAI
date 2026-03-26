package com.nutriai.service.sms;

/**
 * 短信发送提供者接口
 */
public interface SmsProvider {

    /**
     * 发送短信验证码
     *
     * @param phone  手机号
     * @param code   验证码
     * @return 是否发送成功
     */
    boolean sendVerificationCode(String phone, String code);

    /**
     * 获取提供者名称
     */
    String getProviderName();
}
