package com.nutriai.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝支付配置
 * 支持沙箱(sandbox)与生产(prod)双模式切换
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "alipay")
public class AlipayConfig {

    /** 是否启用沙箱模式（开发/测试用） */
    private boolean sandbox = true;

    /** 应用ID（AppID） */
    private String appId;

    /** 应用私钥（RSA2，PKCS8格式，不含头尾及换行） */
    private String privateKey;

    /** 支付宝公钥（从支付宝开放平台获取，用于验签回调） */
    private String alipayPublicKey;

    /** 异步通知地址（必须公网可达，支付宝服务端回调） */
    private String notifyUrl;

    /** 同步跳转地址（用户支付完成后浏览器跳转） */
    private String returnUrl;

    // ---- 固定常量，无需外部配置 ----
    private static final String GATEWAY_URL_PROD    = "https://openapi.alipay.com/gateway.do";
    private static final String GATEWAY_URL_SANDBOX = "https://openapi-sandbox.dl.alipay.com/gateway.do";
    private static final String FORMAT              = "json";
    private static final String CHARSET             = "UTF-8";
    private static final String SIGN_TYPE           = "RSA2";
    private static final String PROD_ALIPAY_PUB_KEY_TYPE = "ALIPAY_PUBLIC_KEY";

    @Bean
    public AlipayClient alipayClient() {
        String gatewayUrl = sandbox ? GATEWAY_URL_SANDBOX : GATEWAY_URL_PROD;
        log.info("初始化支付宝客户端，模式: {}, 网关: {}", sandbox ? "沙箱" : "生产", gatewayUrl);
        return new DefaultAlipayClient(
                gatewayUrl,
                appId,
                privateKey,
                FORMAT,
                CHARSET,
                alipayPublicKey,
                SIGN_TYPE
        );
    }
}
