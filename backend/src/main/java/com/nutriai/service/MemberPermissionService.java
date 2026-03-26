package com.nutriai.service;

import com.nutriai.entity.Member;
import com.nutriai.entity.MemberLevel;
import com.nutriai.entity.VipOrder;
import com.nutriai.repository.MemberLevelRepository;
import com.nutriai.repository.MemberRepository;
import com.nutriai.repository.VipOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 会员权限服务
 * 根据会员等级和VIP状态管理功能权限与AI配额
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberPermissionService {

    private final MemberRepository memberRepository;
    private final MemberLevelRepository memberLevelRepository;
    private final VipOrderRepository vipOrderRepository;
    private final StringRedisTemplate redisTemplate;

    @Value("${nutriai.ai-quota.free:3}")
    private int freeQuota;

    @Value("${nutriai.ai-quota.silver:10}")
    private int silverQuota;

    @Value("${nutriai.ai-quota.gold:20}")
    private int goldQuota;

    private static final String QUOTA_KEY_PREFIX = "ai:quota:daily:";

    /**
     * 获取用户的有效会员等级代码
     * 综合会员等级和VIP状态，返回最高权限
     */
    public String getEffectiveTier(Long userId) {
        // 检查VIP状态
        boolean isVip = isVipActive(userId);
        if (isVip) {
            return "GOLD"; // VIP用户享受黄金等级权限
        }

        // 检查会员等级
        Member member = memberRepository.findByUserId(userId).orElse(null);
        if (member == null) {
            return "FREE";
        }

        MemberLevel level = memberLevelRepository.findById(member.getLevelId()).orElse(null);
        if (level == null) {
            return "FREE";
        }

        String code = level.getLevelCode().toUpperCase();
        // GOLD/PLATINUM -> GOLD, SILVER -> SILVER, else FREE
        if ("GOLD".equals(code) || "PLATINUM".equals(code)) {
            return "GOLD";
        } else if ("SILVER".equals(code)) {
            return "SILVER";
        }
        return "FREE";
    }

    /**
     * 检查用户VIP是否有效
     */
    public boolean isVipActive(Long userId) {
        List<VipOrder> activeOrders = vipOrderRepository.findActiveVipOrders(userId, LocalDateTime.now());
        return !activeOrders.isEmpty();
    }

    /**
     * 获取VIP到期时间（无VIP返回null）
     */
    public LocalDateTime getVipExpireAt(Long userId) {
        List<VipOrder> activeOrders = vipOrderRepository.findActiveVipOrders(userId, LocalDateTime.now());
        if (activeOrders.isEmpty()) return null;
        return activeOrders.get(0).getVipExpireAt();
    }

    /**
     * 获取用户每日AI配额上限
     */
    public int getDailyQuota(Long userId) {
        String tier = getEffectiveTier(userId);
        return switch (tier) {
            case "GOLD" -> goldQuota;
            case "SILVER" -> silverQuota;
            default -> freeQuota;
        };
    }

    /**
     * 获取今日已使用AI次数
     */
    public int getTodayUsage(Long userId) {
        String key = QUOTA_KEY_PREFIX + LocalDate.now() + ":" + userId;
        String val = redisTemplate.opsForValue().get(key);
        return val != null ? Integer.parseInt(val) : 0;
    }

    /**
     * 检查用户是否还有AI额度
     * @return true=可以使用
     */
    public boolean checkAiQuota(Long userId) {
        int quota = getDailyQuota(userId);
        int used = getTodayUsage(userId);
        return used < quota;
    }

    /**
     * 消耗一次AI配额
     */
    public void consumeAiQuota(Long userId) {
        String key = QUOTA_KEY_PREFIX + LocalDate.now() + ":" + userId;
        Long newVal = redisTemplate.opsForValue().increment(key);
        if (newVal != null && newVal == 1) {
            // 首次设置时添加过期时间（次日零点后自动清除）
            redisTemplate.expire(key, Duration.ofHours(25));
        }
    }

    /**
     * 获取用户的权限概要（用于前端展示）
     */
    public Map<String, Object> getPermissionSummary(Long userId) {
        String tier = getEffectiveTier(userId);
        boolean isVip = isVipActive(userId);
        int quota = getDailyQuota(userId);
        int used = getTodayUsage(userId);
        LocalDateTime vipExpireAt = getVipExpireAt(userId);

        // 获取会员等级名称
        String levelName = "普通用户";
        Member member = memberRepository.findByUserId(userId).orElse(null);
        if (member != null) {
            MemberLevel level = memberLevelRepository.findById(member.getLevelId()).orElse(null);
            if (level != null) {
                levelName = level.getLevelName();
            }
        }

        return Map.of(
                "tier", tier,
                "isVip", isVip,
                "memberLevel", levelName,
                "aiQuotaTotal", quota,
                "aiQuotaUsed", used,
                "aiQuotaRemain", Math.max(quota - used, 0),
                "vipExpireAt", vipExpireAt != null ? vipExpireAt.toString() : "",
                "features", getFeaturesByTier(tier)
        );
    }

    /**
     * 根据等级获取可用功能列表
     */
    private Map<String, Boolean> getFeaturesByTier(String tier) {
        return switch (tier) {
            case "GOLD" -> Map.of(
                    "aiChat", true,
                    "foodRecognition", true,
                    "dietPlan", true,
                    "imageRecognition", true,
                    "unlimitedRecords", true
            );
            case "SILVER" -> Map.of(
                    "aiChat", true,
                    "foodRecognition", true,
                    "dietPlan", true,
                    "imageRecognition", true,
                    "unlimitedRecords", false
            );
            default -> Map.of(
                    "aiChat", true,
                    "foodRecognition", true,
                    "dietPlan", false,
                    "imageRecognition", true,
                    "unlimitedRecords", false
            );
        };
    }
}
