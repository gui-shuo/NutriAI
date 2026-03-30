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
}
