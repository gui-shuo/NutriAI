package com.nutriai.service;

import com.nutriai.entity.Coupon;
import com.nutriai.entity.UserCoupon;
import com.nutriai.exception.BusinessException;
import com.nutriai.repository.CouponRepository;
import com.nutriai.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 优惠券服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    /** 获取可用优惠券（用户领取列表） */
    public List<UserCoupon> getUserCoupons(Long userId, String status) {
        if (status == null || status.isEmpty()) {
            return userCouponRepository.findValidByUserId(userId, LocalDateTime.now());
        }
        return userCouponRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
    }

    /** 领取优惠券 */
    @Transactional
    public UserCoupon claimCoupon(Long userId, Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException("优惠券不存在"));
        if (!coupon.getIsActive()) throw new BusinessException("优惠券已失效");
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStartTime() != null && now.isBefore(coupon.getStartTime()))
            throw new BusinessException("优惠券未开始");
        if (coupon.getEndTime() != null && now.isAfter(coupon.getEndTime()))
            throw new BusinessException("优惠券已过期");
        if (coupon.getTotalCount() != -1 && coupon.getUsedCount() >= coupon.getTotalCount())
            throw new BusinessException("优惠券已被领完");

        // 检查每人限领次数
        int alreadyClaimed = userCouponRepository.countByUserIdAndCouponId(userId, couponId);
        if (coupon.getPerUserLimit() > 0 && alreadyClaimed >= coupon.getPerUserLimit())
            throw new BusinessException("每人最多领取 " + coupon.getPerUserLimit() + " 张");

        LocalDateTime expireAt = now.plusDays(coupon.getValidDays() != null ? coupon.getValidDays() : 30);

        UserCoupon uc = UserCoupon.builder()
                .userId(userId)
                .couponId(couponId)
                .coupon(coupon)
                .status("UNUSED")
                .expireAt(expireAt)
                .build();

        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);
        return userCouponRepository.save(uc);
    }

    /** 通过领取码领取 */
    @Transactional
    public UserCoupon claimByCode(Long userId, String code) {
        Coupon coupon = couponRepository.findByCodeAndIsActiveTrue(code)
                .orElseThrow(() -> new BusinessException("优惠码无效或已失效"));
        return claimCoupon(userId, coupon.getId());
    }

    /**
     * 计算优惠金额
     * @param userCouponId 用户优惠券ID，null 则不使用
     * @param originalAmount 原始金额
     * @param applicableType MEAL/PRODUCT/ALL
     */
    public BigDecimal calculateDiscount(Long userCouponId, BigDecimal originalAmount, String applicableType) {
        if (userCouponId == null) return BigDecimal.ZERO;
        UserCoupon uc = userCouponRepository.findById(userCouponId).orElse(null);
        if (uc == null || !"UNUSED".equals(uc.getStatus())) return BigDecimal.ZERO;
        if (uc.getExpireAt().isBefore(LocalDateTime.now())) return BigDecimal.ZERO;

        Coupon coupon = uc.getCoupon();
        if (!coupon.getIsActive()) return BigDecimal.ZERO;
        // 适用类型检查
        if (!"ALL".equals(coupon.getApplicableType()) && !coupon.getApplicableType().equals(applicableType))
            return BigDecimal.ZERO;
        // 满足最低消费
        if (coupon.getMinOrderAmount() != null && originalAmount.compareTo(coupon.getMinOrderAmount()) < 0)
            return BigDecimal.ZERO;

        BigDecimal discount;
        if ("REDUCE".equals(coupon.getType())) {
            discount = coupon.getDiscountValue();
        } else { // DISCOUNT: e.g. 0.8 = 80%
            discount = originalAmount.multiply(BigDecimal.ONE.subtract(coupon.getDiscountValue()));
            if (coupon.getMaxDiscountAmount() != null)
                discount = discount.min(coupon.getMaxDiscountAmount());
        }
        return discount.min(originalAmount).setScale(2, RoundingMode.HALF_UP);
    }

    /** 使用优惠券（下单时调用） */
    @Transactional
    public void useCoupon(Long userCouponId, String orderNo) {
        if (userCouponId == null) return;
        UserCoupon uc = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new BusinessException("优惠券不存在"));
        if (!"UNUSED".equals(uc.getStatus())) throw new BusinessException("优惠券已使用");
        uc.setStatus("USED");
        uc.setUsedOrderNo(orderNo);
        uc.setUsedAt(LocalDateTime.now());
        userCouponRepository.save(uc);
    }

    /** 归还优惠券（取消订单时调用） */
    @Transactional
    public void returnCoupon(Long userCouponId) {
        if (userCouponId == null) return;
        userCouponRepository.findById(userCouponId).ifPresent(uc -> {
            if ("USED".equals(uc.getStatus()) && uc.getExpireAt().isAfter(LocalDateTime.now())) {
                uc.setStatus("UNUSED");
                uc.setUsedOrderNo(null);
                uc.setUsedAt(null);
                userCouponRepository.save(uc);
            }
        });
    }

    /** 过期未使用的优惠券（定时任务调用） */
    @Transactional
    public void expireUnusedCoupons() {
        List<UserCoupon> expired = userCouponRepository.findExpiredUnused(LocalDateTime.now());
        expired.forEach(uc -> uc.setStatus("EXPIRED"));
        userCouponRepository.saveAll(expired);
        log.info("Expired {} unused coupons", expired.size());
    }

    // ---- Admin CRUD ----

    public Page<Coupon> adminList(Pageable pageable) {
        return couponRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional
    public Coupon adminCreate(Coupon coupon) {
        if (coupon.getUsedCount() == null) coupon.setUsedCount(0);
        return couponRepository.save(coupon);
    }

    @Transactional
    public Coupon adminUpdate(Long id, Coupon update) {
        Coupon existing = couponRepository.findById(id)
                .orElseThrow(() -> new BusinessException("优惠券不存在"));
        existing.setName(update.getName());
        existing.setType(update.getType());
        existing.setDiscountValue(update.getDiscountValue());
        existing.setMinOrderAmount(update.getMinOrderAmount());
        existing.setMaxDiscountAmount(update.getMaxDiscountAmount());
        existing.setApplicableType(update.getApplicableType());
        existing.setTotalCount(update.getTotalCount());
        existing.setPerUserLimit(update.getPerUserLimit());
        existing.setValidDays(update.getValidDays());
        existing.setStartTime(update.getStartTime());
        existing.setEndTime(update.getEndTime());
        existing.setDescription(update.getDescription());
        return couponRepository.save(existing);
    }

    @Transactional
    public void adminToggle(Long id, boolean active) {
        couponRepository.findById(id).ifPresent(c -> {
            c.setIsActive(active);
            couponRepository.save(c);
        });
    }

    @Transactional
    public void adminDelete(Long id) {
        couponRepository.deleteById(id);
    }

    public List<Coupon> getAvailableForType(String applicableType) {
        return couponRepository.findAvailableByType(applicableType, LocalDateTime.now());
    }
}
