package com.nutriai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 退款申请
 */
@Entity
@Table(name = "refund_requests", indexes = {
    @Index(name = "idx_refund_order_no", columnList = "order_no"),
    @Index(name = "idx_refund_user_id", columnList = "user_id"),
    @Index(name = "idx_refund_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "refund_no", nullable = false, unique = true, length = 32)
    private String refundNo;

    @Column(name = "order_no", nullable = false, length = 32)
    private String orderNo;

    /** MEAL / PRODUCT */
    @Column(name = "order_type", nullable = false, length = 20)
    private String orderType;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "refund_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "images", columnDefinition = "json")
    private List<String> images;

    /** PENDING / APPROVED / REJECTED / COMPLETED */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "admin_remark", length = 500)
    private String adminRemark;

    @Column(name = "processed_by")
    private Long processedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
