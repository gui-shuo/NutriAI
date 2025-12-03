package com.nutriai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 饮食记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "food_records")
public class FoodRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * 餐次类型：BREAKFAST(早餐), LUNCH(午餐), DINNER(晚餐), SNACK(加餐)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 20)
    private MealType mealType;
    
    /**
     * 食物名称
     */
    @Column(name = "food_name", nullable = false, length = 100)
    private String foodName;
    
    /**
     * 食物照片URL
     */
    @Column(name = "photo_url", length = 500)
    private String photoUrl;
    
    /**
     * 份量（克）
     */
    @Column(precision = 8, scale = 2)
    private BigDecimal portion;
    
    /**
     * 卡路里（千卡）
     */
    @Column(precision = 8, scale = 2)
    private BigDecimal calories;
    
    /**
     * 蛋白质（克）
     */
    @Column(precision = 8, scale = 2)
    private BigDecimal protein;
    
    /**
     * 碳水化合物（克）
     */
    @Column(precision = 8, scale = 2)
    private BigDecimal carbohydrates;
    
    /**
     * 脂肪（克）
     */
    @Column(precision = 8, scale = 2)
    private BigDecimal fat;
    
    /**
     * 纤维（克）
     */
    @Column(precision = 8, scale = 2)
    private BigDecimal fiber;
    
    /**
     * 记录日期时间
     */
    @Column(name = "record_time", nullable = false)
    private LocalDateTime recordTime;
    
    /**
     * 备注
     */
    @Column(length = 500)
    private String notes;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 餐次类型枚举
     */
    public enum MealType {
        BREAKFAST("早餐"),
        LUNCH("午餐"),
        DINNER("晚餐"),
        SNACK("加餐");
        
        private final String displayName;
        
        MealType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
