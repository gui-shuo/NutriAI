package com.nutriai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * VIP充值订单实体
 */
@Entity
@Table(name = "vip_orders", indexes = {
    @Index(name = "idx_vip_orders_user_id", columnList = "user_id"),
    @Index(name = "idx_vip_orders_order_no", columnList = "order_no", unique = true),
    @Index(name = "idx_vip_orders_trade_no", columnList = "trade_no")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VipOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 业务订单号（唯一，格式：VIP+yyyyMMddHHmmss+随机4位）
     */
    @Column(name = "order_no", nullable = false, unique = true, length = 32)
    private String orderNo;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 套餐ID
     */
    @Column(name = "plan_id", nullable = false)
    private Long planId;

    /**
     * 套餐名称（冗余字段，防止套餐被修改后历史订单显示异常）
     */
    @Column(name = "plan_name", nullable = false, length = 100)
    private String planName;

    /**
     * 实付金额（元）
     */
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * 支付方式：ALIPAY
     */
    @Column(name = "payment_method", nullable = false, length = 20)
    @Builder.Default
    private String paymentMethod = "ALIPAY";

    /**
     * 支付状态：PENDING/PAID/FAILED/REFUNDED/CANCELLED/EXPIRED
     */
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private String paymentStatus = "PENDING";

    /**
     * 支付宝交易流水号
     */
    @Column(name = "trade_no", length = 64)
    private String tradeNo;

    /**
     * 实际支付时间
     */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /**
     * 订单超时取消时间
     */
    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    /**
     * VIP到期时间（支付成功后按套餐天数计算）
     */
    @Column(name = "vip_expire_at")
    private LocalDateTime vipExpireAt;

    /**
     * 赠送成长值（已发放）
     */
    @Column(name = "bonus_growth_granted", nullable = false)
    @Builder.Default
    private Boolean bonusGrowthGranted = false;

    /**
     * 支付回调原始数据（脱敏存储，用于审计）
     */
    @Column(name = "notify_data", columnDefinition = "TEXT")
    private String notifyData;

    /**
     * 备注
     */
    @Column(name = "remark", length = 500)
    private String remark;

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
