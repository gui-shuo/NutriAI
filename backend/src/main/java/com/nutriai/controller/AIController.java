package com.nutriai.controller;

import com.nutriai.common.ApiResponse;
import com.nutriai.entity.User;
import com.nutriai.repository.UserRepository;
import com.nutriai.service.AIService;
import com.nutriai.service.MemberPermissionService;
import com.nutriai.service.OssService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * AI控制器
 * 处理AI相关的HTTP请求
 * 
 * @author NutriAI Team
 * @date 2025-12-03
 */
@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AIController {
    
    private final AIService aiService;
    private final UserRepository userRepository;
    private final OssService ossService;
    private final MemberPermissionService memberPermissionService;
    
    /**
     * 初始化AI对话
     * POST /api/ai/init
     */
    @PostMapping("/init")
    public ApiResponse<String> initChat(HttpServletRequest request,
                                   @RequestBody Map<String, Object> params) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            log.info("用户 {} 初始化AI对话", userId);
            
            // 获取用户信息
            User user = userRepository.findById(userId)
                    .orElse(null);
            if (user == null) {
                return ApiResponse.error("用户不存在");
            }
            
            // 获取健康数据
            Double height = params.get("height") != null ? 
                           Double.valueOf(params.get("height").toString()) : null;
            Double weight = params.get("weight") != null ? 
                           Double.valueOf(params.get("weight").toString()) : null;
            Double targetWeight = params.get("targetWeight") != null ? 
                                 Double.valueOf(params.get("targetWeight").toString()) : null;
            String healthGoal = params.get("healthGoal") != null ? 
                               params.get("healthGoal").toString() : null;
            
            // 初始化上下文
            aiService.initializeUserContext(userId, user, height, weight, targetWeight, healthGoal);
            
            return ApiResponse.success("AI对话初始化成功");
            
        } catch (Exception e) {
            log.error("初始化AI对话失败", e);
            return ApiResponse.error("初始化失败，请稍后重试");
        }
    }
    
    /**
     * 发送消息
     * POST /api/ai/chat
     */
    @PostMapping("/chat")
    public ApiResponse<Map<String, Object>> chat(HttpServletRequest request,
                                            @RequestBody ChatRequest chatRequest) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            String message = chatRequest.getMessage();
            
            if (message == null || message.trim().isEmpty()) {
                return ApiResponse.error("消息不能为空");
            }
            
            log.info("📨 用户 {} 发送消息: {}, 模型: {}, 温度: {}, 最大字数: {}, 保持上下文: {}", 
                userId, 
                message.substring(0, Math.min(message.length(), 50)),
                chatRequest.getModel(),
                chatRequest.getTemperature(),
                chatRequest.getMaxTokens(),
                chatRequest.getKeepContext());
            
            // 检查AI配额
            if (!memberPermissionService.checkAiQuota(userId)) {
                int quota = memberPermissionService.getDailyQuota(userId);
                return ApiResponse.error("今日AI咨询次数已达上限（" + quota + "次/天），升级会员可享受更多次数");
            }

            // 调用AI服务（传递用户设置的参数）
            String response = aiService.chat(
                userId, 
                message, 
                chatRequest.getModel(), 
                chatRequest.getTemperature(), 
                chatRequest.getMaxTokens(), 
                chatRequest.getKeepContext()
            );
            
            // 消耗配额
            memberPermissionService.consumeAiQuota(userId);

            // 返回响应
            Map<String, Object> result = Map.of(
                "message", response,
                "timestamp", System.currentTimeMillis(),
                "settings", Map.of(
                    "model", chatRequest.getModel() != null ? chatRequest.getModel() : "qwen-max",
                    "temperature", chatRequest.getTemperature() != null ? chatRequest.getTemperature() : 0.7,
                    "maxTokens", chatRequest.getMaxTokens() != null ? chatRequest.getMaxTokens() : 2000
                )
            );
            
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("AI对话失败", e);
            return ApiResponse.error("对话失败，请稍后重试");
        }
    }
    
    /**
     * 聊天请求DTO
     */
    @Data
    public static class ChatRequest {
        private String message;
        private String model;
        private Double temperature;
        private Integer maxTokens;
        private Boolean keepContext;
    }
    
    /**
     * 上传聊天附件
     * POST /api/ai/upload
     */
    @PostMapping("/upload")
    public ApiResponse<String> uploadFile(HttpServletRequest request,
                                          @RequestParam("file") MultipartFile file) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            
            if (file == null || file.isEmpty()) {
                return ApiResponse.error("文件不能为空");
            }
            if (file.getSize() > 10 * 1024 * 1024) {
                return ApiResponse.error("文件大小不能超过10MB");
            }
            
            log.info("用户 {} 上传聊天附件: {}, 大小: {}", userId, file.getOriginalFilename(), file.getSize());
            
            String fileUrl = ossService.uploadFoodPhoto(file);
            
            return ApiResponse.success(fileUrl);
            
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return ApiResponse.error("文件上传失败，请稍后重试");
        }
    }
    
    /**
     * 分析食物营养
     * POST /api/ai/analyze-food
     */
    @PostMapping("/analyze-food")
    public ApiResponse<String> analyzeFood(HttpServletRequest request,
                                     @RequestBody Map<String, String> params) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            String foodName = params.get("foodName");
            String mealType = params.get("mealType");
            String portion = params.get("portion");
            
            if (foodName == null || foodName.trim().isEmpty()) {
                return ApiResponse.error("食物名称不能为空");
            }
            
            String analysis = aiService.analyzeFoodNutrition(userId, foodName, mealType, portion);
            
            return ApiResponse.success(analysis);
            
        } catch (Exception e) {
            log.error("食物分析失败", e);
            return ApiResponse.error("分析失败，请稍后重试");
        }
    }
    
    /**
     * 生成饮食计划
     * POST /api/ai/generate-plan
     */
    @PostMapping("/generate-plan")
    public ApiResponse<String> generatePlan(HttpServletRequest request,
                                      @RequestBody Map<String, Object> params) {
        try {
            Long userId = (Long) request.getAttribute("userId");

            // 饮食计划需要白银及以上会员
            String tier = memberPermissionService.getEffectiveTier(userId);
            if ("FREE".equals(tier)) {
                return ApiResponse.error("饮食计划功能需要白银及以上会员等级，请升级会员或开通VIP");
            }

            // 检查AI配额
            if (!memberPermissionService.checkAiQuota(userId)) {
                int quota = memberPermissionService.getDailyQuota(userId);
                return ApiResponse.error("今日AI咨询次数已达上限（" + quota + "次/天），升级会员可享受更多次数");
            }

            int days = params.get("days") != null ? 
                      Integer.parseInt(params.get("days").toString()) : 7;
            
            if (days < 1 || days > 30) {
                return ApiResponse.error("计划天数必须在1-30之间");
            }
            
            String plan = aiService.generateDietPlan(userId, days);

            // 消耗配额
            memberPermissionService.consumeAiQuota(userId);
            
            return ApiResponse.success(plan);
            
        } catch (Exception e) {
            log.error("饮食计划生成失败", e);
            return ApiResponse.error("生成失败，请稍后重试");
        }
    }
    
    /**
     * 清除对话历史
     * DELETE /api/ai/clear
     */
    @DeleteMapping("/clear")
    public ApiResponse<String> clearHistory(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            aiService.clearUserContext(userId);
            return ApiResponse.success("对话历史已清除");
        } catch (Exception e) {
            log.error("清除对话历史失败", e);
            return ApiResponse.error("清除失败，请稍后重试");
        }
    }
    
    /**
     * 获取活跃对话数（管理员）
     * GET /api/ai/stats
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        try {
            int activeCount = aiService.getActiveConversationCount();
            Map<String, Object> stats = Map.of(
                "activeConversations", activeCount
            );
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("获取统计数据失败", e);
            return ApiResponse.error("获取失败，请稍后重试");
        }
    }
}
