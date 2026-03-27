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
 * 咨询订单实体
 */
@Entity
@Table(name = "consultation_orders", indexes = {
    @Index(name = "idx_consult_user_id", columnList = "user_id"),
    @Index(name = "idx_consult_nutritionist_id", columnList = "nutritionist_id"),
    @Index(name = "idx_consult_order_no", columnList = "order_no", unique = true),
    @Index(name = "idx_consult_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 订单号 */
    @Column(name = "order_no", nullable = false, unique = true, length = 32)
    private String orderNo;

    /** 用户ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 营养师ID */
    @Column(name = "nutritionist_id", nullable = false)
    private Long nutritionistId;

    /** 营养师姓名（冗余） */
    @Column(name = "nutritionist_name", nullable = false, length = 50)
    private String nutritionistName;

    /** 咨询费用 */
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /** 支付方式 */
    @Column(name = "payment_method", nullable = false, length = 20)
    @Builder.Default
    private String paymentMethod = "SIMULATE";

    /** 支付状态: PENDING/PAID/REFUNDED/CANCELLED/EXPIRED */
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private String paymentStatus = "PENDING";

    /** 咨询状态: WAITING/IN_PROGRESS/COMPLETED/CANCELLED */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "WAITING";

    /** 咨询类型: TEXT/VIDEO */
    @Column(name = "consultation_type", nullable = false, length = 20)
    @Builder.Default
    private String consultationType = "TEXT";

    /** 用户咨询描述 */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 聊天消息记录（JSON) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "messages", columnDefinition = "json")
    private List<Map<String, Object>> messages;

    /** 营养师总结/建议 */
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    /** 用户评分（1-5） */
    @Column(name = "user_rating")
    private Integer userRating;

    /** 用户评价 */
    @Column(name = "user_review", length = 500)
    private String userReview;

    /** 订单超时时间 */
    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    /** 支付时间 */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /** 咨询开始时间 */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /** 咨询结束时间 */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

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
