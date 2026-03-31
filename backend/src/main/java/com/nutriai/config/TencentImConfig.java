package com.nutriai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云即时通信IM配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "tencent.im")
public class TencentImConfig {

    /** SDKAppID */
    private long sdkAppId;

    /** 密钥 */
    private String secretKey;

    /** UserSig有效期（秒），默认7天 */
    private long expire = 604800;

    /** IM REST API管理员账号 */
    private static final String ADMIN_USER_ID = "administrator";

    /** IM REST API基础URL */
    private static final String API_BASE_URL = "https://console.tim.qq.com";

    public String getAdminUserId() {
        return ADMIN_USER_ID;
    }

    public String getApiBaseUrl() {
        return API_BASE_URL;
    }

    public boolean isEnabled() {
        return sdkAppId > 0 && secretKey != null && !secretKey.isBlank();
    }
}
