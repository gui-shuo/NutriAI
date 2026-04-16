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
 * 产品订单实体
 */
@Entity
@Table(name = "product_orders", indexes = {
    @Index(name = "idx_prod_order_user_id", columnList = "user_id"),
    @Index(name = "idx_prod_order_no", columnList = "order_no", unique = true),
    @Index(name = "idx_prod_order_status", columnList = "order_status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOrder {

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

    /** 商品总数量 */
    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    /** 订单总金额 */
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /** 支付方式 */
    @Column(name = "payment_method", nullable = false, length = 20)
    @Builder.Default
    private String paymentMethod = "SIMULATE";

    /** 支付状态: PENDING/PAID/REFUNDED/CANCELLED/EXPIRED */
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private String paymentStatus = "PENDING";

    /** 订单状态: PENDING_PAYMENT/PAID/SHIPPED/DELIVERED/COMPLETED/CANCELLED/REFUNDED */
    @Column(name = "order_status", nullable = false, length = 20)
    @Builder.Default
    private String orderStatus = "PENDING_PAYMENT";

    /** 收货人姓名 */
    @Column(name = "receiver_name", length = 50)
    private String receiverName;

    /** 收货人手机 */
    @Column(name = "receiver_phone", length = 20)
    private String receiverPhone;

    /** 收货地址 */
    @Column(name = "receiver_address", length = 500)
    private String receiverAddress;

    /** 物流单号 */
    @Column(name = "tracking_no", length = 50)
    private String trackingNo;

    /** 用户备注 */
    @Column(name = "remark", length = 500)
    private String remark;

    /** 使用的优惠券ID */
    @Column(name = "coupon_id")
    private Long couponId;

    /** 优惠金额 */
    @Column(name = "discount_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** 配送方式: EXPRESS=快递 PICKUP=自提 */
    @Column(name = "fulfillment_type", length = 20)
    @Builder.Default
    private String fulfillmentType = "EXPRESS";

    /** 自提码（自提时使用） */
    @Column(name = "pickup_code", length = 6)
    private String pickupCode;

    /** 物流公司 */
    @Column(name = "shipping_company", length = 50)
    private String shippingCompany;

    /** 订单超时时间 */
    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    /** 支付时间 */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /** 发货时间 */
    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    /** 完成时间 */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** 取消原因 */
    @Column(name = "cancel_reason", length = 200)
    private String cancelReason;

    /** 取消时间 */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    /** 自动确认收货时间 */
    @Column(name = "auto_confirm_at")
    private LocalDateTime autoConfirmAt;

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
