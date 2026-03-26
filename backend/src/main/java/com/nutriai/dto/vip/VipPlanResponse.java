package com.nutriai.dto.vip;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VipPlanResponse {
    private Long id;
    private String planCode;
    private String planName;
    private String description;
    private BigDecimal originalPrice;
    private BigDecimal discountPrice;
    private Integer durationDays;
    private Map<String, Object> benefits;
    private Integer bonusGrowth;
    private String badge;
    /** 折扣率，如 "5折" */
    private String discountLabel;
    /** 每天均摊价格（元） */
    private BigDecimal pricePerDay;
}
