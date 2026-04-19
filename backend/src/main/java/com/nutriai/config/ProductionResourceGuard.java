package com.nutriai.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductionResourceGuard implements ApplicationRunner {

    private static final Pattern PRIVATE_IPV4_PATTERN = Pattern.compile(
            "^(10\\..*|192\\.168\\..*|172\\.(1[6-9]|2\\d|3[0-1])\\..*)$"
    );

    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        if (Arrays.stream(environment.getActiveProfiles()).noneMatch("prod"::equals)) {
            return;
        }

        String dbHostNei = requireValue("DB_HOST_NEI", environment.getProperty("DB_HOST_NEI"));
        String cosUrl = requireValue("COS_URL", environment.getProperty("COS_URL", environment.getProperty("tencent.cos.url")));
        String datasourceUrl = environment.getProperty("spring.datasource.url", "");

        if (!PRIVATE_IPV4_PATTERN.matcher(dbHostNei.trim()).matches()) {
            throw new IllegalStateException("生产环境 DB_HOST_NEI 必须为腾讯云数据库内网 IP 地址，当前值: " + dbHostNei);
        }
        if (!datasourceUrl.contains("//" + dbHostNei + ":")) {
            throw new IllegalStateException("生产环境数据源未使用 DB_HOST_NEI，当前 datasource.url=" + datasourceUrl);
        }
        if (cosUrl.isBlank()) {
            throw new IllegalStateException("生产环境必须配置 COS_URL，并统一通过 COS_URL 访问腾讯云对象存储");
        }

        log.info("生产资源校验通过：数据库已强制走内网 IP，COS 统一使用 COS_URL");
    }

    private String requireValue(String key, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("生产环境缺少必要配置: " + key);
        }
        return value.trim();
    }
}
