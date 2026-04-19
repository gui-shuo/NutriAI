package com.nutriai.config;

import com.nutriai.service.DynamicConfigService;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.BucketCrossOriginConfiguration;
import com.qcloud.cos.model.CORSRule;
import com.qcloud.cos.region.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@Getter
public class CosConfig {

    @Value("${tencent.cos.secret-id:}")
    private String secretId;

    @Value("${tencent.cos.secret-key:}")
    private String secretKey;

    @Value("${tencent.cos.region:ap-beijing}")
    private String region;

    @Value("${tencent.cos.bucket:nutriai-assets}")
    private String bucket;

    @Value("${tencent.cos.url:}")
    private String cosUrl;

    @Value("${tencent.cos.custom-domain:}")
    private String customDomain;

    @Value("${cors.allowed-origins:*}")
    private String allowedOrigins;

    @Autowired(required = false)
    private DynamicConfigService dynamicConfig;

    @Bean
    public COSClient cosClient() {
        COSCredentials credentials = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        clientConfig.setHttpProtocol(HttpProtocol.https);
        COSClient client = new COSClient(credentials, clientConfig);

        // 设置 COS 存储桶的 CORS 规则，允许前端跨域加载图片到 Canvas
        try {
            CORSRule rule = new CORSRule();
            if ("*".equals(allowedOrigins)) {
                rule.setAllowedOrigins(List.of("*"));
            } else {
                rule.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
            }
            rule.setAllowedMethods(Arrays.asList(
                    CORSRule.AllowedMethods.GET,
                    CORSRule.AllowedMethods.HEAD
            ));
            rule.setAllowedHeaders(List.of("*"));
            rule.setMaxAgeSeconds(3600);

            BucketCrossOriginConfiguration corsConfig = new BucketCrossOriginConfiguration();
            corsConfig.setRules(List.of(rule));
            client.setBucketCrossOriginConfiguration(bucket, corsConfig);
            log.info("COS 存储桶 CORS 规则已设置, allowedOrigins={}", allowedOrigins);
        } catch (Exception e) {
            log.warn("设置 COS CORS 规则失败（不影响上传，但跨域访问可能受限）: {}", e.getMessage());
        }

        return client;
    }

    /**
     * 获取 COS 默认访问域名
     */
    public String getDefaultCosBaseUrl() {
        return "https://" + bucket + ".cos." + region + ".myqcloud.com";
    }

    /**
     * 获取 COS 访问域名（优先使用 COS_URL，便于腾讯云内网智能解析）
     */
    public String getCosBaseUrl() {
        String domain = dynamicConfig != null
                ? dynamicConfig.getString("cos.url", "tencent.cos.url", cosUrl)
                : cosUrl;
        if (domain != null && !domain.isBlank()) {
            return normalizeUrl(domain);
        }
        return getDefaultCosBaseUrl();
    }

    /**
     * 获取 COS 下载域名（生产环境优先统一走 COS_URL，兼容历史自定义域名）
     */
    public String getDownloadBaseUrl() {
        String primaryDomain = dynamicConfig != null
                ? dynamicConfig.getString("cos.url", "tencent.cos.url", cosUrl)
                : cosUrl;
        if (primaryDomain != null && !primaryDomain.isBlank()) {
            return normalizeUrl(primaryDomain);
        }

        String legacyDownloadDomain = dynamicConfig != null
                ? dynamicConfig.getString("cos.custom_domain", "tencent.cos.custom-domain", customDomain)
                : customDomain;
        if (legacyDownloadDomain != null && !legacyDownloadDomain.isBlank()) {
            return normalizeUrl(legacyDownloadDomain);
        }

        return getDefaultCosBaseUrl();
    }

    private String normalizeUrl(String domain) {
        String normalized = domain.trim();
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://" + normalized;
        }
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }
}
