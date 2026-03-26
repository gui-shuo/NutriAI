package com.nutriai.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 重置密码请求DTO
 */
@Data
public class ResetPasswordRequest {
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码长度为6位")
    private String code;
    
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]+$",
        message = "密码必须包含大小写字母和数字"
    )
    private String newPassword;
    
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
    
    public boolean isPasswordMatch() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}
