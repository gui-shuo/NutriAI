package com.nutriai.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.nutriai.dto.DietPlanRequest;
import com.nutriai.dto.DietPlanResponse;
import com.nutriai.entity.FoodNutrition;
import com.nutriai.repository.FoodNutritionRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 饮食计划生成服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DietPlanService {
    
    private final ChatLanguageModel chatLanguageModel;
    private final FoodNutritionRepository foodNutritionRepository;
    
    @Value("${tongyi.api-key}")
    private String apiKey;
    
    @Value("${tongyi.model:qwen-max}")
    private String modelName;
    
    @Value("${tongyi.timeout:180}")
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
            log.info("✓ 计算得出每日热量需求: {} kcal", dailyCalories);
            
            // 2. 从数据库获取营养数据
            log.info("步骤2: 从数据库加载食物数据...");
            List<FoodNutrition> allFoods = foodNutritionRepository.findAll();
            log.info("✓ 从数据库加载了 {} 种食物数据", allFoods.size());
            
            if (allFoods.isEmpty()) {
                log.error("❌ 数据库中没有食物数据！");
                throw new RuntimeException("数据库中没有食物数据");
            }
            
            // 3. 构建Prompt
            log.info("步骤3: 构建AI Prompt...");
            String prompt = buildDietPlanPrompt(request, dailyCalories, allFoods);
            log.info("✓ Prompt构建完成，长度: {} 字符", prompt.length());
            
            // 4. 调用AI生成计划（分批生成策略）
            log.info("步骤4: 调用AI生成饮食计划（分批生成，每批2-3天）...");
            String aiResponse = generateDietPlanInBatches(request, dailyCalories, allFoods, taskId, taskService);
            log.info("✓ AI响应成功，总长度: {} 字符", aiResponse.length());
            
            // 5. 生成采购清单（如果需要）
            String fullMarkdownContent = aiResponse;
            if (request.getIncludeShoppingList() != null && request.getIncludeShoppingList()) {
                log.info("步骤5: 生成采购清单...");
                String shoppingListPrompt = buildShoppingListPrompt(aiResponse, request.getDays());
                String shoppingListContent = chatLanguageModel.generate(shoppingListPrompt);
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
     * 构建饮食计划生成Prompt
     */
    private String buildDietPlanPrompt(DietPlanRequest request, int dailyCalories, List<FoodNutrition> allFoods) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("# 任务：生成个性化饮食计划\n\n");
        
        prompt.append("## 用户信息\n");
        if (request.getHeight() != null) {
            prompt.append("- 身高：").append(request.getHeight()).append(" cm\n");
        }
        if (request.getWeight() != null) {
            prompt.append("- 体重：").append(request.getWeight()).append(" kg\n");
        }
        if (request.getAge() != null) {
            prompt.append("- 年龄：").append(request.getAge()).append(" 岁\n");
        }
        if (request.getGender() != null) {
            prompt.append("- 性别：").append("male".equals(request.getGender()) ? "男" : "女").append("\n");
        }
        prompt.append("- 目标：").append(getGoalDescription(request.getGoal())).append("\n");
        prompt.append("- 运动强度：").append(getExerciseLevelDescription(request.getExerciseLevel())).append("\n");
        prompt.append("- 每日热量目标：").append(dailyCalories).append(" kcal\n");
        
        if (request.getPreferences() != null && !request.getPreferences().isEmpty()) {
            prompt.append("- 饮食偏好：").append(request.getPreferences()).append("\n");
        }
        if (request.getAllergies() != null && !request.getAllergies().isEmpty()) {
            prompt.append("- 过敏/忌口：").append(request.getAllergies()).append("\n");
        }
        
        prompt.append("\n## 计划要求\n");
        prompt.append("- 计划天数：").append(request.getDays()).append(" 天\n");
        prompt.append("- 每天包含：早餐、午餐、晚餐、加餐（可选）\n");
        prompt.append("- 营养均衡：蛋白质、碳水化合物、脂肪比例合理\n");
        prompt.append("- 食材多样化：避免重复\n");
        prompt.append("- 简单易做：适合家庭制作\n\n");
        
        prompt.append("## 可用食材数据库（部分示例）\n");
        prompt.append("以下是营养数据库中的部分食材，生成计划时可以参考这些食材的营养成分：\n\n");
        
        // 随机选择50个食材作为示例
        List<FoodNutrition> sampleFoods = allFoods.stream()
                .limit(50)
                .collect(Collectors.toList());
        
        for (FoodNutrition food : sampleFoods) {
            prompt.append("- **").append(food.getFoodName()).append("**：");
            prompt.append(food.getEnergy()).append("kcal/100g，");
            prompt.append("蛋白质").append(food.getProtein()).append("g，");
            prompt.append("碳水").append(food.getCarbohydrate()).append("g，");
            prompt.append("脂肪").append(food.getFat()).append("g\n");
        }
        
        prompt.append("\n提示：你可以使用数据库中的任何食材，不限于以上示例。\n\n");
        
        prompt.append("## 输出格式要求（必须严格遵守）\n");
        prompt.append("请使用Markdown格式输出，**必须包含**以下所有内容：\n\n");
        prompt.append("### 1. 计划概述\n");
        prompt.append("简要说明这个饮食计划的特点、预期效果、适合人群\n\n");
        
        prompt.append("### 2. 营养目标\n");
        prompt.append("**必须包含具体数字**：\n");
        prompt.append("- 每日总热量：").append(dailyCalories).append(" kcal\n");
        prompt.append("- 蛋白质：XXXg (XX%)\n");
        prompt.append("- 碳水化合物：XXXg (XX%)\n");
        prompt.append("- 脂肪：XXXg (XX%)\n\n");
        
        prompt.append("### 3. 每日详细计划\n");
        prompt.append("**必须生成").append(request.getDays()).append("天完整计划，每天格式如下：**\n\n");
        
        prompt.append("## 第X天 - YYYY-MM-DD\n\n");
        
        prompt.append("### 早餐 (7:00-8:00)\n");
        prompt.append("- **食材1**：数量");
        if (request.getIncludeNutrition() != null && request.getIncludeNutrition()) {
            prompt.append(" - 营养数据");
        }
        prompt.append("\n- **食材2**：数量\n- **食材3**：数量\n");
        if (request.getIncludeNutrition() != null && request.getIncludeNutrition()) {
            prompt.append("- **小计**：XXX kcal\n");
        }
        if (request.getIncludeCookingTips() != null && request.getIncludeCookingTips()) {
            prompt.append("- **烹饪提示**：简要说明\n");
        }
        prompt.append("\n### 午餐、晚餐、加餐：同样格式\n");
        if (request.getIncludeNutrition() != null && request.getIncludeNutrition()) {
            prompt.append("### 每日营养总计\n- 总热量、蛋白质、碳水、脂肪\n");
        }
        prompt.append("\n");
        
        prompt.append("### 4. 饮食建议\n");
        prompt.append("给出5-8条实用的饮食建议\n\n");
        
        prompt.append("**重要提示**：\n");
        prompt.append("1. 必须使用数据库中的真实食材及其营养数据\n");
        prompt.append("2. 每个食材必须标注具体数量");
        
        if (request.getIncludeNutrition() != null && request.getIncludeNutrition()) {
            prompt.append("和完整营养成分\n");
            prompt.append("3. 每餐必须计算小计热量\n");
            prompt.append("4. 每天必须计算营养总计\n");
        } else {
            prompt.append("\n");
            prompt.append("3. 食材描述要清晰易懂\n");
        }
        
        if (request.getIncludeCookingTips() != null && request.getIncludeCookingTips()) {
            prompt.append("5. 烹饪提示要简单实用\n");
        }
        
        prompt.append("6. 所有").append(request.getDays()).append("天的计划都必须详细列出\n\n");
        
        LocalDate startDate = LocalDate.now();
        prompt.append("\n**关键要求**：\n");
        prompt.append("1. 必须生成").append(request.getDays()).append("天完整计划（从").append(startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append("开始）\n");
        prompt.append("2. 每天包含早餐、午餐、晚餐、加餐，每餐至少3种食材\n");
        prompt.append("3. 每天详细程度一致，不能简化\n");
        prompt.append("4. 每天食材要有变化\n\n");
        prompt.append("现在开始生成").append(request.getDays()).append("天计划：\n");
        
        return prompt.toString();
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
     * 分批生成饮食计划（每次生成1天，解决超时问题）
     */
    private String generateDietPlanInBatches(DietPlanRequest request, int dailyCalories, 
                                            List<FoodNutrition> allFoods, String taskId, 
                                            DietPlanTaskService taskService) {
        int totalDays = request.getDays();
        StringBuilder fullPlan = new StringBuilder();
        
        // 先生成标题和营养目标（只生成一次）
        fullPlan.append("# ").append(generatePlanTitle(request)).append("\n\n");
        fullPlan.append("## 1. 计划概述\n");
        fullPlan.append(getGoalDescription(request.getGoal())).append("计划\n\n");
        fullPlan.append("## 2. 营养目标\n");
        fullPlan.append("- 每日总热量：").append(dailyCalories).append(" kcal\n");
        fullPlan.append("- 蛋白质、碳水、脂肪比例合理分配\n\n");
        fullPlan.append("## 3. 每日详细计划\n\n");
        
        LocalDate startDate = LocalDate.now();
        
        // 逐天生成（每次生成1天，带重试机制）
        for (int day = 1; day <= totalDays; day++) {
            // 检查任务是否已被取消
            if (taskId != null && taskService != null && taskService.isTaskCancelled(taskId)) {
                log.info("任务已取消，停止生成: taskId={}, 已生成{}天", taskId, day - 1);
                throw new RuntimeException("任务已取消");
            }
            
            log.info("正在生成第{}天的计划（{}/{}）...", day, day, totalDays);
            log.info("检查进度更新条件: taskId={}, taskService={}", taskId, taskService != null ? "存在" : "null");
            
            // 更新进度：开始生成这一天
            if (taskId != null && taskService != null) {
                int progress = (int) ((day - 1) * 100.0 / totalDays);
                log.info("准备更新任务进度: {}%, 当前第{}天", progress, day);
                taskService.updateTaskStatus(taskId, "running", progress, day, null, null);
                log.info("更新任务进度完成: {}%, 当前第{}天", progress, day);
            } else {
                log.warn("跳过进度更新: taskId={}, taskService={}", taskId, taskService != null ? "存在" : "null");
            }
            
            // 构建这一天的Prompt
            String dayPrompt = buildSingleDayPrompt(request, dailyCalories, allFoods, day, startDate);
            
            // 调用AI生成这一天（带重试和取消支持）
            String dayResponse = generateWithRetry(dayPrompt, day, 2, taskId, taskService);
            log.info("✓ 第{}天生成成功，长度: {} 字符", day, dayResponse.length());
            
            // 合并结果
            fullPlan.append(dayResponse).append("\n\n");
            
            // 更新进度：这一天生成完成
            if (taskId != null && taskService != null) {
                int progress = (int) (day * 100.0 / totalDays);
                taskService.updateTaskStatus(taskId, "running", progress, day, null, null);
                log.info("第{}天完成，进度: {}%", day, progress);
            }
        }
        
        // 添加饮食建议
        fullPlan.append("## 4. 饮食建议\n");
        fullPlan.append("1. 保持规律的用餐时间\n");
        fullPlan.append("2. 多喝水，每天至少8杯\n");
        fullPlan.append("3. 控制油盐糖的摄入\n");
        fullPlan.append("4. 适量运动，配合饮食计划\n");
        fullPlan.append("5. 保证充足睡眠\n");
        
        return fullPlan.toString();
    }
    
    /**
     * 带重试机制的AI生成（支持取消）
     */
    private String generateWithRetry(String prompt, int day, int maxRetries, 
                                     String taskId, DietPlanTaskService taskService) {
        int attempt = 0;
        Exception lastException = null;
        
        while (attempt < maxRetries) {
            // 在每次重试前检查是否已取消
            if (taskId != null && taskService != null && taskService.isTaskCancelled(taskId)) {
                log.info("任务已取消，停止重试: day={}", day);
                throw new RuntimeException("任务已取消");
            }
            
            try {
                attempt++;
                if (attempt > 1) {
                    log.warn("第{}天生成失败，正在重试（第{}/{}次）...", day, attempt, maxRetries);
                }
                
                // AI调用（这是阻塞的，无法中断）
                log.info("开始调用AI生成第{}天...", day);
                String response = chatLanguageModel.generate(prompt);
                log.info("AI调用完成，第{}天", day);
                
                // AI调用完成后立即检查是否已取消
                if (taskId != null && taskService != null && taskService.isTaskCancelled(taskId)) {
                    log.info("任务在AI调用完成后被取消，丢弃结果: day={}", day);
                    throw new RuntimeException("任务已取消");
                }
                
                // 验证响应不为空
                if (response == null || response.trim().isEmpty()) {
                    throw new RuntimeException("AI返回空响应");
                }
                
                return response;
                
            } catch (Exception e) {
                // 检查是否是取消导致的异常
                if (taskId != null && taskService != null && taskService.isTaskCancelled(taskId)) {
                    log.info("任务已取消: day={}", day);
                    throw new RuntimeException("任务已取消");
                }
                
                lastException = e;
                log.error("第{}天生成失败（第{}/{}次）: {}", day, attempt, maxRetries, e.getMessage());
                
                if (attempt < maxRetries) {
                    // 等待一段时间后重试（期间检查取消状态）
                    try {
                        for (int i = 0; i < 20; i++) { // 分成20个100ms的检查
                            if (taskId != null && taskService != null && taskService.isTaskCancelled(taskId)) {
                                log.info("任务在等待重试期间被取消: day={}", day);
                                throw new RuntimeException("任务已取消");
                            }
                            Thread.sleep(100);
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("任务被中断");
                    }
                }
            }
        }
        
        // 所有重试都失败，返回简化版本
        log.error("第{}天生成失败，所有重试都失败，使用简化版本", day);
        return generateSimplifiedDay(day, LocalDate.now().plusDays(day - 1));
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
     * 构建单天的Prompt
     */
    private String buildSingleDayPrompt(DietPlanRequest request, int dailyCalories, 
                                        List<FoodNutrition> allFoods, int day, LocalDate startDate) {
        StringBuilder prompt = new StringBuilder();
        
        LocalDate currentDate = startDate.plusDays(day - 1);
        
        prompt.append("为第").append(day).append("天（")
              .append(currentDate.format(DateTimeFormatter.ofPattern("MM-dd")))
              .append("）生成饮食计划。\n\n");
        
        prompt.append("要求：\n");
        prompt.append("- 热量：").append(dailyCalories).append("kcal\n");
        prompt.append("- 目标：").append(getGoalDescription(request.getGoal())).append("\n");
        
        // 添加运动强度
        if (request.getExerciseLevel() != null && !request.getExerciseLevel().isEmpty()) {
            String exerciseDesc = switch (request.getExerciseLevel()) {
                case "low" -> "轻度运动";
                case "medium" -> "中度运动";
                case "high" -> "高强度运动";
                default -> "中度运动";
            };
            prompt.append("- 运动强度：").append(exerciseDesc).append("\n");
        }
        
        // 添加饮食偏好
        if (request.getPreferences() != null && !request.getPreferences().isEmpty()) {
            prompt.append("- 饮食偏好：").append(request.getPreferences()).append("\n");
        }
        
        // 添加过敏食物
        if (request.getAllergies() != null && !request.getAllergies().isEmpty()) {
            prompt.append("- 过敏/忌口：").append(request.getAllergies()).append("（避免使用）\n");
        }
        
        prompt.append("- 包含：早餐、午餐、晚餐、加餐，每餐3种食材+数量");
        if (request.getIncludeNutrition() != null && request.getIncludeNutrition()) {
            prompt.append("+营养数据");
        }
        if (request.getIncludeCookingTips() != null && request.getIncludeCookingTips()) {
            prompt.append("+烹饪提示");
        }
        prompt.append("\n\n");
        
        // 简化食材列表（只列出10种）
        prompt.append("可用食材：");
        List<FoodNutrition> sampleFoods = allFoods.stream().limit(10).toList();
        for (int i = 0; i < sampleFoods.size(); i++) {
            if (i > 0) prompt.append("、");
            prompt.append(sampleFoods.get(i).getFoodName());
        }
        prompt.append("等。\n\n");
        
        prompt.append("格式：\n");
        prompt.append("## 第").append(day).append("天 - ")
              .append(currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append("\n");
        prompt.append("### 早餐\n- 食材：数量\n");
        prompt.append("### 午餐\n### 晚餐\n### 加餐\n");
        if (request.getIncludeNutrition() != null && request.getIncludeNutrition()) {
            prompt.append("### 营养总计\n");
        }
        
        return prompt.toString();
    }
    
    /**
     * 使用DashScope SDK直接调用，配置自定义超时
     */
    private String callDashScopeWithTimeout(String prompt) {
        try {
            // 构建Generation参数（不使用反射修改全局httpClient，避免线程安全问题）
            GenerationParam param = GenerationParam.builder()
                    .apiKey(apiKey)
                    .model(modelName)
                    .prompt(prompt)
                    .build();
            
            // 调用API
            Generation gen = new Generation();
            GenerationResult result = gen.call(param);
            
            // 提取响应内容
            return result.getOutput().getText();
            
        } catch (ApiException | NoApiKeyException | com.alibaba.dashscope.exception.InputRequiredException e) {
            log.error("DashScope API调用失败: {}", e.getMessage());
            throw new RuntimeException("AI调用失败: " + e.getMessage(), e);
        }
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
            
            // 调用AI生成修改后的计划
            String modifiedContent = chatLanguageModel.generate(prompt);
            
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
