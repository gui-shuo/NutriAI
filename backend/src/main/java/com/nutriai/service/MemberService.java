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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

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

    private static final int INVITATION_REWARD = 50;

    /**
     * 获取会员信息
     */
    @Transactional
    public MemberInfoResponse getMemberInfo(Long userId) {
        // 查找或自动创建会员记录
        Member member = memberRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("为 userId={} 自动创建会员记录", userId);
                    userRepository.findById(userId)
                            .orElseThrow(() -> BusinessException.User.userNotFound());
                    MemberLevel initial = memberLevelRepository.findByLevelOrder(1)
                            .orElseThrow(() -> new RuntimeException("未找到初始会员等级"));
                    String code = generateInvitationCode(userId);
                    Member m = Member.builder()
                            .userId(userId)
                            .levelId(initial.getId())
                            .totalGrowth(0)
                            .currentGrowth(0)
                            .invitationCode(code)
                            .invitationCount(0)
                            .isActive(true)
                            .activatedAt(LocalDateTime.now())
                            .build();
                    return memberRepository.save(m);
                });

        // 检查并自动升级
        checkAndUpgradeLevel(member);
        member = memberRepository.findById(member.getId()).orElse(member);

        MemberLevel currentLevel = memberLevelRepository.findById(member.getLevelId())
                .orElseThrow(() -> new RuntimeException("会员等级不存在"));
        MemberLevel nextLevel = memberLevelRepository
                .findByLevelOrder(currentLevel.getLevelOrder() + 1)
                .orElse(null);

        double upgradeProgress = 0.0;
        int growthToNextLevel = 0;
        if (nextLevel != null) {
            int range = nextLevel.getGrowthRequired() - currentLevel.getGrowthRequired();
            if (range > 0) {
                upgradeProgress = Math.min((member.getCurrentGrowth() * 100.0) / range, 100.0);
                growthToNextLevel = Math.max(range - member.getCurrentGrowth(), 0);
            }
        }

        long memberDays = member.getActivatedAt() != null
                ? ChronoUnit.DAYS.between(member.getActivatedAt(), LocalDateTime.now()) : 0L;

        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.User.userNotFound());

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
     * 获取成长值记录（分页）
     */
    public Page<GrowthRecordResponse> getGrowthRecords(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return growthRecordRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::buildGrowthRecordResponse);
    }

    /**
     * 生成邀请链接
     */
    public GenerateInvitationResponse generateInvitationLink(Long userId) {
        Member member = memberRepository.findByUserId(userId)
                .orElseThrow(() -> BusinessException.User.userNotFound());

        String link = baseUrl + "/register?code=" + member.getInvitationCode();
        String text = String.format(
                "邀请你加入NutriAI健康饮食助手！用我的邀请码 %s 注册，双方各得成长值奖励！",
                member.getInvitationCode()
        );

        return GenerateInvitationResponse.builder()
                .invitationCode(member.getInvitationCode())
                .invitationLink(link)
                .invitationText(text)
                .build();
    }

    /**
     * 查询邀请记录（批量查用户名，避免N+1）
     */
    public Page<InvitationResponse> getInvitationRecords(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Invitation> invitations = invitationRepository
                .findByInviterIdOrderByInvitedAtDesc(userId, pageable);

        // 收集所有被邀请人ID，批量查用户名
        Set<Long> inviteeIds = invitations.stream()
                .map(Invitation::getInviteeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> usernameMap = new HashMap<>();
        if (!inviteeIds.isEmpty()) {
            userRepository.findAllById(inviteeIds)
                    .forEach(u -> usernameMap.put(u.getId(), u.getUsername()));
        }

        return invitations.map(inv -> buildInvitationResponse(inv, usernameMap));
    }

    /**
     * 每日签到
     *
     * @return 成长值（0表示今日已签到）
     */
    @Transactional
    public int dailySignIn(Long userId) {
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime dayEnd   = dayStart.plusDays(1);

        // 检查今日是否已签到
        List<GrowthRecord> todaySignIns = growthRecordRepository
                .findByUserIdAndDateRange(userId, dayStart, dayEnd)
                .stream()
                .filter(r -> "SIGN_IN".equals(r.getGrowthType()))
                .collect(Collectors.toList());

        if (!todaySignIns.isEmpty()) {
            log.debug("userId={} 今日已签到", userId);
            return 0;
        }

        addGrowth(userId, 10, "SIGN_IN", "每日签到");
        return 10;
    }

    /**
     * 添加成长值（事务内调用）
     */
    @Transactional
    public void addGrowth(Long userId, Integer growthValue, String growthType, String description) {
        Member member = memberRepository.findByUserId(userId).orElse(null);
        if (member == null) {
            log.warn("addGrowth: 用户 {} 无会员记录，跳过", userId);
            return;
        }

        GrowthRecord record = GrowthRecord.builder()
                .memberId(member.getId())
                .userId(userId)
                .growthValue(growthValue)
                .growthType(growthType)
                .description(description)
                .build();
        growthRecordRepository.save(record);

        member.setTotalGrowth(member.getTotalGrowth() + growthValue);
        member.setCurrentGrowth(member.getCurrentGrowth() + growthValue);
        memberRepository.save(member);

        checkAndUpgradeLevel(member);
        log.debug("用户 {} 获得成长值 {}, 类型: {}", userId, growthValue, growthType);
    }

    /**
     * 检查并升级等级（内部调用）
     */
    @Transactional
    public void checkAndUpgradeLevel(Member member) {
        MemberLevel currentLevel = memberLevelRepository.findById(member.getLevelId())
                .orElseThrow(() -> new RuntimeException("会员等级不存在"));

        MemberLevel targetLevel = memberLevelRepository
                .findFirstByGrowthRequiredLessThanEqualOrderByGrowthRequiredDesc(member.getTotalGrowth())
                .orElse(currentLevel);

        if (targetLevel.getLevelOrder() > currentLevel.getLevelOrder()) {
            // 升级：currentGrowth 重置为区间内进度
            int innerProgress = member.getTotalGrowth() - targetLevel.getGrowthRequired();
            member.setLevelId(targetLevel.getId());
            member.setCurrentGrowth(Math.max(innerProgress, 0));
            memberRepository.save(member);

            log.info("用户 {} 升级到 {}（totalGrowth={}）",
                    member.getUserId(), targetLevel.getLevelName(), member.getTotalGrowth());

            // 记录升级事件
            GrowthRecord upgradeRecord = GrowthRecord.builder()
                    .memberId(member.getId())
                    .userId(member.getUserId())
                    .growthValue(0)
                    .growthType("LEVEL_UP")
                    .description("升级至" + targetLevel.getLevelName())
                    .build();
            growthRecordRepository.save(upgradeRecord);
        }
    }

    /**
     * 处理邀请注册奖励
     */
    @Transactional
    public void processInvitation(String invitationCode, Long newUserId) {
        if (invitationCode == null || invitationCode.isBlank()) return;

        Member inviter = memberRepository.findByInvitationCode(invitationCode).orElse(null);
        if (inviter == null) {
            log.warn("邀请码不存在: {}", invitationCode);
            return;
        }

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

        inviter.setInvitationCount(inviter.getInvitationCount() + 1);
        memberRepository.save(inviter);

        addGrowth(inviter.getUserId(), INVITATION_REWARD, "INVITATION", "成功邀请新用户注册");
        log.info("邀请奖励已发放: inviter={}, newUser={}", inviter.getUserId(), newUserId);
    }

    // ========== 私有辅助方法 ==========

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

    private InvitationResponse buildInvitationResponse(Invitation inv, Map<Long, String> usernameMap) {
        return InvitationResponse.builder()
                .id(inv.getId())
                .inviterId(inv.getInviterId())
                .inviteeId(inv.getInviteeId())
                .inviteeName(inv.getInviteeId() != null
                        ? usernameMap.getOrDefault(inv.getInviteeId(), "新用户") : null)
                .invitationCode(inv.getInvitationCode())
                .status(inv.getStatus())
                .statusName(getStatusName(inv.getStatus()))
                .invitedAt(inv.getInvitedAt())
                .acceptedAt(inv.getAcceptedAt())
                .rewardGrowth(inv.getRewardGrowth())
                .isRewarded(inv.getIsRewarded())
                .build();
    }

    private String getGrowthTypeName(String type) {
        Map<String, String> typeNames = Map.of(
                "SIGN_IN",       "每日签到",
                "FOOD_RECORD",   "记录饮食",
                "AI_CHAT",       "AI咨询",
                "INVITATION",    "邀请好友",
                "LEVEL_UP",      "等级提升",
                "SYSTEM_REWARD", "系统奖励"
        );
        return typeNames.getOrDefault(type, type);
    }

    private String getStatusName(String status) {
        Map<String, String> names = Map.of(
                "PENDING",  "待接受",
                "ACCEPTED", "已接受",
                "EXPIRED",  "已过期"
        );
        return names.getOrDefault(status, status);
    }

    private String generateInvitationCode(Long userId) {
        String code;
        do {
            code = String.format("INV%06d%s", userId,
                    UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase());
        } while (memberRepository.existsByInvitationCode(code));
        return code;
    }
}
