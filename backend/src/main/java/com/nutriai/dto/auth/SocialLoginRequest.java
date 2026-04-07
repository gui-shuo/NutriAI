package com.nutriai.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 第三方社交登录请求
 */
@Data
public class SocialLoginRequest {
    
    @NotBlank(message = "授权码不能为空")
    private String code;
    
    /**
     * 社交平台类型: wechat, qq
     */
    @NotBlank(message = "平台类型不能为空")
    private String provider;

    /**
     * OAuth state参数，用于区分平台来源(app_login, h5_login, login等)
     */
    private String state;
}
