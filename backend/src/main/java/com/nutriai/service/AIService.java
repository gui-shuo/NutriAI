package com.nutriai.service;

import com.nutriai.ai.AIToolkit;
import com.nutriai.ai.ConversationContextManager;
import com.nutriai.ai.PromptTemplateManager;
import com.nutriai.entity.User;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI服务类
 * 整合所有AI功能，提供统一的对外接口
 * 
 * @author NutriAI Team
 * @date 2025-12-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {
    
    private final ChatLanguageModel chatModel;
    private final StreamingChatLanguageModel streamingChatModel;
    private final PromptTemplateManager promptManager;
    private final ConversationContextManager contextManager;
    private final AIToolkit toolkit;
    
    /**
     * 初始化用户的AI对话上下文
     * 
     * @param userId 用户ID
     * @param user 用户信息
     * @param height 身高
     * @param weight 体重
     * @param targetWeight 目标体重
     * @param healthGoal 健康目标
     */
    public void initializeUserContext(Long userId, User user, Double height, Double weight,
                                     Double targetWeight, String healthGoal) {
        log.info("初始化用户 {} 的AI对话上下文", userId);
        
        // 添加系统角色
        String systemRole = promptManager.getTemplate("SYSTEM_ROLE").apply(java.util.Collections.emptyMap()).text();
        contextManager.addSystemMessage(userId, systemRole);
        
        // 保存用户信息到上下文
        contextManager.setContextData(userId, "user", user);
        contextManager.setContextData(userId, "height", height);
        contextManager.setContextData(userId, "weight", weight);
        contextManager.setContextData(userId, "targetWeight", targetWeight);
        contextManager.setContextData(userId, "healthGoal", healthGoal);
        
        // 构建用户画像
        String userProfile = toolkit.buildUserProfile(user, height, weight, targetWeight, healthGoal);
        contextManager.setContextData(userId, "userProfile", userProfile);
        
        log.info("用户 {} 的上下文初始化完成", userId);
    }
    
    /**
     * 发送消息并获取AI响应（同步）- 使用默认配置
     * 
     * @param userId 用户ID
     * @param userMessage 用户消息
     * @return AI响应
     */
    public String chat(Long userId, String userMessage) {
        return chat(userId, userMessage, null, null, null, null);
    }
    
    /**
     * 发送消息并获取AI响应（同步）- 支持自定义参数
     * 
     * @param userId 用户ID
     * @param userMessage 用户消息
     * @param model AI模型名称
     * @param temperature 温度参数
     * @param maxTokens 最大token数
     * @param keepContext 是否保持上下文
     * @return AI响应
     */
    public String chat(Long userId, String userMessage, String model, Double temperature, Integer maxTokens, Boolean keepContext) {
        // 使用默认值
        String actualModel = model != null ? model : "qwen-max";
        Double actualTemperature = temperature != null ? temperature : 0.7;
        Integer actualMaxTokens = maxTokens != null ? maxTokens : 2000;
        Boolean actualKeepContext = keepContext != null ? keepContext : true;
        
        log.info("用户 {} 发送消息: {}, 模型: {}, 温度: {}, 最大Token: {}, 保持上下文: {}", 
            userId, 
            userMessage.substring(0, Math.min(userMessage.length(), 50)),
            actualModel,
            actualTemperature,
            actualMaxTokens,
            actualKeepContext);
        
        try {
            // 检查敏感内容
            if (toolkit.containsSensitiveContent(userMessage)) {
                log.warn("用户 {} 的消息包含敏感医疗内容", userId);
                return "您的问题涉及医疗诊断，建议咨询专业医生。" + toolkit.getMedicalDisclaimer();
            }
            
            // 添加用户消息到上下文
            contextManager.addUserMessage(userId, userMessage);
            
            // 获取消息历史（根据keepContext决定）
            List<ChatMessage> messageHistory;
            if (actualKeepContext) {
                messageHistory = contextManager.getMessageHistory(userId);
                log.debug("使用上下文记忆，历史消息数: {}", messageHistory.size());
            } else {
                // 不保持上下文，只发送当前消息
                messageHistory = contextManager.getMessageHistory(userId);
                if (messageHistory.size() > 2) {
                    // 只保留系统消息和当前用户消息
                    messageHistory = List.of(
                        messageHistory.get(0),  // 系统消息
                        messageHistory.get(messageHistory.size() - 1)  // 当前用户消息
                    );
                }
                log.debug("不使用上下文记忆，只发送当前消息");
            }
            
            // 调用AI模型（使用默认的chatModel，参数已在配置中设置）
            // 注意：temperature和maxTokens需要在创建模型时设置，这里暂时使用默认配置
            // TODO: Sprint 8 实现动态模型配置
            Response<dev.langchain4j.data.message.AiMessage> response = chatModel.generate(messageHistory);
            String aiResponse = response.content().text();
            
            // 添加AI响应到上下文
            contextManager.addAiMessage(userId, aiResponse);
            
            // 格式化响应
            String formattedResponse = toolkit.formatAIResponse(aiResponse);
            
            log.info("✅ 用户 {} 收到AI响应，长度: {} 字符", userId, formattedResponse.length());
            
            return formattedResponse;
            
        } catch (Exception e) {
            log.error("❌ AI对话出错，用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            return "抱歉，AI服务暂时不可用，请稍后重试。错误信息：" + e.getMessage();
        }
    }
    
    /**
     * 分析食物营养
     * 
     * @param userId 用户ID
     * @param foodName 食物名称
     * @param mealType 餐次
     * @param portion 分量
     * @return 营养分析结果
     */
    public String analyzeFoodNutrition(Long userId, String foodName, String mealType, String portion) {
        log.info("用户 {} 请求分析食物: {}", userId, foodName);
        
        try {
            // 构建变量
            Map<String, Object> variables = new HashMap<>();
            variables.put("foodName", foodName);
            variables.put("mealType", mealType != null ? mealType : "未指定");
            variables.put("portion", portion != null ? portion : "适量");
            
            // 应用模板
            String prompt = promptManager.apply("FOOD_ANALYSIS", variables);
            
            // 调用AI
            String response = chatModel.generate(prompt);
            
            return toolkit.formatAIResponse(response);
            
        } catch (Exception e) {
            log.error("食物分析出错", e);
            return "抱歉，食物分析服务暂时不可用。";
        }
    }
    
    /**
     * 生成饮食计划
     * 
     * @param userId 用户ID
     * @param days 计划天数
     * @return 饮食计划
     */
    public String generateDietPlan(Long userId, int days) {
        log.info("用户 {} 请求生成 {} 天饮食计划", userId, days);
        
        try {
            // 从上下文获取用户信息
            String userProfile = (String) contextManager.getContextData(userId, "userProfile");
            
            if (userProfile == null) {
                return "请先完善个人健康信息后再生成饮食计划。";
            }
            
            // 构建变量
            Map<String, Object> variables = new HashMap<>();
            variables.put("days", days);
            variables.put("userProfile", userProfile);
            
            // 应用模板
            String prompt = promptManager.apply("DIET_PLAN", variables);
            
            // 调用AI
            String response = chatModel.generate(prompt);
            
            return toolkit.formatAIResponse(response);
            
        } catch (Exception e) {
            log.error("饮食计划生成出错", e);
            return "抱歉，饮食计划生成服务暂时不可用。";
        }
    }
    
    /**
     * 推荐食谱
     * 
     * @param userId 用户ID
     * @param ingredients 可用食材
     * @param cookingTime 烹饪时间（分钟）
     * @param difficulty 难度（简单/中等/困难）
     * @return 食谱推荐
     */
    public String recommendRecipe(Long userId, String ingredients, int cookingTime, String difficulty) {
        log.info("用户 {} 请求食谱推荐", userId);
        
        try {
            // 构建变量
            Map<String, Object> variables = new HashMap<>();
            variables.put("ingredients", ingredients);
            variables.put("cookingTime", cookingTime);
            variables.put("difficulty", difficulty != null ? difficulty : "简单");
            variables.put("preferences", "");
            
            // 应用模板
            String prompt = promptManager.apply("RECIPE_RECOMMEND", variables);
            
            // 调用AI
            String response = chatModel.generate(prompt);
            
            return toolkit.formatAIResponse(response);
            
        } catch (Exception e) {
            log.error("食谱推荐出错", e);
            return "抱歉，食谱推荐服务暂时不可用。";
        }
    }
    
    /**
     * 提供健康建议
     * 
     * @param userId 用户ID
     * @param question 用户问题
     * @return 健康建议
     */
    public String provideHealthAdvice(Long userId, String question) {
        log.info("用户 {} 请求健康建议", userId);
        
        try {
            // 从上下文获取用户信息
            String userProfile = (String) contextManager.getContextData(userId, "userProfile");
            
            // 构建变量
            Map<String, Object> variables = new HashMap<>();
            variables.put("userProfile", userProfile != null ? userProfile : "用户信息未完善");
            variables.put("question", question);
            
            // 应用模板
            String prompt = promptManager.apply("HEALTH_ADVICE", variables);
            
            // 调用AI
            String response = chatModel.generate(prompt);
            
            // 添加免责声明
            return toolkit.formatAIResponse(response) + toolkit.getMedicalDisclaimer();
            
        } catch (Exception e) {
            log.error("健康建议提供出错", e);
            return "抱歉，健康建议服务暂时不可用。";
        }
    }
    
    /**
     * 获取用户的对话历史
     * 
     * @param userId 用户ID
     * @return 消息历史
     */
    public List<ChatMessage> getChatHistory(Long userId) {
        return contextManager.getMessageHistory(userId);
    }
    
    /**
     * 清除用户的对话上下文
     * 
     * @param userId 用户ID
     */
    public void clearUserContext(Long userId) {
        contextManager.clearContext(userId);
        log.info("已清除用户 {} 的对话上下文", userId);
    }
    
    /**
     * 获取活跃的对话数量
     * 
     * @return 活跃对话数
     */
    public int getActiveConversationCount() {
        return contextManager.getActiveContextCount();
    }
}
