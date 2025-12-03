package com.nutriai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 食材营养数据实体
 * 
 * @author NutriAI Team
 * @date 2025-12-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "food_nutrition")
public class FoodNutrition {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "food_code", nullable = false, unique = true, length = 50)
    private String foodCode;
    
    @Column(name = "food_name", nullable = false, length = 200)
    private String foodName;
    
    @Column(name = "food_name_en", length = 200)
    private String foodNameEn;
    
    @Column(name = "category_id", nullable = false)
    private Long categoryId;
    
    // 基本信息
    @Column(name = "edible_portion", precision = 5, scale = 2)
    private BigDecimal ediblePortion;
    
    // 能量
    @Column(name = "energy", precision = 10, scale = 2)
    private BigDecimal energy;
    
    // 三大营养素
    @Column(name = "protein", precision = 10, scale = 2)
    private BigDecimal protein;
    
    @Column(name = "fat", precision = 10, scale = 2)
    private BigDecimal fat;
    
    @Column(name = "carbohydrate", precision = 10, scale = 2)
    private BigDecimal carbohydrate;
    
    @Column(name = "dietary_fiber", precision = 10, scale = 2)
    private BigDecimal dietaryFiber;
    
    // 维生素
    @Column(name = "vitamin_a", precision = 10, scale = 2)
    private BigDecimal vitaminA;
    
    @Column(name = "vitamin_b1", precision = 10, scale = 2)
    private BigDecimal vitaminB1;
    
    @Column(name = "vitamin_b2", precision = 10, scale = 2)
    private BigDecimal vitaminB2;
    
    @Column(name = "vitamin_c", precision = 10, scale = 2)
    private BigDecimal vitaminC;
    
    @Column(name = "vitamin_e", precision = 10, scale = 2)
    private BigDecimal vitaminE;
    
    // 矿物质
    @Column(name = "calcium", precision = 10, scale = 2)
    private BigDecimal calcium;
    
    @Column(name = "phosphorus", precision = 10, scale = 2)
    private BigDecimal phosphorus;
    
    @Column(name = "potassium", precision = 10, scale = 2)
    private BigDecimal potassium;
    
    @Column(name = "sodium", precision = 10, scale = 2)
    private BigDecimal sodium;
    
    @Column(name = "magnesium", precision = 10, scale = 2)
    private BigDecimal magnesium;
    
    @Column(name = "iron", precision = 10, scale = 2)
    private BigDecimal iron;
    
    @Column(name = "zinc", precision = 10, scale = 2)
    private BigDecimal zinc;
    
    @Column(name = "selenium", precision = 10, scale = 2)
    private BigDecimal selenium;
    
    // 其他营养素
    @Column(name = "cholesterol", precision = 10, scale = 2)
    private BigDecimal cholesterol;
    
    @Column(name = "water", precision = 10, scale = 2)
    private BigDecimal water;
    
    // 标签和属性
    @Column(name = "tags", length = 500)
    private String tags;
    
    @Column(name = "season", length = 100)
    private String season;
    
    @Column(name = "storage_method", length = 200)
    private String storageMethod;
    
    @Column(name = "cooking_methods", length = 500)
    private String cookingMethods;
    
    // 健康信息
    @Column(name = "health_benefits", columnDefinition = "TEXT")
    private String healthBenefits;
    
    @Column(name = "suitable_people", length = 500)
    private String suitablePeople;
    
    @Column(name = "unsuitable_people", length = 500)
    private String unsuitablePeople;
    
    // 搜索优化
    @Column(name = "search_keywords", length = 500)
    private String searchKeywords;
    
    @Column(name = "pinyin", length = 200)
    private String pinyin;
    
    // 数据来源
    @Column(name = "data_source", length = 100)
    private String dataSource;
    
    @Column(name = "reference_url", length = 500)
    private String referenceUrl;
    
    // 状态
    @Column(name = "status", length = 20)
    private String status;
    
    @Column(name = "is_verified")
    private Boolean isVerified;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "ACTIVE";
        }
        if (isVerified == null) {
            isVerified = false;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
