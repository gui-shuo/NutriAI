package com.nutriai.service;

import com.nutriai.dto.user.*;
import com.nutriai.entity.User;
import com.nutriai.exception.BusinessException;
import com.nutriai.repository.*;
import com.nutriai.service.sms.SmsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SmsProvider smsProvider;
    private final EmailService emailService;
    
    // 注销相关的仓库依赖
    private final AIChatFavoriteRepository aiChatFavoriteRepository;
    private final AIChatHistoryRepository aiChatHistoryRepository;
    private final AIChatLogRepository aiChatLogRepository;
    private final CommunityCommentRepository communityCommentRepository;
    private final CommunityLikeRepository communityLikeRepository;
    private final CommunityPostRepository communityPostRepository;
    private final ConsultationOrderRepository consultationOrderRepository;
    private final DietPlanHistoryRepository dietPlanHistoryRepository;
    private final DietPlanTaskRepository dietPlanTaskRepository;
    private final FeedbackRepository feedbackRepository;
    private final FoodRecognitionHistoryRepository foodRecognitionHistoryRepository;
    private final FoodRecordRepository foodRecordRepository;
    private final GrowthRecordRepository growthRecordRepository;
    private final HealthProfileRepository healthProfileRepository;
    private final MemberRepository memberRepository;
    private final NutritionistRepository nutritionistRepository;
    private final ProductOrderRepository productOrderRepository;
    private final UserAddressRepository userAddressRepository;
    private final UserFollowRepository userFollowRepository;
    private final InvitationRepository invitationRepository;
    private final VipOrderRepository vipOrderRepository;

    private static final String SMS_CODE_PREFIX = "sms:code:";
    private static final int SMS_CODE_EXPIRE_MINUTES = 5;
    private static final int SMS_CODE_LENGTH = 6;

    private static final String PHONE_CHANGE_EMAIL_CODE_PREFIX = "phone_change:email:code:";
    private static final int PHONE_CHANGE_CODE_EXPIRE_MINUTES = 5;
    
    /**
     * 获取用户资料
     */
    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.User.userNotFound());
        
        return UserProfileResponse.fromEntity(user);
    }
    
    /**
     * 更新用户资料
     */
    @Transactional
    public UserProfileResponse updateUserProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.User.userNotFound());
        
        // 更新邮箱
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            // 检查邮箱是否已被使用
            if (userRepository.existsByEmail(request.getEmail()) && 
                !request.getEmail().equals(user.getEmail())) {
                throw BusinessException.User.emailAlreadyExists();
            }
            user.setEmail(request.getEmail());
        }
        
        // 更新昵称
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        
        // 更新头像
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        
        user = userRepository.save(user);
        log.info("用户资料更新成功: userId={}", userId);
        
        return UserProfileResponse.fromEntity(user);
    }
    
    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        // 验证两次密码是否一致
        if (!request.isPasswordMatch()) {
            throw BusinessException.Auth.passwordMismatch();
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.User.userNotFound());
        
        // 验证旧密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw BusinessException.User.oldPasswordIncorrect();
        }
        
        // 检查新密码不能与旧密码相同
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw BusinessException.User.newPasswordSameAsOld();
        }
        
        // 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        log.info("用户密码修改成功: userId={}", userId);
    }
    
    /**
     * 发送短信验证码
     */
    public SmsCodeResponse sendSmsCode(String phone) {
        // 检查手机号是否已被注册
        if (userRepository.existsByPhone(phone)) {
            throw BusinessException.User.phoneAlreadyExists();
        }
        
        // 检查发送频率（防刷：同一手机号60秒内不能重复发送）
        String rateLimitKey = "sms:rate:" + phone;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(rateLimitKey))) {
            throw new BusinessException(40111, "验证码发送过于频繁，请60秒后再试");
        }
        
        // 生成6位数字验证码
        String code = generateSmsCode();
        String key = UUID.randomUUID().toString();
        
        // 存储到Redis
        String redisKey = SMS_CODE_PREFIX + key;
        redisTemplate.opsForValue().set(
                redisKey, 
                code, 
                SMS_CODE_EXPIRE_MINUTES, 
                TimeUnit.MINUTES
        );
        
        // 设置发送频率限制（60秒）
        redisTemplate.opsForValue().set(rateLimitKey, "1", 60, TimeUnit.SECONDS);
        
        // 调用短信提供者发送验证码
        boolean sent = smsProvider.sendVerificationCode(phone, code);
        if (!sent) {
            // 发送失败，清除Redis中的验证码
            redisTemplate.delete(redisKey);
            throw new BusinessException(40112, "验证码发送失败，请稍后重试");
        }
        
        log.info("短信验证码发送成功: phone={}, provider={}, key={}", phone, smsProvider.getProviderName(), key);
        
        return SmsCodeResponse.builder()
                .smsKey(key)
                .expiresIn((long) SMS_CODE_EXPIRE_MINUTES * 60)
                .message("验证码已发送，5分钟内有效")
                .build();
    }
    
    /**
     * 发送邮箱验证码（用于修改手机号）
     */
    public void sendEmailCodeForPhoneChange(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.User.userNotFound());

        String email = user.getEmail();
        if (email == null || email.isBlank()) {
            throw new BusinessException(40113, "该账号未绑定邮箱，请先绑定邮箱后再修改手机号");
        }

        // 防刷限制：60秒内不重复发送
        String rateLimitKey = "phone_change:email:rate:" + userId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(rateLimitKey))) {
            throw new BusinessException(40111, "验证码发送过于频繁，请60秒后再试");
        }

        String code = generateSmsCode();
        String redisKey = PHONE_CHANGE_EMAIL_CODE_PREFIX + userId;
        redisTemplate.opsForValue().set(redisKey, code, PHONE_CHANGE_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(rateLimitKey, "1", 60, TimeUnit.SECONDS);

        emailService.sendPhoneChangeCode(email, user.getUsername(), code);
        log.info("手机号修改邮箱验证码已发送: userId={}, email={}", userId, email);
    }

    /**
     * 修改手机号（支持邮箱验证码和短信验证码）
     */
    @Transactional
    public void changePhone(Long userId, ChangePhoneRequest request) {
        // 优先使用邮箱验证码（新版），否则回退短信验证码（旧版兼容）
        if (request.getEmailCode() != null && !request.getEmailCode().isBlank()) {
            String redisKey = PHONE_CHANGE_EMAIL_CODE_PREFIX + userId;
            String storedCode = (String) redisTemplate.opsForValue().get(redisKey);
            if (storedCode == null) {
                throw new BusinessException(40114, "验证码已过期，请重新获取");
            }
            if (!storedCode.equalsIgnoreCase(request.getEmailCode())) {
                throw new BusinessException(40115, "验证码错误，请重新输入");
            }
            redisTemplate.delete(redisKey);
        } else if (request.getSmsKey() != null && request.getSmsCode() != null) {
            validateSmsCode(request.getSmsKey(), request.getSmsCode());
        } else {
            throw new BusinessException("请提供验证码");
        }

        // 检查新手机号是否已被使用
        if (userRepository.existsByPhone(request.getNewPhone())) {
            throw BusinessException.User.phoneAlreadyExists();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.User.userNotFound());

        user.setPhone(request.getNewPhone());
        userRepository.save(user);

        log.info("用户手机号修改成功: userId={}, newPhone={}", userId, request.getNewPhone());
    }

    /**
     * 验证短信验证码（保留，供其他场景使用）
     */
    private void validateSmsCode(String key, String code) {
        if (key == null || code == null) {
            throw BusinessException.User.smsCodeInvalid();
        }

        String redisKey = SMS_CODE_PREFIX + key;
        String storedCode = (String) redisTemplate.opsForValue().get(redisKey);

        if (storedCode == null) {
            throw BusinessException.User.smsCodeExpired();
        }

        if (!storedCode.equalsIgnoreCase(code)) {
            throw BusinessException.User.smsCodeInvalid();
        }
    }

    /**
     * 生成验证码（使用安全随机数）
     */
    private String generateSmsCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < SMS_CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    /**
     * 注销账号 — 验证密码后级联删除所有用户数据
     */
    @Transactional
    public void deleteAccount(Long userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(BusinessException.Auth::userNotFound);

        // 管理员账号不允许自行注销
        if ("ADMIN".equals(user.getRole()) || "SUPER_ADMIN".equals(user.getRole())) {
            throw new BusinessException(403, "管理员账号不允许通过此方式注销");
        }

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(400, "密码验证失败，请输入正确的登录密码");
        }

        log.warn("用户请求注销账号: userId={}, email={}", userId, user.getEmail());

        // 级联删除所有关联数据
        aiChatFavoriteRepository.deleteByUserId(userId);
        aiChatHistoryRepository.deleteByUserId(userId);
        aiChatLogRepository.deleteAllByUserId(userId);
        communityCommentRepository.deleteAllByUserId(userId);
        communityLikeRepository.deleteAllByUserId(userId);
        communityPostRepository.deleteAllByUserId(userId);
        consultationOrderRepository.deleteAllByUserId(userId);
        dietPlanHistoryRepository.deleteAllByUserId(userId);
        dietPlanTaskRepository.deleteAllByUserId(userId);
        feedbackRepository.deleteAllByUserId(userId);
        foodRecognitionHistoryRepository.deleteAllByUserId(userId);
        foodRecordRepository.deleteByUserId(userId);
        growthRecordRepository.deleteAllByUserId(userId);
        healthProfileRepository.deleteByUserId(userId);
        memberRepository.deleteByUserId(userId);
        nutritionistRepository.deleteByUserId(userId);
        productOrderRepository.deleteAllByUserId(userId);
        userAddressRepository.deleteAllByUserId(userId);
        userFollowRepository.deleteAllByFollowerIdOrFollowingId(userId, userId);
        invitationRepository.deleteAllByInviterIdOrInviteeId(userId, userId);
        vipOrderRepository.deleteAllByUserId(userId);

        // 最后删除用户本身
        userRepository.delete(user);

        log.warn("用户账号已注销: userId={}, email={}", userId, user.getEmail());
    }
}
