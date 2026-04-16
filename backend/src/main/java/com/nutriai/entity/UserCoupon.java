package com.nutriai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 用户优惠券
 */
@Entity
@Table(name = "user_coupons", indexes = {
    @Index(name = "idx_user_coupon_user_id", columnList = "user_id"),
    @Index(name = "idx_user_coupon_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "coupon_id", insertable = false, updatable = false)
    private Coupon coupon;

    /** UNUSED / USED / EXPIRED */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "UNUSED";

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "used_order_no", length = 32)
    private String usedOrderNo;

    @Column(name = "expire_at", nullable = false)
    private LocalDateTime expireAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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
