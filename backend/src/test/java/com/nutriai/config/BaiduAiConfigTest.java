package com.nutriai.config;

import com.baidu.aip.imageclassify.AipImageClassify;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class BaiduAiConfigTest {

    private final ApplicationContextRunner contextRunner =
        new ApplicationContextRunner().withUserConfiguration(BaiduAiConfig.class);

    @Test
    void shouldNotCreateAipImageClassifyBeanWhenCredentialsMissing() {
        contextRunner.run(context ->
            assertThat(context).doesNotHaveBean(AipImageClassify.class)
        );
    }

    @Test
    void shouldCreateAipImageClassifyBeanWhenAllCredentialsProvided() {
        contextRunner
            .withPropertyValues(
                "baidu.ai.app-id=test-app-id",
                "baidu.ai.api-key=test-api-key",
                "baidu.ai.secret-key=test-secret-key"
            )
            .run(context ->
                assertThat(context).hasSingleBean(AipImageClassify.class)
            );
    }
}
