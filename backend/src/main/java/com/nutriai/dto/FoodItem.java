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
    
    private String name;
    private Double confidence;
    /** 食品种类（天聚数行API type字段，如 畜肉类、谷类 等） */
    private String category;
    private NutritionInfo nutrition;
    
    /**
     * 营养信息 — 对齐天聚数行食物营养识别API全量字段
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NutritionInfo {
        // ---- 基础五项 ----
        private Double energy;          // 热量 kcal/100g
        private Double protein;         // 蛋白质 g
        private Double carbohydrate;    // 碳水化合物 g
        private Double fat;             // 脂肪 g
        private Double fiber;           // 膳食纤维 g

        // ---- 矿物质 ----
        private Double sodium;          // 钠 mg
        private Double calcium;         // 钙 mg
        private Double potassium;       // 钾 mg
        private Double magnesium;       // 镁 mg
        private Double iron;            // 铁 mg
        private Double zinc;            // 锌 mg
        private Double phosphorus;      // 磷 mg
        private Double selenium;        // 硒 μg
        private Double copper;          // 铜 mg
        private Double manganese;       // 锰 mg

        // ---- 维生素 ----
        private Double vitaminA;        // 维生素A μg
        private Double vitaminC;        // 维生素C mg
        private Double vitaminE;        // 维生素E mg
        private Double carotene;        // 胡萝卜素 μg
        private Double thiamine;        // 硫胺素(B1) mg
        private Double riboflavin;      // 核黄素(B2) mg
        private Double niacin;          // 烟酸(B3) mg
        private Double retinolEquivalent; // 视黄醇当量 μg

        // ---- 其他 ----
        private Double cholesterol;     // 胆固醇 mg

        /** 数据来源：tianapi / database / estimated / default */
        private String source;
    }
}
