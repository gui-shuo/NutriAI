package com.nutriai.ai;

import com.nutriai.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.Map;

/**
 * AI工具函数封装
 * 提供AI对话中需要的各种工具函数
 * 
 * @author NutriAI Team
 * @date 2025-12-03
 */
@Slf4j
@Component
public class AIToolkit {
    
    /**
     * 构建用户画像文本
     * 
     * @param user 用户实体
     * @param height 身高（cm）
     * @param weight 体重（kg）
     * @param targetWeight 目标体重（kg）
     * @param healthGoal 健康目标
     * @return 用户画像文本
     */
    public String buildUserProfile(User user, Double height, Double weight, 
                                   Double targetWeight, String healthGoal) {
        StringBuilder profile = new StringBuilder();
        
        profile.append("用户信息：\n");
        profile.append("- 昵称：").append(user.getNickname() != null ? user.getNickname() : "用户").append("\n");
        
        if (height != null && weight != null) {
            profile.append("- 身高：").append(height).append("cm\n");
            profile.append("- 体重：").append(weight).append("kg\n");
            
            double bmi = calculateBMI(height, weight);
            profile.append("- BMI：").append(String.format("%.1f", bmi))
                   .append(" (").append(getBMICategory(bmi)).append(")\n");
            
            if (targetWeight != null) {
                profile.append("- 目标体重：").append(targetWeight).append("kg\n");
                double diff = weight - targetWeight;
                if (diff > 0) {
                    profile.append("- 需要减重：").append(String.format("%.1f", diff)).append("kg\n");
                } else if (diff < 0) {
                    profile.append("- 需要增重：").append(String.format("%.1f", Math.abs(diff))).append("kg\n");
                }
            }
        }
        
        if (healthGoal != null && !healthGoal.isEmpty()) {
            profile.append("- 健康目标：").append(healthGoal).append("\n");
        }
        
        return profile.toString();
    }
    
    /**
     * 构建用户画像变量Map（用于Prompt模板）
     * 
     * @param user 用户实体
     * @param height 身高
     * @param weight 体重
     * @param targetWeight 目标体重
     * @param healthGoal 健康目标
     * @return 变量Map
     */
    public Map<String, Object> buildUserProfileVariables(User user, Double height, Double weight,
                                                         Double targetWeight, String healthGoal) {
        Map<String, Object> vars = new HashMap<>();
        
        vars.put("nickname", user.getNickname() != null ? user.getNickname() : "用户");
        vars.put("gender", "未知"); // 如果User实体有性别字段，从这里获取
        vars.put("age", "未知"); // 如果有生日，计算年龄
        
        if (height != null) {
            vars.put("height", height);
        }
        
        if (weight != null) {
            vars.put("weight", weight);
        }
        
        if (height != null && weight != null) {
            double bmi = calculateBMI(height, weight);
            vars.put("bmi", String.format("%.1f", bmi));
            vars.put("bmiCategory", getBMICategory(bmi));
        }
        
        if (targetWeight != null) {
            vars.put("targetWeight", targetWeight);
        }
        
        if (healthGoal != null) {
            vars.put("healthGoal", healthGoal);
        } else {
            vars.put("healthGoal", "保持健康");
        }
        
        return vars;
    }
    
    /**
     * 计算BMI
     * 
     * @param height 身高（cm）
     * @param weight 体重（kg）
     * @return BMI值
     */
    public double calculateBMI(Double height, Double weight) {
        if (height == null || weight == null || height <= 0 || weight <= 0) {
            return 0.0;
        }
        double heightInMeters = height / 100.0;
        return weight / (heightInMeters * heightInMeters);
    }
    
    /**
     * 获取BMI分类
     * 
     * @param bmi BMI值
     * @return BMI分类（中文）
     */
    public String getBMICategory(double bmi) {
        if (bmi < 18.5) {
            return "偏瘦";
        } else if (bmi < 24) {
            return "正常";
        } else if (bmi < 28) {
            return "偏胖";
        } else {
            return "肥胖";
        }
    }
    
