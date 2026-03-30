package com.nutriai.config;

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

    @Value("${tencent.cos.custom-domain:}")
    private String customDomain;

    @Value("${cors.allowed-origins:*}")
    private String allowedOrigins;

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
     * 获取COS访问域名
     */
    public String getCosBaseUrl() {
        return "https://" + bucket + ".cos." + region + ".myqcloud.com";
    }

    /**
     * 获取COS下载域名（优先使用自定义域名，用于APK等受限文件）
     */
    public String getDownloadBaseUrl() {
        if (customDomain != null && !customDomain.isBlank()) {
            return "https://" + customDomain;
        }
        return getCosBaseUrl();
    }
}
