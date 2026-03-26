package com.nutriai.ai;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * 营养知识库管理器
 * 加载并管理营养学知识库数据，用于注入AI对话上下文
 * 
 * 知识库数据来源：
 * 1. 中国食物成分表（第6版）- 中国疾控中心营养与健康所
 * 2. 中国居民膳食指南（2022版）- 中国营养学会 http://dg.cnsoc.org/
 * 3. 国家食品安全风险评估中心 https://www.cfsa.net.cn/
 * 4. 中国知网(CNKI)营养学文献 https://www.cnki.net/
 * 5. USDA食品数据库（补充参考） https://fdc.nal.usda.gov/
 */
@Slf4j
@Component
public class NutritionKnowledgeBase {
    
    @Getter
    private String knowledgeContent;
    
    @Getter
    private String systemPromptWithKnowledge;
    
    @PostConstruct
    public void init() {
        loadKnowledgeBase();
        buildSystemPrompt();
    }
    
    private void loadKnowledgeBase() {
        try {
            ClassPathResource resource = new ClassPathResource("knowledge/nutrition-knowledge-base.md");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                knowledgeContent = reader.lines().collect(Collectors.joining("\n"));
                log.info("✅ 营养知识库加载成功，内容长度: {} 字符", knowledgeContent.length());
            }
        } catch (Exception e) {
            log.warn("⚠️ 营养知识库加载失败，使用内置精简知识: {}", e.getMessage());
            knowledgeContent = getBuiltinKnowledge();
        }
    }
    
    private void buildSystemPrompt() {
        systemPromptWithKnowledge = """
            你是NutriAI，一位具有专业营养学知识的AI健康饮食规划助手。
            
            ## 你的核心能力
            1. 基于用户健康数据（身高、体重、BMI、年龄、性别等）提供**个性化**饮食建议
            2. 精准分析食物营养成分，给出科学健康评价
            3. 制定科学合理的饮食计划（日计划、周计划）
            4. 回答关于营养、健康饮食、食疗养生的专业问题
            5. 提供中式食谱推荐和烹饪建议
            
            ## 你的专业原则
            - **循证为本**：所有建议必须基于中国居民膳食指南（2022版）和中国食物成分表
            - **个性化**：根据用户的BMI、健康目标、年龄、性别定制方案
            - **安全第一**：不提供医疗诊断和药物建议，涉及疾病时建议就医
            - **中国特色**：优先推荐国人常见食材和中式烹饪方式
            - **数据可信**：营养数据引用权威来源，不编造数据
            
            ## 营养知识库（核心参考数据）
            
            """ + knowledgeContent + """
            
            ## 回答格式规范
            - 使用Markdown格式，清晰分段
            - 营养数据使用表格展示
            - 重点信息使用**加粗**
            - 给出建议时需说明科学依据
            - 如果用户情况需要就医，务必提醒
            - 涉及营养数据时，标注"每100g可食部分"
            """;
        
        log.info("✅ AI系统提示词构建完成（含知识库），总长度: {} 字符", systemPromptWithKnowledge.length());
    }
    
    /**
     * 获取针对特定用户的增强系统提示词
     */
    public String getEnhancedSystemPrompt(String userProfile) {
        StringBuilder prompt = new StringBuilder(systemPromptWithKnowledge);
        if (userProfile != null && !userProfile.isBlank()) {
            prompt.append("\n\n## 当前用户信息\n").append(userProfile);
        }
        return prompt.toString();
    }
    
    /**
     * 内置精简知识库（当文件加载失败时的备选）
     */
    private String getBuiltinKnowledge() {
        return """
            ### 中国居民膳食营养素参考摄入量
            - 成年男性：能量2250kcal，蛋白质65g
            - 成年女性：能量1800kcal，蛋白质55g
            - 膳食纤维：25-30g/天
            - 钙：800mg/天
            - 铁：男12mg/天，女20mg/天
            
            ### BMI标准（中国）
            - 偏瘦：<18.5
            - 正常：18.5-23.9
            - 超重：24.0-27.9
            - 肥胖：≥28.0
            
            ### 膳食宝塔
            - 谷薯类：250-400g
            - 蔬菜：300-500g，水果：200-350g
            - 肉禽：40-75g，水产：40-75g，蛋类：40-50g
            - 奶制品：300-500g
            - 盐<5g，油25-30g
            - 饮水：1500-1700ml
            """;
    }
}
