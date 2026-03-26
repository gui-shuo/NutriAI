package com.nutriai.service;

import com.nutriai.dto.admin.UserManagementDTO;
import com.nutriai.entity.User;
import com.nutriai.repository.AIChatLogRepository;
import com.nutriai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 管理后台用户管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {
    
    private final UserRepository userRepository;
    private final AIChatLogRepository chatLogRepository;
    
    /**
     * 分页查询用户列表
     */
    public Page<UserManagementDTO> getUserList(int page, int size, String keyword, String status, String memberLevel) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<User> userPage;
        
        // 根据条件查询
        if (keyword != null && !keyword.isEmpty()) {
            userPage = userRepository.findByUsernameContainingOrEmailContainingOrPhoneContaining(
                    keyword, keyword, keyword, pageable);
        } else if (status != null && !status.isEmpty() && memberLevel != null && !memberLevel.isEmpty()) {
            userPage = userRepository.findByStatusAndMemberLevel(status, memberLevel, pageable);
        } else if (status != null && !status.isEmpty()) {
            userPage = userRepository.findByStatus(status, pageable);
        } else if (memberLevel != null && !memberLevel.isEmpty()) {
            userPage = userRepository.findByMemberLevel(memberLevel, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }
        
        // 转换为DTO
        return userPage.map(this::convertToDTO);
    }
    
    /**
     * 获取用户详情
     */
    public UserManagementDTO getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return convertToDTO(user);
    }
    
    /**
     * 更新用户状态
     */
    @Transactional
    public void updateUserStatus(Long userId, String status) {
        log.info("开始更新用户状态: userId={}, status={}", userId, status);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        log.info("找到用户: username={}, 当前状态={}", user.getUsername(), user.getStatus());
        
        user.setStatus(status);
        userRepository.save(user);
        
        log.info("用户状态更新成功: userId={}, 新状态={}", userId, status);
    }
    
    /**
     * 更新用户会员等级
     */
    @Transactional
    public void updateUserMemberLevel(Long userId, String memberLevel) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        user.setMemberLevel(memberLevel);
        userRepository.save(user);
        
        log.info("更新用户会员等级: userId={}, memberLevel={}", userId, memberLevel);
    }
    
    /**
     * 更新用户角色
     */
    @Transactional
    public void updateUserRole(Long userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        user.setRole(role);
        userRepository.save(user);
        
        log.info("更新用户角色: userId={}, role={}", userId, role);
    }
    
    /**
     * 转换为DTO
     */
    private UserManagementDTO convertToDTO(User user) {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime now = LocalDateTime.now();
        
        // 统计用户的对话数
        long totalChats = chatLogRepository.countByUserId(user.getId());
        long todayChats = chatLogRepository.countByUserIdAndCreatedAtBetween(user.getId(), todayStart, now);
        
        return UserManagementDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .status(user.getStatus())
                .role(user.getRole())
                .memberLevel(user.getMemberLevel())
                .growthValue(user.getGrowthValue())
                .memberExpireTime(user.getMemberExpireTime())
                .lastLoginTime(user.getLastLoginTime())
                .createdAt(user.getCreatedAt())
                .totalChats(totalChats)
                .todayChats(todayChats)
                .build();
    }
}
