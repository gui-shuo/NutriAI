package com.nutriai.service;

import com.nutriai.dto.member.*;
import com.nutriai.entity.*;
import com.nutriai.exception.BusinessException;
import com.nutriai.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Optional;

/**
 * 会员服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {
    
    private final MemberRepository memberRepository;
    private final MemberLevelRepository memberLevelRepository;
    private final GrowthRecordRepository growthRecordRepository;
    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    
    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;
    
    private static final int INVITATION_REWARD = 50; // 邀请奖励成长值
    
    /**
     * 获取会员信息
     */
    public MemberInfoResponse getMemberInfo(Long userId) {
        log.info("==========================================");
        log.info("开始查询会员信息");
        log.info("输入参数 userId: {}", userId);
        log.info("userId 类型: {}", userId.getClass().getName());
        
        // 查询所有会员记录用于调试
        long count = memberRepository.count();
        log.info("数据库中会员记录总数: {}", count);
        
        if (count > 0) {
            log.info("列出所有会员记录:");
            memberRepository.findAll().forEach(m -> 
                log.info("  - Member[id={}, userId={}, userId类型={}, invitationCode={}]", 
                    m.getId(), m.getUserId(), m.getUserId().getClass().getName(), m.getInvitationCode())
            );
        }
        
        log.info("执行查询: memberRepository.findByUserId({})", userId);
        Optional<Member> memberOpt = memberRepository.findByUserId(userId);
        log.info("查询结果: {}", memberOpt.isPresent() ? "找到会员" : "未找到会员");
        
        if (memberOpt.isPresent()) {
            Member found = memberOpt.get();
            log.info("找到的会员: id={}, userId={}", found.getId(), found.getUserId());
        }
        
        Member member = memberOpt
                .orElseThrow(() -> {
                    log.error("未找到userId={}的会员记录", userId);
                    return BusinessException.User.USER_NOT_FOUND;
                });
        
        // 检查并自动升级
        checkAndUpgradeLevel(member);
        // 重新加载member以获取最新状态
        member = memberRepository.findById(member.getId()).orElse(member);
        
        MemberLevel currentLevel = memberLevelRepository.findById(member.getLevelId())
                .orElseThrow(() -> new RuntimeException("会员等级不存在"));
        
        // 获取下一等级
        MemberLevel nextLevel = memberLevelRepository
                .findByLevelOrder(currentLevel.getLevelOrder() + 1)
                .orElse(null);
        
        // 计算升级进度
        Double upgradeProgress = 0.0;
        Integer growthToNextLevel = 0;
        
        if (nextLevel != null) {
            int currentLevelGrowth = currentLevel.getGrowthRequired();
            int nextLevelGrowth = nextLevel.getGrowthRequired();
            int requiredGrowth = nextLevelGrowth - currentLevelGrowth;
            
            if (requiredGrowth > 0) {
                upgradeProgress = (member.getCurrentGrowth() * 100.0) / requiredGrowth;
                growthToNextLevel = requiredGrowth - member.getCurrentGrowth();
            }
        }
        
        // 计算会员天数
        Long memberDays = member.getActivatedAt() != null ? 
                ChronoUnit.DAYS.between(member.getActivatedAt(), LocalDateTime.now()) : 0L;
        
        // 获取用户信息
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.User.USER_NOT_FOUND);
        
        return MemberInfoResponse.builder()
                .memberId(member.getId())
                .userId(userId)
                .username(user.getUsername())
                .currentLevel(buildLevelInfo(currentLevel))
                .nextLevel(nextLevel != null ? buildLevelInfo(nextLevel) : null)
                .totalGrowth(member.getTotalGrowth())
                .currentGrowth(member.getCurrentGrowth())
                .upgradeProgress(upgradeProgress)
                .growthToNextLevel(growthToNextLevel)
                .invitationCode(member.getInvitationCode())
                .invitationLink(baseUrl + "/register?code=" + member.getInvitationCode())
                .invitationCount(member.getInvitationCount())
                .isActive(member.getIsActive())
                .activatedAt(member.getActivatedAt())
                .expireAt(member.getExpireAt())
                .memberDays(memberDays)
                .build();
    }
    
    /**
     * 获取成长值记录
     */
    public Page<GrowthRecordResponse> getGrowthRecords(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<GrowthRecord> records = growthRecordRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        
        return records.map(this::buildGrowthRecordResponse);
    }
    
    /**
     * 生成邀请链接
     */
    public GenerateInvitationResponse generateInvitationLink(Long userId) {
        Member member = memberRepository.findByUserId(userId)
                .orElseThrow(() -> BusinessException.User.USER_NOT_FOUND);
        
        String invitationLink = baseUrl + "/register?code=" + member.getInvitationCode();
        String invitationText = String.format(
            "邀请你加入AI健康饮食规划助手！使用我的邀请码 %s 注册，我们都能获得成长值奖励！",
            member.getInvitationCode()
        );
        
        return GenerateInvitationResponse.builder()
                .invitationCode(member.getInvitationCode())
                .invitationLink(invitationLink)
                .invitationText(invitationText)
                .build();
    }
    
    /**
     * 查询邀请记录
     */
    public Page<InvitationResponse> getInvitationRecords(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Invitation> invitations = invitationRepository.findByInviterIdOrderByInvitedAtDesc(userId, pageable);
        
        return invitations.map(this::buildInvitationResponse);
    }
    
    /**
     * 添加成长值
     */
    @Transactional
    public void addGrowth(Long userId, Integer growthValue, String growthType, String description) {
        Member member = memberRepository.findByUserId(userId)
                .orElseThrow(() -> BusinessException.User.USER_NOT_FOUND);
        
        // 记录成长值
        GrowthRecord record = GrowthRecord.builder()
                .memberId(member.getId())
                .userId(userId)
                .growthValue(growthValue)
                .growthType(growthType)
                .description(description)
                .build();
        growthRecordRepository.save(record);
        
        // 更新会员成长值
        member.setTotalGrowth(member.getTotalGrowth() + growthValue);
        member.setCurrentGrowth(member.getCurrentGrowth() + growthValue);
        memberRepository.save(member);
        
        // 检查是否需要升级
        checkAndUpgradeLevel(member);
        
        log.info("用户 {} 获得成长值 {}, 类型: {}", userId, growthValue, growthType);
    }
    
    /**
     * 检查并升级等级
     */
    @Transactional
    public void checkAndUpgradeLevel(Member member) {
        MemberLevel currentLevel = memberLevelRepository.findById(member.getLevelId())
                .orElseThrow(() -> new RuntimeException("会员等级不存在"));
        
        // 查找应该升级到的等级
        MemberLevel targetLevel = memberLevelRepository
                .findFirstByGrowthRequiredLessThanEqualOrderByGrowthRequiredDesc(member.getTotalGrowth())
                .orElse(currentLevel);
        
        if (targetLevel.getLevelOrder() > currentLevel.getLevelOrder()) {
            // 升级
            int growthSpent = targetLevel.getGrowthRequired() - currentLevel.getGrowthRequired();
            member.setLevelId(targetLevel.getId());
            member.setCurrentGrowth(member.getCurrentGrowth() - growthSpent);
            memberRepository.save(member);
            
            log.info("用户 {} 升级到 {}", member.getUserId(), targetLevel.getLevelName());
            
            // 记录升级日志
            GrowthRecord upgradeRecord = GrowthRecord.builder()
                    .memberId(member.getId())
                    .userId(member.getUserId())
                    .growthValue(0)
                    .growthType("LEVEL_UP")
                    .description("升级到" + targetLevel.getLevelName())
                    .build();
            growthRecordRepository.save(upgradeRecord);
        }
    }
    
    /**
     * 处理邀请注册
     */
    @Transactional
    public void processInvitation(String invitationCode, Long newUserId) {
        if (invitationCode == null || invitationCode.isEmpty()) {
            return;
        }
        
        // 查找邀请人
        Member inviter = memberRepository.findByInvitationCode(invitationCode)
                .orElse(null);
        
        if (inviter == null) {
            log.warn("邀请码不存在: {}", invitationCode);
            return;
        }
        
        // 创建邀请记录
        Invitation invitation = Invitation.builder()
                .inviterId(inviter.getUserId())
                .inviteeId(newUserId)
                .invitationCode(invitationCode)
                .status("ACCEPTED")
                .acceptedAt(LocalDateTime.now())
                .rewardGrowth(INVITATION_REWARD)
                .isRewarded(true)
                .build();
        invitationRepository.save(invitation);
        
        // 增加邀请人的邀请数量
        inviter.setInvitationCount(inviter.getInvitationCount() + 1);
        memberRepository.save(inviter);
        
        // 给邀请人发放成长值奖励
        addGrowth(inviter.getUserId(), INVITATION_REWARD, "INVITATION", 
                  "成功邀请新用户注册");
        
        log.info("用户 {} 通过邀请码 {} 注册，邀请人 {} 获得奖励", 
                 newUserId, invitationCode, inviter.getUserId());
    }
    
    // 辅助方法
    
    private MemberInfoResponse.LevelInfo buildLevelInfo(MemberLevel level) {
        return MemberInfoResponse.LevelInfo.builder()
                .levelId(level.getId())
                .levelCode(level.getLevelCode())
                .levelName(level.getLevelName())
                .levelOrder(level.getLevelOrder())
                .growthRequired(level.getGrowthRequired())
                .benefits(level.getBenefits())
                .iconUrl(level.getIconUrl())
                .color(level.getColor())
                .build();
    }
    
    private GrowthRecordResponse buildGrowthRecordResponse(GrowthRecord record) {
        return GrowthRecordResponse.builder()
                .id(record.getId())
                .growthValue(record.getGrowthValue())
                .growthType(record.getGrowthType())
                .growthTypeName(getGrowthTypeName(record.getGrowthType()))
                .description(record.getDescription())
                .relatedId(record.getRelatedId())
                .createdAt(record.getCreatedAt())
                .build();
    }
    
    private InvitationResponse buildInvitationResponse(Invitation invitation) {
        String inviterName = userRepository.findById(invitation.getInviterId())
                .map(User::getUsername).orElse("未知");
        
        String inviteeName = invitation.getInviteeId() != null ?
                userRepository.findById(invitation.getInviteeId())
                        .map(User::getUsername).orElse("未知") : null;
        
        return InvitationResponse.builder()
                .id(invitation.getId())
                .inviterId(invitation.getInviterId())
                .inviterName(inviterName)
                .inviteeId(invitation.getInviteeId())
                .inviteeName(inviteeName)
                .invitationCode(invitation.getInvitationCode())
                .status(invitation.getStatus())
                .statusName(getStatusName(invitation.getStatus()))
                .invitedAt(invitation.getInvitedAt())
                .acceptedAt(invitation.getAcceptedAt())
                .rewardGrowth(invitation.getRewardGrowth())
                .isRewarded(invitation.getIsRewarded())
                .build();
    }
    
    private String getGrowthTypeName(String type) {
        Map<String, String> typeNames = Map.of(
            "SIGN_IN", "每日签到",
            "FOOD_RECORD", "记录饮食",
            "AI_CHAT", "AI咨询",
            "INVITATION", "邀请好友",
            "LEVEL_UP", "等级提升",
            "SYSTEM_REWARD", "系统奖励"
        );
        return typeNames.getOrDefault(type, type);
    }
    
    private String getStatusName(String status) {
        Map<String, String> statusNames = Map.of(
            "PENDING", "待接受",
            "ACCEPTED", "已接受",
            "EXPIRED", "已过期"
        );
        return statusNames.getOrDefault(status, status);
    }
}
