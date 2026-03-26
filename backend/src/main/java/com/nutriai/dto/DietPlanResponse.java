package com.nutriai.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * 饮食计划响应
 */
@Data
@Builder
public class DietPlanResponse {
    
    /**
     * 计划ID
     */
    private String planId;
    
    /**
     * 计划标题
     */
    private String title;
    
    /**
     * 计划天数
     */
    private Integer days;
    
    /**
     * 目标描述
     */
    private String goalDescription;
    
    /**
     * 完整的Markdown内容
     */
    private String markdownContent;
    
    /**
     * 每日计划列表
     */
    private List<DailyPlan> dailyPlans;
    
    /**
     * 采购清单
     */
    private ShoppingList shoppingList;
    
    /**
     * 营养总结
     */
    private NutritionSummary nutritionSummary;
    
    /**
     * 是否收藏
     */
    private Boolean isFavorite;
    
    /**
     * 生成时间
     */
    private Long timestamp;
    
    /**
     * 每日计划
     */
    @Data
    @Builder
    public static class DailyPlan {
        private Integer day;
        private String date;
        private List<Meal> meals;
        private Integer totalCalories;
        private Double totalProtein;
        private Double totalCarbs;
        private Double totalFat;
    }
    
    /**
     * 单餐
     */
    @Data
    @Builder
    public static class Meal {
        private String mealType; // 早餐、午餐、晚餐、加餐
        private String time;
        private List<FoodItem> foods;
        private Integer calories;
        private String cookingTips;
    }
    
    /**
     * 食物项
     */
    @Data
    @Builder
    public static class FoodItem {
        private String name;
        private Double quantity;
        private String unit;
        private Integer calories;
        private Double protein;
        private Double carbs;
        private Double fat;
    }
    
    /**
     * 采购清单
     */
    @Data
    @Builder
    public static class ShoppingList {
        private List<ShoppingItem> items;
        private String tips;
    }
    
    /**
     * 采购项
     */
    @Data
    @Builder
    public static class ShoppingItem {
        private String category; // 蔬菜、水果、肉类、主食等
        private String name;
        private Double quantity;
        private String unit;
    }
    
    /**
     * 营养总结
     */
    @Data
    @Builder
    public static class NutritionSummary {
        private Integer avgDailyCalories;
        private Double avgProtein;
        private Double avgCarbs;
        private Double avgFat;
        private String proteinRatio;
        private String carbsRatio;
        private String fatRatio;
        private String advice;
    }
}
