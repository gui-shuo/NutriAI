package com.nutriai.dto.vip;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 当前用户VIP状态汇总（在会员信息中携带）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VipStatusResponse {
    /** 是否为有效VIP */
    private Boolean isVip;
    /** VIP套餐名称 */
    private String planName;
    /** VIP到期时间（null表示非VIP） */
    private LocalDateTime vipExpireAt;
    /** 剩余天数 */
    private Long remainDays;
}
