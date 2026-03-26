package com.nutriai.exception;

import lombok.Getter;

/**
 * 业务异常类
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private final Integer code;
    private final String message;
    
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
    
    public BusinessException(String message) {
        this(400, message);
    }
    
    /**
     * 认证相关业务异常
     */
    public static class Auth {
        public static final int CODE_USERNAME_EXISTS = 40001;
        public static final int CODE_EMAIL_EXISTS = 40002;
        public static final int CODE_PHONE_EXISTS = 40003;
        public static final int CODE_PASSWORD_MISMATCH = 40004;
        public static final int CODE_CAPTCHA_INVALID = 40005;
        public static final int CODE_LOGIN_FAILED = 40006;
        public static final int CODE_ACCOUNT_DISABLED = 40007;
        public static final int CODE_ACCOUNT_BANNED = 40008;
        public static final int CODE_TOKEN_INVALID = 40009;
        public static final int CODE_USER_NOT_FOUND = 40010;
        public static final int CODE_CAPTCHA_REQUIRED = 40011;
        public static final int CODE_EMAIL_SEND_FAILED = 40012;
        public static final int CODE_RESET_CODE_INVALID = 40013;

        public static BusinessException usernameExists() { return new BusinessException(CODE_USERNAME_EXISTS, "用户名已存在"); }
        public static BusinessException emailExists() { return new BusinessException(CODE_EMAIL_EXISTS, "邮箱已被注册"); }
        public static BusinessException phoneExists() { return new BusinessException(CODE_PHONE_EXISTS, "手机号已被注册"); }
        public static BusinessException passwordMismatch() { return new BusinessException(CODE_PASSWORD_MISMATCH, "两次密码输入不一致"); }
        public static BusinessException captchaInvalid() { return new BusinessException(CODE_CAPTCHA_INVALID, "验证码错误或已过期"); }
        public static BusinessException loginFailed() { return new BusinessException(CODE_LOGIN_FAILED, "用户名或密码错误"); }
        public static BusinessException accountDisabled() { return new BusinessException(CODE_ACCOUNT_DISABLED, "账号已被禁用"); }
        public static BusinessException accountBanned() { return new BusinessException(CODE_ACCOUNT_BANNED, "账号已被封禁"); }
        public static BusinessException tokenInvalid() { return new BusinessException(CODE_TOKEN_INVALID, "令牌无效或已过期"); }
        public static BusinessException userNotFound() { return new BusinessException(CODE_USER_NOT_FOUND, "用户不存在"); }
        public static BusinessException captchaRequired() { return new BusinessException(CODE_CAPTCHA_REQUIRED, "登录失败次数过多，需要验证码"); }
        public static BusinessException emailSendFailed() { return new BusinessException(CODE_EMAIL_SEND_FAILED, "邮件发送失败，请稍后重试"); }
        public static BusinessException resetCodeInvalid() { return new BusinessException(CODE_RESET_CODE_INVALID, "重置验证码错误或已过期"); }

    }
    
    /**
     * 用户相关业务异常
     */
    public static class User {
        public static final int CODE_USER_NOT_FOUND = 40101;
        public static final int CODE_EMAIL_ALREADY_EXISTS = 40102;
        public static final int CODE_PHONE_ALREADY_EXISTS = 40103;
        public static final int CODE_OLD_PASSWORD_INCORRECT = 40104;
        public static final int CODE_NEW_PASSWORD_SAME_AS_OLD = 40105;
        public static final int CODE_SMS_CODE_INVALID = 40106;
        public static final int CODE_SMS_CODE_EXPIRED = 40107;
        public static final int CODE_FILE_UPLOAD_FAILED = 40108;
        public static final int CODE_FILE_TYPE_NOT_ALLOWED = 40109;
        public static final int CODE_FILE_SIZE_EXCEEDED = 40110;

        public static BusinessException userNotFound() { return new BusinessException(CODE_USER_NOT_FOUND, "用户不存在"); }
        public static BusinessException emailAlreadyExists() { return new BusinessException(CODE_EMAIL_ALREADY_EXISTS, "邮箱已被使用"); }
        public static BusinessException phoneAlreadyExists() { return new BusinessException(CODE_PHONE_ALREADY_EXISTS, "手机号已被使用"); }
        public static BusinessException oldPasswordIncorrect() { return new BusinessException(CODE_OLD_PASSWORD_INCORRECT, "旧密码不正确"); }
        public static BusinessException newPasswordSameAsOld() { return new BusinessException(CODE_NEW_PASSWORD_SAME_AS_OLD, "新密码不能与旧密码相同"); }
        public static BusinessException smsCodeInvalid() { return new BusinessException(CODE_SMS_CODE_INVALID, "短信验证码错误"); }
        public static BusinessException smsCodeExpired() { return new BusinessException(CODE_SMS_CODE_EXPIRED, "短信验证码已过期"); }
        public static BusinessException fileUploadFailed() { return new BusinessException(CODE_FILE_UPLOAD_FAILED, "文件上传失败"); }
        public static BusinessException fileTypeNotAllowed() { return new BusinessException(CODE_FILE_TYPE_NOT_ALLOWED, "不支持的文件类型"); }
        public static BusinessException fileSizeExceeded() { return new BusinessException(CODE_FILE_SIZE_EXCEEDED, "文件大小超过限制"); }
    }
}
