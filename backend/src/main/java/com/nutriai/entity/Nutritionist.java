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
 * 营养师实体
 */
@Entity
@Table(name = "nutritionists", indexes = {
    @Index(name = "idx_nutritionist_status", columnList = "status"),
    @Index(name = "idx_nutritionist_rating", columnList = "rating")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Nutritionist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的用户账号ID（可为空，兼容未绑定账号的营养师） */
    @Column(name = "user_id", unique = true)
    private Long userId;

    /** 营养师姓名 */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /** 头像URL */
    @Column(name = "avatar", length = 500)
    private String avatar;

    /** 职称：初级营养师/中级营养师/高级营养师/注册营养师 */
    @Column(name = "title", nullable = false, length = 50)
    private String title;

    /** 专业领域标签（JSON数组） */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "specialties", columnDefinition = "json")
    private List<String> specialties;

    /** 简介 */
    @Column(name = "introduction", columnDefinition = "TEXT")
    private String introduction;

    /** 从业年限 */
    @Column(name = "experience_years", nullable = false)
    @Builder.Default
    private Integer experienceYears = 0;

    /** 咨询费/次（元） */
    @Column(name = "consultation_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal consultationFee;

    /** 评分（5分制） */
    @Column(name = "rating", precision = 3, scale = 1)
    @Builder.Default
    private BigDecimal rating = BigDecimal.valueOf(5.0);

    /** 咨询次数 */
    @Column(name = "consultation_count")
    @Builder.Default
    private Integer consultationCount = 0;

    /** 状态：ONLINE/OFFLINE/BUSY */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "OFFLINE";

    /** 资质证书信息（JSON） */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "certificates", columnDefinition = "json")
    private Map<String, Object> certificates;

    /** 工作时间描述 */
    @Column(name = "working_hours", length = 200)
    private String workingHours;

    /** 是否启用 */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /** 排序权重 */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

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
