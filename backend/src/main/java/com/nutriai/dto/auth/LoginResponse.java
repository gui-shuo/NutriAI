package com.nutriai.dto.auth;

import com.nutriai.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户登录响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    
    /**
     * 访问令牌
     */
    private String accessToken;
    
    /**
     * 刷新令牌
     */
    private String refreshToken;
    
    /**
     * 令牌类型
     */
    @Builder.Default
    private String tokenType = "Bearer";
    
    /**
     * 过期时间（秒）
     */
    private Long expiresIn;
    
    /**
     * 用户信息
     */
    private UserInfo userInfo;
    
    /**
     * 用户信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String nickname;
        private String email;
        private String phone;
        private String avatar;
        private String role;
        private String status;
        private LocalDateTime lastLoginTime;
        private boolean wechatBound;
        private boolean qqBound;
        
        /**
         * 从User实体构建UserInfo
         */
        public static UserInfo fromUser(User user) {
            return UserInfo.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .avatar(user.getAvatar())
                    .role(user.getRole())
                    .status(user.getStatus())
                    .lastLoginTime(user.getLastLoginTime())
                    .wechatBound(user.getWxOpenId() != null && !user.getWxOpenId().isBlank())
                    .qqBound(user.getQqOpenId() != null && !user.getQqOpenId().isBlank())
                    .build();
        }
    }
}
