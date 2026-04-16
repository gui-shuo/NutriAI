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
 * 营养餐订单实体
 */
@Entity
@Table(name = "meal_orders", indexes = {
    @Index(name = "idx_meal_order_user", columnList = "user_id"),
    @Index(name = "idx_meal_order_no", columnList = "order_no", unique = true),
    @Index(name = "idx_meal_order_status", columnList = "order_status"),
    @Index(name = "idx_meal_order_date", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 订单号 */
    @Column(name = "order_no", nullable = false, unique = true, length = 32)
    private String orderNo;

    /** 用户ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 订单商品列表（JSON） */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "items", columnDefinition = "json")
    private List<Map<String, Object>> items;

    /** 总数量 */
    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    /** 总金额 */
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /** 支付方式: WECHAT_PAY/SIMULATE */
    @Column(name = "payment_method", nullable = false, length = 20)
    @Builder.Default
    private String paymentMethod = "WECHAT_PAY";

    /** 支付状态: PENDING/PAID/REFUNDED */
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private String paymentStatus = "PENDING";

    /** 订单状态: PENDING_PAYMENT/PAID/PREPARING/READY/PICKED_UP/DELIVERED/COMPLETED/CANCELLED */
    @Column(name = "order_status", nullable = false, length = 20)
    @Builder.Default
    private String orderStatus = "PENDING_PAYMENT";

    /** 履约方式: PICKUP/DELIVERY */
    @Column(name = "fulfillment_type", nullable = false, length = 20)
    @Builder.Default
    private String fulfillmentType = "PICKUP";

    /** 取餐时间 */
    @Column(name = "pickup_time", length = 50)
    private String pickupTime;

    /** 取餐地点 */
    @Column(name = "pickup_location", length = 200)
    @Builder.Default
    private String pickupLocation = "营养工作室";

    /** 收货人 */
    @Column(name = "receiver_name", length = 50)
    private String receiverName;

    /** 联系电话 */
    @Column(name = "receiver_phone", length = 20)
    private String receiverPhone;

    /** 配送地址(配送时) */
    @Column(name = "receiver_address", length = 500)
    private String receiverAddress;

    /** 备注 */
    @Column(name = "remark", length = 500)
    private String remark;

    /** 使用的优惠券ID */
    @Column(name = "coupon_id")
    private Long couponId;

    /** 优惠金额 */
    @Column(name = "discount_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** 微信支付交易号 */
    @Column(name = "wx_transaction_id", length = 64)
    private String wxTransactionId;

    /** 支付时间 */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /** 备餐完成时间 */
    @Column(name = "ready_at")
    private LocalDateTime readyAt;

    /** 完成时间 */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** 取消时间 */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    /** 取消原因 */
    @Column(name = "cancel_reason", length = 200)
    private String cancelReason;

    /** 取餐码（6位数字） */
    @Column(name = "pickup_code", length = 6)
    private String pickupCode;

    /** 取餐码核验时间 */
    @Column(name = "pickup_code_verified_at")
    private LocalDateTime pickupCodeVerifiedAt;

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
