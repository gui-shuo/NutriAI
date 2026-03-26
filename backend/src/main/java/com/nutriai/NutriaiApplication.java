package com.nutriai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI健康饮食规划助手 - 启动类
 * 
 * @author NutriAI Team
 * @since 2025-12-02
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class NutriaiApplication {

    public static void main(String[] args) {
        SpringApplication.run(NutriaiApplication.class, args);
        System.out.println("""
            
            ╔═══════════════════════════════════════════════════════╗
            ║                                                       ║
            ║   AI健康饮食规划助手 - 后端服务启动成功！              ║
            ║   NutriAI Backend Service Started Successfully!      ║
            ║                                                       ║
            ║   API文档: http://localhost:8080/api/doc            ║
            ║   健康检查: http://localhost:8080/api/health         ║
            ║                                                       ║
            ╚═══════════════════════════════════════════════════════╝
            
            """);
    }
}
