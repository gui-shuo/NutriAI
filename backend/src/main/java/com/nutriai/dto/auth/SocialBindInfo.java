package com.nutriai.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 社交账号绑定状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialBindInfo {
    
    private boolean wechatBound;
    private String wechatNickname;
    
    private boolean qqBound;
    private String qqNickname;

    /** APP端缺少QQ openId，需要在APP端验证绑定 */
    private boolean qqNeedAppVerify;

    /** Web端缺少QQ openId，需要在Web端验证绑定 */
    private boolean qqNeedWebVerify;
}
