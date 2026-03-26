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
import java.util.Map;

/**
 * VIP套餐实体 - 充值类型会员
 */
@Entity
@Table(name = "vip_plans")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VipPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 套餐代码：MONTHLY/QUARTERLY/YEARLY
     */
    @Column(name = "plan_code", nullable = false, unique = true, length = 20)
    private String planCode;

    /**
     * 套餐名称，如"营养月卡"
     */
    @Column(name = "plan_name", nullable = false, length = 100)
    private String planName;

    /**
     * 套餐描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 原价（元）
     */
    @Column(name = "original_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal originalPrice;

    /**
     * 优惠价（元）
     */
    @Column(name = "discount_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountPrice;

    /**
     * 有效天数
     */
    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    /**
     * 套餐权益配置（JSON）
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "benefits", columnDefinition = "json")
    private Map<String, Object> benefits;

    /**
     * 购买赠送成长值
     */
    @Column(name = "bonus_growth", nullable = false)
    @Builder.Default
    private Integer bonusGrowth = 0;

    /**
     * 是否启用
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 排序权重（越小越靠前）
     */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * 促销标签，如"热门"、"推荐"、"超值"
     */
    @Column(name = "badge", length = 20)
    private String badge;

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
