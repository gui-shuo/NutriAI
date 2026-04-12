package com.nutriai.service;

import com.nutriai.dto.admin.UserManagementDTO;
import com.nutriai.entity.Nutritionist;
import com.nutriai.entity.User;
import com.nutriai.repository.AIChatHistoryRepository;
import com.nutriai.repository.AIChatLogRepository;
import com.nutriai.repository.NutritionistRepository;
import com.nutriai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

/**
 * 管理后台用户管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {
    
    private final UserRepository userRepository;
    private final NutritionistRepository nutritionistRepository;
    private final AIChatLogRepository chatLogRepository;
    private final AIChatHistoryRepository chatHistoryRepository;
    
    /**
     * 分页查询用户列表
     */
    public Page<UserManagementDTO> getUserList(int page, int size, String keyword, String status, String memberLevel) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
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
     * 更新用户角色 (支持多角色: USER,NUTRITIONIST 或 ADMIN,NUTRITIONIST)
     * 授予营养师角色时自动创建/激活营养师档案，取消时自动停用
     */
    @Transactional
    public void updateUserRole(Long userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // Validate: ADMIN and USER cannot coexist
        if (role != null && role.contains("ADMIN") && role.contains("USER")) {
            throw new RuntimeException("管理员和普通用户角色不能同时拥有");
        }
        
        // Validate only known roles
        if (role != null) {
            for (String r : role.split(",")) {
                String trimmed = r.trim();
                if (!trimmed.equals("USER") && !trimmed.equals("ADMIN") && !trimmed.equals("NUTRITIONIST")) {
                    throw new RuntimeException("无效的角色: " + trimmed);
                }
            }
        }
        
        String oldRole = user.getRole();
        boolean wasNutritionist = oldRole != null && oldRole.contains("NUTRITIONIST");
        boolean isNutritionist = role != null && role.contains("NUTRITIONIST");
        
        user.setRole(role);
        userRepository.save(user);
        
        // 授予营养师角色：创建或激活营养师档案
        if (isNutritionist && !wasNutritionist) {
            Optional<Nutritionist> existing = nutritionistRepository.findByUserId(userId);
            if (existing.isPresent()) {
                Nutritionist n = existing.get();
                n.setIsActive(true);
                n.setApprovalStatus("APPROVED");
                nutritionistRepository.save(n);
                log.info("激活营养师档案: userId={}, nutritionistId={}", userId, n.getId());
            } else {
                Nutritionist n = Nutritionist.builder()
                        .userId(userId)
                        .name(user.getUsername())
                        .title("初级营养师")
                        .consultationFee(BigDecimal.ZERO)
                        .isActive(true)
                        .approvalStatus("APPROVED")
                        .status("OFFLINE")
                        .build();
                nutritionistRepository.save(n);
                log.info("创建营养师档案: userId={}, nutritionistId={}", userId, n.getId());
            }
        }
        
        // 取消营养师角色：停用营养师档案
        if (!isNutritionist && wasNutritionist) {
            nutritionistRepository.findByUserId(userId).ifPresent(n -> {
                n.setIsActive(false);
                n.setApprovalStatus("REJECTED");
                nutritionistRepository.save(n);
                log.info("停用营养师档案: userId={}, nutritionistId={}", userId, n.getId());
            });
        }
        
        log.info("更新用户角色: userId={}, oldRole={}, newRole={}", userId, oldRole, role);
    }
    
    /**
     * 转换为DTO
     */
    private UserManagementDTO convertToDTO(User user) {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime now = LocalDateTime.now();
        
        // 统计用户的对话数（会话数从ai_chat_history，调用数取两表较大值）
        long totalChats = chatHistoryRepository.countByUserId(user.getId());
        long logCalls = chatLogRepository.countByUserId(user.getId());
        long totalCalls = Math.max(logCalls, totalChats);
        
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
                .todayChats(totalCalls)
                .build();
    }
}
