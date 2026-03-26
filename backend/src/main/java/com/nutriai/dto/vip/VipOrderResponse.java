package com.nutriai.dto.vip;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VipOrderResponse {
    private Long id;
    private String orderNo;
    private String planName;
    private BigDecimal amount;
    private String paymentMethod;
    private String paymentStatus;
    private String paymentStatusName;
    /** 支付宝收银台跳转URL（创建订单时返回） */
    private String payUrl;
    private LocalDateTime expireTime;
    private LocalDateTime vipExpireAt;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
