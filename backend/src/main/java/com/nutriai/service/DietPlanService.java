package com.nutriai.service;

import com.nutriai.config.AIConfig;
import com.nutriai.dto.DietPlanRequest;
import com.nutriai.dto.DietPlanResponse;
import com.nutriai.entity.FoodNutrition;
import com.nutriai.repository.FoodNutritionRepository;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 饮食计划生成服务 — 使用流式AI调用避免超时
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DietPlanService {
    
    private final AIConfig aiConfig;
    private final FoodNutritionRepository foodNutritionRepository;
    
    @Value("${ai.api-key:}")
    private String apiKey;
    
    @Value("${ai.model-name:qwen-turbo}")
    private String modelName;
    
    @Value("${ai.timeout:300}")
    private Integer timeout;
    
    /**
     * 生成饮食计划（支持取消）
     */
    public DietPlanResponse generateDietPlanWithCancellation(Long userId, DietPlanRequest request, 
                                                             String taskId, DietPlanTaskService taskService) {
        return generateDietPlanInternal(userId, request, taskId, taskService);
    }
    
    /**
     * 生成饮食计划
     */
    public DietPlanResponse generateDietPlan(Long userId, DietPlanRequest request) {
        return generateDietPlanInternal(userId, request, null, null);
    }
    
    /**
     * 内部生成方法
     */
    private DietPlanResponse generateDietPlanInternal(Long userId, DietPlanRequest request, 
                                                      String taskId, DietPlanTaskService taskService) {
        log.info("开始为用户 {} 生成{}天饮食计划", userId, request.getDays());
        
        try {
            // 1. 计算每日热量需求
            log.info("步骤1: 计算每日热量需求...");
            int dailyCalories = calculateDailyCalories(request);
            log.info("✓ 每日热量需求: {} kcal", dailyCalories);
            
            // 2. 从数据库获取食物参考（只取前15种，用于提示AI）
            log.info("步骤2: 加载食物参考数据...");
            List<FoodNutrition> sampleFoods = foodNutritionRepository.findAll()
                    .stream().limit(15).collect(Collectors.toList());
            log.info("✓ 加载了 {} 种食物参考", sampleFoods.size());
            
            if (sampleFoods.isEmpty()) {
                log.error("❌ 数据库中没有食物数据！");
                throw new RuntimeException("数据库中没有食物数据");
            }
            
            // 3. 调用AI生成计划
            log.info("步骤3: 调用AI生成饮食计划...");
            String aiResponse = generateDietPlanInBatches(request, dailyCalories, sampleFoods, taskId, taskService);
            log.info("✓ AI响应成功，总长度: {} 字符", aiResponse.length());
            
            // 4. 生成采购清单（如果需要）
            String fullMarkdownContent = aiResponse;
            if (request.getIncludeShoppingList() != null && request.getIncludeShoppingList()) {
                log.info("步骤5: 生成采购清单...");
                String shoppingListPrompt = buildShoppingListPrompt(aiResponse, request.getDays());
                String shoppingListContent = callStreamingAI(shoppingListPrompt, taskId, taskService);
                log.info("✓ 采购清单生成成功");
                fullMarkdownContent = aiResponse + "\n\n---\n\n" + shoppingListContent;
            } else {
                log.info("步骤5: 跳过采购清单生成（用户未选择）");
            }
            
            // 6. 构建响应
            
            DietPlanResponse response = DietPlanResponse.builder()
                    .planId("plan_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                    .title(generatePlanTitle(request))
                    .days(request.getDays())
                    .goalDescription(getGoalDescription(request.getGoal()))
                    .markdownContent(fullMarkdownContent)
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            log.info("✅ 饮食计划生成成功");
            return response;
            
        } catch (Exception e) {
            log.error("❌ 生成饮食计划失败", e);
            throw new RuntimeException("生成饮食计划失败: " + e.getMessage());
        }
    }
    
    /**
     * 计算每日热量需求
     * 使用Harris-Benedict公式
     */
    private int calculateDailyCalories(DietPlanRequest request) {
        // 如果用户指定了热量，直接使用
        if (request.getDailyCalories() != null && request.getDailyCalories() > 0) {
            return request.getDailyCalories();
        }
        
        // 否则根据身高体重年龄性别计算基础代谢率（BMR）
        double bmr = 0;
        
        if (request.getHeight() != null && request.getWeight() != null && 
            request.getAge() != null && request.getGender() != null) {
            
            if ("male".equalsIgnoreCase(request.getGender())) {
                // 男性: BMR = 66 + (13.7 × 体重kg) + (5 × 身高cm) - (6.8 × 年龄)
                bmr = 66 + (13.7 * request.getWeight()) + (5 * request.getHeight()) - (6.8 * request.getAge());
            } else {
                // 女性: BMR = 655 + (9.6 × 体重kg) + (1.8 × 身高cm) - (4.7 × 年龄)
                bmr = 655 + (9.6 * request.getWeight()) + (1.8 * request.getHeight()) - (4.7 * request.getAge());
            }
            
            // 根据运动强度计算总消耗（TDEE）
            double activityMultiplier = getActivityMultiplier(request.getExerciseLevel());
            double tdee = bmr * activityMultiplier;
            
            // 根据目标调整热量
            if ("lose_weight".equals(request.getGoal())) {
                return (int) (tdee * 0.8); // 减少20%
            } else if ("gain_muscle".equals(request.getGoal())) {
                return (int) (tdee * 1.15); // 增加15%
            } else {
                return (int) tdee;
            }
        }
        
        // 默认返回2000卡路里
        return 2000;
    }
    
    /**
     * 获取运动强度系数
     */
    private double getActivityMultiplier(String exerciseLevel) {
        if (exerciseLevel == null) return 1.375; // 默认轻度运动
        
        return switch (exerciseLevel.toLowerCase()) {
            case "low" -> 1.375;    // 轻度运动（每周1-3天）
            case "medium" -> 1.55;  // 中度运动（每周3-5天）
            case "high" -> 1.725;   // 高强度运动（每周6-7天）
            default -> 1.375;
        };
    }
    
    /**
     * 构建采购清单生成Prompt
     */
    private String buildShoppingListPrompt(String dietPlanContent, int days) {
        return String.format("""
                根据以下%d天的饮食计划，生成一份完整的采购清单。
                
                饮食计划内容：
                %s
                
                请按照以下格式输出Markdown采购清单：
                
                # 🛒 采购清单（%d天）
                
                ## 🥬 蔬菜类
                - 食材名：数量 + 单位
                
                ## 🍎 水果类
                - 食材名：数量 + 单位
                
                ## 🍖 肉类/蛋白质
                - 食材名：数量 + 单位
                
                ## 🍚 主食类
                - 食材名：数量 + 单位
                
                ## 🥛 其他
                - 食材名：数量 + 单位
                
                ## 💡 采购建议
                - 给出一些实用的采购tips
                
                注意：
                1. 合并相同食材的数量
                2. 考虑食材的保鲜期
                3. 单位使用常见的购买单位（斤、个、包等）
                """, days, dietPlanContent, days);
    }
    
    /**
     * 生成计划标题
     */
    private String generatePlanTitle(DietPlanRequest request) {
        String goalText = switch (request.getGoal()) {
            case "lose_weight" -> "减脂";
            case "gain_muscle" -> "增肌";
            default -> "健康";
        };
        
        return String.format("%s计划 - %d天", goalText, request.getDays());
    }
    
    /**
     * 获取目标描述
     */
    private String getGoalDescription(String goal) {
        return switch (goal) {
            case "lose_weight" -> "减脂塑形";
            case "gain_muscle" -> "增肌强体";
            case "maintain" -> "维持健康";
            default -> "均衡饮食";
        };
    }
    
    /**
     * 分批生成饮食计划（短计划一次生成，长计划分批）
     */
    private String generateDietPlanInBatches(DietPlanRequest request, int dailyCalories, 
                                            List<FoodNutrition> sampleFoods, String taskId, 
                                            DietPlanTaskService taskService) {
        int totalDays = request.getDays();
        StringBuilder fullPlan = new StringBuilder();
        
        // 标题和概述
        fullPlan.append("# ").append(generatePlanTitle(request)).append("\n\n");
        fullPlan.append("## 计划概述\n");
        fullPlan.append(getGoalDescription(request.getGoal())).append("计划，每日目标 ")
                .append(dailyCalories).append(" kcal\n\n");
        
        LocalDate startDate = LocalDate.now();
        
        // 批次策略：1-3天一批，4+天每3天一批
        int batchSize = totalDays <= 3 ? totalDays : 3;
        
        int day = 1;
        while (day <= totalDays) {
            if (taskId != null && taskService != null && taskService.isTaskCancelled(taskId)) {
                throw new RuntimeException("任务已取消");
            }
            
            int batchEnd = Math.min(day + batchSize - 1, totalDays);
            int batchDays = batchEnd - day + 1;
            log.info("正在生成第{}-{}天的计划（共{}天）...", day, batchEnd, totalDays);
            
            if (taskId != null && taskService != null) {
                int progress = (int) ((day - 1) * 100.0 / totalDays);
                taskService.updateTaskStatus(taskId, "running", progress, day, null, null);
            }
            
            // 构建精简prompt
            String batchPrompt = buildCompactPrompt(request, dailyCalories, sampleFoods, day, batchEnd, startDate);
            
            // 根据批次天数动态设置maxTokens：每天约500-700 tokens
            int maxTokens = Math.min(batchDays * 800 + 200, 3000);
            
            String batchResponse = generateWithRetry(batchPrompt, maxTokens, day, 2, taskId, taskService);
            log.info("✓ 第{}-{}天生成成功，长度: {} 字符", day, batchEnd, batchResponse.length());
            
            fullPlan.append(batchResponse).append("\n\n");
            
            if (taskId != null && taskService != null) {
                int progress = (int) (batchEnd * 100.0 / totalDays);
                taskService.updateTaskStatus(taskId, "running", progress, batchEnd, null, null);
            }
            
            day = batchEnd + 1;
        }
        
        fullPlan.append("## 饮食建议\n");
        fullPlan.append("1. 保持规律用餐时间\n");
        fullPlan.append("2. 每天至少8杯水\n");
        fullPlan.append("3. 控制油盐糖摄入\n");
        fullPlan.append("4. 适量运动配合饮食\n");
        fullPlan.append("5. 保证充足睡眠\n");
        
        return fullPlan.toString();
    }
    
    /**
     * 使用流式API生成（支持取消和重试，动态maxTokens）
     */
    private String generateWithRetry(String prompt, int maxTokens, int day, int maxRetries, 
                                     String taskId, DietPlanTaskService taskService) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            if (taskId != null && taskService != null && taskService.isTaskCancelled(taskId)) {
                throw new RuntimeException("任务已取消");
            }
            
            try {
                if (attempt > 1) {
                    log.warn("第{}天重试（第{}/{}次）...", day, attempt, maxRetries);
                    Thread.sleep(2000);
                }
                
                long startTime = System.currentTimeMillis();
                String response = callStreamingAI(prompt, maxTokens, taskId, taskService);
                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                log.info("AI流式调用完成，第{}天，耗时{}秒，maxTokens={}", day, elapsed, maxTokens);
                
                if (response == null || response.trim().isEmpty()) {
                    throw new RuntimeException("AI返回空响应");
                }
                return response;
                
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("任务已取消")) {
                    throw new RuntimeException("任务已取消");
                }
                lastException = e;
                log.error("第{}天生成失败（第{}/{}次）: {}", day, attempt, maxRetries, e.getMessage());
            }
        }
        
        log.error("第{}天所有重试失败，使用简化版本", day);
        return generateSimplifiedDay(day, LocalDate.now().plusDays(day - 1));
    }
    
    /**
     * 流式AI调用 — token逐个到达，不会socket超时
     */
    private String callStreamingAI(String prompt, int maxTokens, String taskId, DietPlanTaskService taskService) {
        CompletableFuture<String> future = new CompletableFuture<>();
        StringBuilder sb = new StringBuilder();
        
        StreamingChatLanguageModel model = aiConfig.getStreamingChatModel(null, 0.7, maxTokens);
        
        model.generate(prompt, new StreamingResponseHandler<AiMessage>() {
            @Override
            public void onNext(String token) {
                sb.append(token);
            }
            
            @Override
            public void onComplete(Response<AiMessage> response) {
                future.complete(sb.toString());
            }
            
            @Override
            public void onError(Throwable error) {
                future.completeExceptionally(error);
            }
        });
        
        try {
            return future.get(300, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("AI流式调用失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 流式AI调用（使用默认maxTokens，用于采购清单等）
     */
    private String callStreamingAI(String prompt, String taskId, DietPlanTaskService taskService) {
        return callStreamingAI(prompt, 2000, taskId, taskService);
    }
    
    /**
     * 生成简化版的单天计划（作为降级方案）
     */
    private String generateSimplifiedDay(int day, LocalDate date) {
        StringBuilder plan = new StringBuilder();
        plan.append("## 第").append(day).append("天 - ")
            .append(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append("\n\n");
        plan.append("### 早餐 (7:00-8:00)\n");
        plan.append("- **燕麦粥**：50g\n");
        plan.append("- **鸡蛋**：1个\n");
        plan.append("- **牛奶**：250ml\n\n");
        plan.append("### 午餐 (12:00-13:00)\n");
        plan.append("- **米饭**：150g\n");
        plan.append("- **鸡胸肉**：100g\n");
        plan.append("- **西兰花**：100g\n\n");
        plan.append("### 晚餐 (18:00-19:00)\n");
        plan.append("- **糙米饭**：100g\n");
        plan.append("- **鱼肉**：120g\n");
        plan.append("- **青菜**：150g\n\n");
        plan.append("### 加餐 (15:00)\n");
        plan.append("- **苹果**：1个\n");
        plan.append("- **坚果**：30g\n\n");
        return plan.toString();
    }
    
    /**
     * 构建精简prompt — 减少输入token提升速度
     */
    private String buildCompactPrompt(DietPlanRequest request, int dailyCalories,
                                       List<FoodNutrition> sampleFoods, int dayStart, int dayEnd, LocalDate startDate) {
        StringBuilder p = new StringBuilder();
        int days = dayEnd - dayStart + 1;
        
        // 系统指令：简洁直接
        p.append("直接输出Markdown格式饮食计划，不要多余解释。\n\n");
        
        // 需求（一行式）
        p.append("需求：").append(dailyCalories).append("kcal/天，")
         .append(getGoalDescription(request.getGoal()));
        if (request.getPreferences() != null && !request.getPreferences().isEmpty()) {
            p.append("，偏好：").append(request.getPreferences());
        }
        if (request.getAllergies() != null && !request.getAllergies().isEmpty()) {
            p.append("，忌口：").append(request.getAllergies());
        }
        p.append("\n\n");
        
        // 食材参考（紧凑）
        p.append("食材参考：");
        for (int i = 0; i < Math.min(sampleFoods.size(), 10); i++) {
            if (i > 0) p.append("、");
            p.append(sampleFoods.get(i).getFoodName());
        }
        p.append("等\n\n");
        
        // 格式要求
        p.append("请生成第").append(dayStart).append("-").append(dayEnd).append("天计划，每天含早餐、午餐、晚餐、加餐，每餐3种食材+数量");
        if (request.getIncludeNutrition() != null && request.getIncludeNutrition()) {
            p.append("+热量");
        }
        if (request.getIncludeCookingTips() != null && request.getIncludeCookingTips()) {
            p.append("+烹饪提示");
        }
        p.append("。食材每天要有变化。\n\n");
        
        // 格式模板
        for (int d = dayStart; d <= dayEnd; d++) {
            LocalDate date = startDate.plusDays(d - 1);
            p.append("## 第").append(d).append("天 - ")
             .append(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append("\n");
        }
        
        return p.toString();
    }
    
    /**
     * 获取运动强度描述
     */
    private String getExerciseLevelDescription(String level) {
        if (level == null) return "轻度运动";
        
        return switch (level.toLowerCase()) {
            case "low" -> "轻度运动";
            case "medium" -> "中度运动";
            case "high" -> "高强度运动";
            default -> "轻度运动";
        };
    }
    
    /**
     * 根据用户修改建议修改饮食计划
     */
    public DietPlanResponse modifyDietPlan(Long userId, DietPlanResponse originalPlan, 
                                           String suggestion, String taskId, 
                                           DietPlanTaskService taskService) {
        log.info("开始修改饮食计划: userId={}, planId={}, suggestion={}", userId, originalPlan.getPlanId(), suggestion);
        
        try {
            // 构建修改Prompt
            String prompt = buildModifyPrompt(originalPlan, suggestion);
            
            if (taskId != null && taskService != null) {
                taskService.updateTaskStatus(taskId, "running", 30, 0, null, null);
            }
            
            // 调用AI生成修改后的计划（流式）
            String modifiedContent = callStreamingAI(prompt, taskId, taskService);
            
            if (taskId != null && taskService != null) {
                if (taskService.isTaskCancelled(taskId)) {
                    throw new RuntimeException("任务已取消");
                }
                taskService.updateTaskStatus(taskId, "running", 80, 0, null, null);
            }
            
            if (modifiedContent == null || modifiedContent.trim().isEmpty()) {
                throw new RuntimeException("AI返回空响应");
            }
            
            DietPlanResponse response = DietPlanResponse.builder()
                    .planId("plan_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                    .title(originalPlan.getTitle() + "（修改版）")
                    .days(originalPlan.getDays())
                    .goalDescription(originalPlan.getGoalDescription())
                    .markdownContent(modifiedContent)
                    .isFavorite(false)
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            log.info("饮食计划修改成功: newPlanId={}", response.getPlanId());
            return response;
            
        } catch (Exception e) {
            log.error("修改饮食计划失败", e);
            throw new RuntimeException("修改饮食计划失败: " + e.getMessage());
        }
    }
    
    /**
     * 构建修改建议Prompt
     */
    private String buildModifyPrompt(DietPlanResponse originalPlan, String suggestion) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("# 任务：根据用户反馈修改饮食计划\n\n");
        prompt.append("## 原始饮食计划\n");
        prompt.append(originalPlan.getMarkdownContent()).append("\n\n");
        prompt.append("## 用户的修改建议\n");
        prompt.append(suggestion).append("\n\n");
        prompt.append("## 要求\n");
        prompt.append("1. 根据用户的修改建议对原始饮食计划进行调整\n");
        prompt.append("2. 保持原有的Markdown格式不变\n");
        prompt.append("3. 只修改用户提到的相关部分，其余保持不变\n");
        prompt.append("4. 确保修改后的营养数据和热量仍然合理\n");
        prompt.append("5. 如果用户要求替换食材，确保替换后的食材营养价值相当\n");
        prompt.append("6. 在计划开头简要说明本次修改的内容\n\n");
        prompt.append("请输出完整的修改后的饮食计划（Markdown格式）：\n");
        return prompt.toString();
    }
    
}
