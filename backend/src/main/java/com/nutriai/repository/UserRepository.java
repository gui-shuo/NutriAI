package com.nutriai.repository;

import com.nutriai.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 用户数据访问接口
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 根据手机号查找用户
     */
    Optional<User> findByPhone(String phone);
    
    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);
    
    /**
     * 检查手机号是否存在
     */
    boolean existsByPhone(String phone);
    
    // ==================== 管理后台查询方法 ====================
    
    /**
     * 根据关键词搜索用户（用户名、邮箱、手机号）
     */
    Page<User> findByUsernameContainingOrEmailContainingOrPhoneContaining(
            String username, String email, String phone, Pageable pageable);
    
    /**
     * 根据状态查询用户
     */
    Page<User> findByStatus(String status, Pageable pageable);
    
    /**
     * 根据会员等级查询用户
     */
    Page<User> findByMemberLevel(String memberLevel, Pageable pageable);
    
    /**
     * 根据状态和会员等级查询用户
     */
    Page<User> findByStatusAndMemberLevel(String status, String memberLevel, Pageable pageable);
    
    /**
     * 统计指定时间后创建的用户数
     */
    long countByCreatedAtAfter(LocalDateTime date);
    
    /**
     * 统计指定时间范围内创建的用户数
     */
    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 统计指定时间后登录的用户数（活跃用户）
     */
    long countByLastLoginTimeAfter(LocalDateTime date);
    
    /**
     * 根据会员等级统计用户数
     */
    long countByMemberLevel(String memberLevel);
    
    /**
     * 根据微信OpenID查找用户
     */
    Optional<User> findByWxOpenId(String wxOpenId);
    
    /**
     * 根据QQ OpenID查找用户
     */
    Optional<User> findByQqOpenId(String qqOpenId);
}
