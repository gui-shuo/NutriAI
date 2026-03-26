package com.nutriai.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResponse {
    
    /**
     * 验证码Key（用于后续验证）
     */
    private String captchaKey;
    
    /**
     * 验证码图片Base64编码
     */
    private String captchaImage;
    
    /**
     * 过期时间（秒）
     */
    @Builder.Default
    private Long expiresIn = 300L; // 5分钟
}
