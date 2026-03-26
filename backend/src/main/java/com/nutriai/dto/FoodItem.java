package com.nutriai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 识别的食物项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodItem {
    
    /**
     * 食物名称
     */
    private String name;
    
    /**
     * 置信度（0-1）
     */
    private Double confidence;
    
    /**
     * 营养信息
     */
    private NutritionInfo nutrition;
    
    /**
     * 营养信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NutritionInfo {
        /**
         * 热量（kcal/100g）
         */
        private Double energy;
        
        /**
         * 蛋白质（g/100g）
         */
        private Double protein;
        
        /**
         * 碳水化合物（g/100g）
         */
        private Double carbohydrate;
        
        /**
         * 脂肪（g/100g）
         */
        private Double fat;
        
        /**
         * 膳食纤维（g/100g）
         */
        private Double fiber;
        
        /**
         * 数据来源：database（数据库）/ estimated（AI估算）
         */
        private String source;
    }
}