    /**
     * 计算理想体重范围
     * 
     * @param height 身高（cm）
     * @return 理想体重范围文本
     */
    public String getIdealWeightRange(Double height) {
        if (height == null || height <= 0) {
            return "无法计算";
        }
        
        double heightInMeters = height / 100.0;
        double minWeight = 18.5 * heightInMeters * heightInMeters;
        double maxWeight = 24 * heightInMeters * heightInMeters;
        
        return String.format("%.1f - %.1f kg", minWeight, maxWeight);
    }
    
    /**
     * 根据年龄和生日计算年龄
     * 
     * @param birthDate 生日
     * @return 年龄
     */
    public int calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return 0;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
    
    /**
     * 估算基础代谢率（BMR）
     * 使用Harris-Benedict公式
     * 
     * @param gender 性别（M/F）
     * @param weight 体重（kg）
     * @param height 身高（cm）
     * @param age 年龄
     * @return 基础代谢率（kcal/day）
     */
    public double calculateBMR(String gender, Double weight, Double height, Integer age) {
        if (weight == null || height == null || age == null) {
            return 0.0;
        }
        
        if ("M".equalsIgnoreCase(gender) || "男".equals(gender)) {
            // 男性：BMR = 66 + (13.7 × 体重kg) + (5 × 身高cm) - (6.8 × 年龄)
            return 66 + (13.7 * weight) + (5 * height) - (6.8 * age);
        } else {
            // 女性：BMR = 655 + (9.6 × 体重kg) + (1.8 × 身高cm) - (4.7 × 年龄)
            return 655 + (9.6 * weight) + (1.8 * height) - (4.7 * age);
        }
    }
    
    /**
     * 估算每日总能量消耗（TDEE）
     * 
     * @param bmr 基础代谢率
     * @param activityLevel 活动水平（1-5）
     * @return 每日总能量消耗（kcal/day）
     */
    public double calculateTDEE(double bmr, int activityLevel) {
        double[] multipliers = {1.2, 1.375, 1.55, 1.725, 1.9};
        int index = Math.max(0, Math.min(activityLevel - 1, multipliers.length - 1));
        return bmr * multipliers[index];
    }
    
    /**
     * 格式化营养数据为表格
     * 
     * @param calories 卡路里
     * @param protein 蛋白质（g）
     * @param carbs 碳水化合物（g）
     * @param fat 脂肪（g）
     * @return Markdown表格
     */
    public String formatNutritionTable(double calories, double protein, double carbs, double fat) {
        return String.format(
            """
            | 营养成分 | 含量 |
            |---------|------|
            | 热量 | %.0f kcal |
            | 蛋白质 | %.1f g |
            | 碳水化合物 | %.1f g |
            | 脂肪 | %.1f g |
            """,
            calories, protein, carbs, fat
        );
    }
    
    /**
     * 清理和格式化AI响应
     * 
     * @param response AI原始响应
     * @return 格式化后的响应
     */
    public String formatAIResponse(String response) {
        if (response == null || response.isEmpty()) {
            return "";
        }
        
        // 移除多余的空行
        String cleaned = response.replaceAll("\\n{3,}", "\n\n");
        
        // 确保Markdown标题前后有空行
        cleaned = cleaned.replaceAll("([^\\n])\\n(#{1,6} )", "$1\n\n$2");
        cleaned = cleaned.replaceAll("(#{1,6} [^\\n]+)\\n([^\\n])", "$1\n\n$2");
        
        return cleaned.trim();
    }
    
    /**
     * 检查消息是否包含敏感内容
     * 
     * @param message 消息内容
     * @return 是否包含敏感内容
     */
    public boolean containsSensitiveContent(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        
        String lowerMessage = message.toLowerCase();
        
        // 检查医疗相关敏感词
        String[] medicalKeywords = {
            "治疗", "诊断", "药物", "处方", "手术", "疾病",
            "cure", "diagnosis", "medicine", "prescription"
        };
        
        for (String keyword : medicalKeywords) {
            if (lowerMessage.contains(keyword)) {
                log.warn("检测到敏感医疗关键词: {}", keyword);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 生成医疗免责声明
     * 
     * @return 免责声明文本
     */
    public String getMedicalDisclaimer() {
        return """
            
            ---
            ⚠️ **重要提示**：
            - 本建议仅供参考，不构成医疗建议
            - 如有疾病或特殊健康状况，请咨询专业医生
            - 开始任何新的饮食或运动计划前，建议先咨询医疗专业人员
            """;
    }
}
