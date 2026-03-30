package com.nutriai.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    
    @Column(nullable = false, length = 100)
    private String password;
    
    @Column(unique = true, length = 100)
    private String email;
    
    @Column(unique = true, length = 20)
    private String phone;
    
    @Column(length = 255)
    private String avatar;
    
    @Column(length = 50)
    private String nickname;
    
    @Builder.Default
    @Column(length = 20)
    private String role = "USER";
    
    @Builder.Default
    @Column(length = 20)
    private String status = "ACTIVE";
    
    @Builder.Default
    @Column(name = "member_level", length = 20)
    private String memberLevel = "FREE";
    
    @Builder.Default
    @Column(name = "growth_value")
    private Integer growthValue = 0;
    
    @Column(name = "member_expire_time")
    private LocalDateTime memberExpireTime;
    
    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;
    
    @Column(name = "last_login_ip", length = 50)
    private String lastLoginIp;
    
    @Column(name = "wx_open_id", unique = true, length = 100)
    private String wxOpenId;
    
    @Column(name = "qq_open_id", unique = true, length = 100)
    private String qqOpenId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
