package com.nutriai.ai;

import dev.langchain4j.model.input.PromptTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Prompt模板管理器
 * 管理所有AI对话的Prompt模板
 * 
 * @author NutriAI Team
 * @date 2025-12-03
 */
@Slf4j
@Component
public class PromptTemplateManager {
    
    private final Map<String, PromptTemplate> templates;
    
    public PromptTemplateManager() {
        this.templates = new HashMap<>();
        initializeTemplates();
    }
    
    /**
     * 初始化所有Prompt模板
     */
    private void initializeTemplates() {
        // 1. 系统角色定义
        templates.put("SYSTEM_ROLE", PromptTemplate.from(
            """
            你是NutriAI，一个专业的AI健康饮食规划助手。
            
            你的职责：
            1. 根据用户的健康数据（身高、体重、BMI、年龄等）提供个性化饮食建议
            2. 分析食物的营养成分，给出健康评价
            3. 制定科学的饮食计划（日计划、周计划、月计划）
            4. 回答关于营养、健康饮食的问题
            5. 提供食谱推荐和烹饪建议
            
            你的特点：
            - 专业：基于营养学知识，提供科学的建议
            - 个性化：根据用户的具体情况定制方案
            - 友好：用亲切、易懂的语言交流
            - 安全：不提供医疗建议，建议用户咨询医生
            
            回答格式：
            - 使用Markdown格式，清晰分段
            - 重点信息使用**加粗**
            - 列表使用 - 或编号
            - 营养数据使用表格展示
            """
        ));
        
        // 2. 用户画像模板
        templates.put("USER_PROFILE", PromptTemplate.from(
            """
            用户信息：
            - 昵称：{{nickname}}
            - 性别：{{gender}}
            - 年龄：{{age}}岁
            - 身高：{{height}}cm
            - 体重：{{weight}}kg
            - BMI：{{bmi}} ({{bmiCategory}})
            - 目标体重：{{targetWeight}}kg
            - 健康目标：{{healthGoal}}
            """
        ));
        
        // 3. 饮食分析模板
        templates.put("FOOD_ANALYSIS", PromptTemplate.from(
            """
            请分析以下食物的营养成分和健康评价：
            
            食物名称：{{foodName}}
            餐次：{{mealType}}
            分量：{{portion}}
            
            请提供：
            1. 主要营养成分（卡路里、蛋白质、脂肪、碳水化合物）
            2. 维生素和矿物质含量
            3. 健康评价（优点和注意事项）
            4. 搭配建议
            5. 适合的人群
            """
        ));
        
        // 4. 饮食计划生成模板
        templates.put("DIET_PLAN", PromptTemplate.from(
            """
            请为用户制定{{days}}天的饮食计划：
            
            用户信息：
            {{userProfile}}
            
            要求：
            1. 每日三餐（早餐、午餐、晚餐）
            2. 营养均衡，满足用户的健康目标
            3. 食材常见，易于获取
            4. 考虑季节性食材
            5. 每餐包含：主食、蛋白质、蔬菜、水果
            
            输出格式：
            ## 第X天
            
            ### 早餐 (7:00-8:00)
            - 食物1 (分量)
            - 食物2 (分量)
            **营养数据**: 卡路里XXXkcal，蛋白质XXg，碳水XXg，脂肪XXg
            
            ### 午餐 (12:00-13:00)
            ...
            
            ### 晚餐 (18:00-19:00)
            ...
            
            ### 每日营养总计
            ...
            """
        ));
        
        // 5. 食物识别模板
        templates.put("FOOD_RECOGNITION", PromptTemplate.from(
            """
            根据图片识别结果，这是：{{recognizedFood}}
            
            请提供：
            1. 食物的准确名称和类别
            2. 估算的营养成分
            3. 这个食物的健康评价
            4. 食用建议（适合的时间、分量、搭配）
            5. 替代食物推荐（如果需要更健康的选择）
            """
        ));
        
        // 6. 食谱推荐模板
        templates.put("RECIPE_RECOMMEND", PromptTemplate.from(
            """
            请根据以下食材推荐食谱：
            
            可用食材：{{ingredients}}
            烹饪时间：{{cookingTime}}分钟内
            烹饪难度：{{difficulty}}
            用户喜好：{{preferences}}
            
            请提供：
            1. 推荐的菜品名称
            2. 所需食材和分量
            3. 烹饪步骤（详细）
            4. 营养成分分析
            5. 烹饪技巧和注意事项
            """
        ));
        
        // 7. 健康建议模板
        templates.put("HEALTH_ADVICE", PromptTemplate.from(
            """
            用户健康数据：
            {{userProfile}}
            
            用户问题：{{question}}
            
            请提供：
            1. 针对性的健康建议
            2. 饮食调整方案
            3. 生活方式建议
            4. 注意事项
            5. 如需就医，明确提示
            
            注意：不提供诊断和药物建议，严重问题建议就医。
            """
        ));
        
        // 8. 营养知识问答模板
        templates.put("NUTRITION_QA", PromptTemplate.from(
            """
            用户问题：{{question}}
            
            请以专业但易懂的方式回答，包含：
            1. 问题的核心答案
            2. 相关的营养学知识
            3. 实用建议
            4. 常见误区澄清
            5. 推荐阅读或参考
            """
        ));
        
        // 9. 采购清单生成模板
        templates.put("SHOPPING_LIST", PromptTemplate.from(
            """
            根据以下饮食计划生成采购清单：
            
            {{dietPlan}}
            
            请按类别整理：
            1. 主食类
            2. 肉蛋类
            3. 蔬菜类
            4. 水果类
            5. 调味料和其他
            
            每项包含：名称、估算数量、保存方式
            """
        ));
        
        // 10. 运动建议模板
        templates.put("EXERCISE_ADVICE", PromptTemplate.from(
            """
            用户信息：
            {{userProfile}}
            
            健康目标：{{healthGoal}}
            当前饮食：{{currentDiet}}
            
            请提供配套的运动建议：
            1. 推荐的运动类型
            2. 运动强度和时长
            3. 运动时间安排
            4. 运动前后的饮食建议
            5. 注意事项和安全提示
            """
        ));
        
        log.info("已加载 {} 个Prompt模板", templates.size());
    }
    
    /**
     * 获取指定的Prompt模板
     * 
     * @param templateName 模板名称
     * @return Prompt模板
     */
    public PromptTemplate getTemplate(String templateName) {
        PromptTemplate template = templates.get(templateName);
        if (template == null) {
            log.warn("未找到Prompt模板: {}", templateName);
            throw new IllegalArgumentException("Prompt模板不存在: " + templateName);
        }
        return template;
    }
    
    /**
     * 应用模板并填充变量
     * 
     * @param templateName 模板名称
     * @param variables    变量Map
     * @return 填充后的Prompt文本
     */
    public String apply(String templateName, Map<String, Object> variables) {
        PromptTemplate template = getTemplate(templateName);
        return template.apply(variables).text();
    }
    
    /**
     * 检查模板是否存在
     * 
     * @param templateName 模板名称
     * @return 是否存在
     */
    public boolean hasTemplate(String templateName) {
        return templates.containsKey(templateName);
    }
    
    /**
     * 获取所有模板名称
     * 
     * @return 模板名称列表
     */
    public java.util.Set<String> getAllTemplateNames() {
        return templates.keySet();
    }
}
