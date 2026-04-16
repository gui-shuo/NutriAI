package com.nutriai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 营养餐菜品实体
 */
@Entity
@Table(name = "meal_items", indexes = {
    @Index(name = "idx_meal_category", columnList = "category"),
    @Index(name = "idx_meal_type", columnList = "meal_type"),
    @Index(name = "idx_meal_available", columnList = "is_available")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 餐品名称 */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** 主图URL */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** 图片列表（JSON数组） */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "image_urls", columnDefinition = "json")
    private List<String> imageUrls;

    /** 分类: ANTI_INFLAMMATORY/LOW_FAT/HIGH_PROTEIN/VEGETARIAN */
    @Column(name = "category", nullable = false, length = 50)
    @Builder.Default
    private String category = "ANTI_INFLAMMATORY";

    /** 餐类: BREAKFAST/LUNCH/DINNER/SNACK */
    @Column(name = "meal_type", length = 20)
    @Builder.Default
    private String mealType = "LUNCH";

    /** 简短描述 */
    @Column(name = "brief", length = 500)
    private String brief;

    /** 详细描述 */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 食材成分 */
    @Column(name = "ingredients", columnDefinition = "TEXT")
    private String ingredients;

    /** 营养信息（JSON）{calories,protein,fat,carbs,fiber} */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "nutrition_info", columnDefinition = "json")
    private Map<String, Object> nutritionInfo;

    /** 原价 */
    @Column(name = "original_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal originalPrice;

    /** 售价 */
    @Column(name = "sale_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal salePrice;

    /** 每日库存 */
    @Column(name = "stock", nullable = false)
    @Builder.Default
    private Integer stock = 999;

    /** 每人每日限购(0=不限) */
    @Column(name = "daily_limit")
    @Builder.Default
    private Integer dailyLimit = 0;

    /** 累计销量 */
    @Column(name = "sales_count")
    @Builder.Default
    private Integer salesCount = 0;

    /** 评分 */
    @Column(name = "rating", precision = 3, scale = 1)
    @Builder.Default
    private BigDecimal rating = BigDecimal.valueOf(5.0);

    /** 标签（JSON数组） */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "json")
    private List<String> tags;

    /** 是否推荐 */
    @Column(name = "is_recommended")
    @Builder.Default
    private Boolean isRecommended = false;

    /** 是否上架 */
    @Column(name = "is_available")
    @Builder.Default
    private Boolean isAvailable = true;

    /** 供应日(1=周一) */
    @Column(name = "available_days", length = 100)
    @Builder.Default
    private String availableDays = "1,2,3,4,5";

    /** 开始供应时间 */
    @Column(name = "available_start_time", length = 10)
    @Builder.Default
    private String availableStartTime = "07:00";

    /** 结束供应时间 */
    @Column(name = "available_end_time", length = 10)
    @Builder.Default
    private String availableEndTime = "20:00";

    /** 排序 */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    /** 乐观锁 */
    @Version
    @Column(name = "version")
    @Builder.Default
    private Integer version = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
