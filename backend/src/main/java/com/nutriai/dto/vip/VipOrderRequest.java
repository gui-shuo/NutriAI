package com.nutriai.dto.vip;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VipOrderRequest {

    @NotNull(message = "套餐ID不能为空")
    private Long planId;

    /** 支付方式，目前仅支持 ALIPAY */
    private String paymentMethod = "ALIPAY";
}
