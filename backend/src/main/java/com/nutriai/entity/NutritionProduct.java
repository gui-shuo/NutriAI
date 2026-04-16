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
 * 营养产品实体
 */
@Entity
@Table(name = "nutrition_products", indexes = {
    @Index(name = "idx_product_category", columnList = "category"),
    @Index(name = "idx_product_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NutritionProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 产品名称 */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** 产品图片URL */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** 产品图片列表（JSON数组） */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "image_urls", columnDefinition = "json")
    private List<String> imageUrls;

    /** 分类：SUPPLEMENT/PROTEIN/VITAMIN/ORGANIC/HEALTH_FOOD/EQUIPMENT */
    @Column(name = "category", nullable = false, length = 50)
    private String category;

    /** 品牌 */
    @Column(name = "brand", length = 100)
    private String brand;

    /** 简短描述 */
    @Column(name = "brief", length = 500)
    private String brief;

    /** 详细描述（富文本） */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 原价 */
    @Column(name = "original_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal originalPrice;

    /** 售价 */
    @Column(name = "sale_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal salePrice;

    /** 库存 */
    @Column(name = "stock", nullable = false)
    @Builder.Default
    private Integer stock = 999;

    /** 销量 */
    @Column(name = "sales_count")
    @Builder.Default
    private Integer salesCount = 0;

    /** 评分 */
    @Column(name = "rating", precision = 3, scale = 1)
    @Builder.Default
    private BigDecimal rating = BigDecimal.valueOf(5.0);

    /** 评价数 */
    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    /** 产品标签（JSON数组）*/
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "json")
    private List<String> tags;

    /** 规格信息（JSON） */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "specifications", columnDefinition = "json")
    private Map<String, Object> specifications;

    /** 产品状态: ON_SALE/OFF_SALE/SOLD_OUT */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ON_SALE";

    /** 是否推荐 */
    @Column(name = "is_recommended")
    @Builder.Default
    private Boolean isRecommended = false;

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
