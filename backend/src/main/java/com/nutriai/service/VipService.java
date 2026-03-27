package com.nutriai.service;

import com.nutriai.dto.vip.*;
import com.nutriai.entity.VipOrder;
import com.nutriai.entity.VipPlan;
import com.nutriai.exception.BusinessException;
import com.nutriai.repository.VipOrderRepository;
import com.nutriai.repository.VipPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;
import java.time.format.DateTimeFormatter;

/**
 * VIP充值类型会员服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VipService {

    private final VipPlanRepository vipPlanRepository;
    private final VipOrderRepository vipOrderRepository;
    private final MemberService memberService;

    private static final int ORDER_TIMEOUT_MINUTES = 30;

    // ========== 套餐相关 ==========

    /**
     * 获取所有启用的VIP套餐列表
     */
    public List<VipPlanResponse> getActivePlans() {
        return vipPlanRepository.findByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(this::buildPlanResponse)
                .collect(Collectors.toList());
    }

    // ========== 订单相关 ==========

    /**
     * 创建充值订单并生成支付跳转URL
     */
    @Transactional
    public VipOrderResponse createOrder(Long userId, Long planId) {
        return createOrder(userId, planId, null);
    }

    /**
     * 创建充值订单（模拟支付模式：自动完成支付流程）
     * @param payType 支付方式：alipay/wxpay/qqpay，默认alipay
     */
    @Transactional
    public VipOrderResponse createOrder(Long userId, Long planId, String payType) {
        VipPlan plan = vipPlanRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("套餐不存在"));

        if (!plan.getIsActive()) {
            throw new BusinessException("该套餐已下架");
        }

        // 生成唯一订单号
        String orderNo = generateOrderNo();

        // 计算订单有效期（30分钟内完成支付）
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(ORDER_TIMEOUT_MINUTES);

        String method = resolvePaymentMethod(payType);

        VipOrder order = VipOrder.builder()
                .orderNo(orderNo)
                .userId(userId)
                .planId(planId)
                .planName(plan.getPlanName())
                .amount(plan.getDiscountPrice())
                .paymentMethod(method)
                .paymentStatus("PENDING")
                .expireTime(expireTime)
                .build();
        order = vipOrderRepository.save(order);

        log.info("VIP订单创建成功(模拟支付), userId={}, orderNo={}, plan={}, payType={}", userId, orderNo, plan.getPlanName(), method);

        return VipOrderResponse.builder()
                .id(order.getId())
                .orderNo(orderNo)
                .planName(plan.getPlanName())
                .amount(plan.getDiscountPrice())
                .paymentMethod(method)
                .paymentStatus("PENDING")
                .paymentStatusName("待支付")
                .expireTime(expireTime)
                .createdAt(order.getCreatedAt())
                .build();
    }

    /**
     * 模拟支付确认（前端调用，模拟用户完成支付）
     */
    @Transactional
    public VipOrderResponse simulatePayment(Long userId, String orderNo) {
        VipOrder order = vipOrderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException("订单不存在"));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }

        if ("PAID".equals(order.getPaymentStatus())) {
            return buildOrderResponse(order);
        }

        if (isTerminalStatus(order.getPaymentStatus())) {
            throw new BusinessException("订单已关闭，无法支付");
        }

        // 模拟支付成功
        String simulatedTradeNo = "SIM" + System.currentTimeMillis();
        handlePaySuccess(order, simulatedTradeNo, "模拟支付成功");

        order = vipOrderRepository.findByOrderNo(orderNo).orElse(order);
        log.info("模拟支付成功, userId={}, orderNo={}", userId, orderNo);
        return buildOrderResponse(order);
    }

    /**
     * 模拟退款（前端调用，模拟订单退款）
     */
    @Transactional
    public VipOrderResponse simulateRefund(Long userId, String orderNo) {
        VipOrder order = vipOrderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException("订单不存在"));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }

        if (!"PAID".equals(order.getPaymentStatus())) {
            throw new BusinessException("仅已支付的订单可以退款");
        }

        order.setPaymentStatus("REFUNDED");
        order.setRemark("模拟退款 - " + LocalDateTime.now());
        vipOrderRepository.save(order);

        log.info("模拟退款成功, userId={}, orderNo={}", userId, orderNo);
        return buildOrderResponse(order);
    }

    /**
     * 主动查询订单支付状态（前端轮询调用）
     */
    @Transactional
    public VipOrderResponse queryOrderStatus(Long userId, String orderNo) {
        VipOrder order = vipOrderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException("订单不存在"));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权查询该订单");
        }

        // 如果已是终态，直接返回
        if (isTerminalStatus(order.getPaymentStatus())) {
            return buildOrderResponse(order);
        }

        // 检查是否超时
        if (order.getExpireTime().isBefore(LocalDateTime.now())) {
            order.setPaymentStatus("EXPIRED");
            vipOrderRepository.save(order);
            return buildOrderResponse(order);
        }

        // 重新加载
        order = vipOrderRepository.findByOrderNo(orderNo).orElse(order);
        return buildOrderResponse(order);
    }

    /**
     * 处理易支付异步回调通知（已弃用，使用模拟支付）
     */
    @Transactional
    public String handleEPayNotify(Map<String, String> params) {
        log.info("易支付回调已弃用（模拟支付模式），忽略通知");
        return "success";
    }

    /**
     * 获取当前用户VIP状态
     */
    public VipStatusResponse getVipStatus(Long userId) {
        List<VipOrder> activeOrders = vipOrderRepository.findActiveVipOrders(userId, LocalDateTime.now());

        if (activeOrders.isEmpty()) {
            return VipStatusResponse.builder()
                    .isVip(false)
                    .remainDays(0L)
                    .build();
        }

        VipOrder latestOrder = activeOrders.get(0);
        long remainDays = ChronoUnit.DAYS.between(LocalDateTime.now(), latestOrder.getVipExpireAt());

        return VipStatusResponse.builder()
                .isVip(true)
                .planName(latestOrder.getPlanName())
                .vipExpireAt(latestOrder.getVipExpireAt())
                .remainDays(Math.max(remainDays, 0L))
                .build();
    }

    /**
     * 获取用户订单历史
     */
    public Page<VipOrderResponse> getOrderHistory(Long userId, int page, int size) {
        return vipOrderRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size))
                .map(this::buildOrderResponse);
    }

    // ========== 定时任务：清理超时未支付订单 ==========

    @Scheduled(fixedDelay = 60_000) // 每1分钟扫描一次
    public void cancelExpiredOrders() {
        List<VipOrder> expiredOrders = vipOrderRepository.findExpiredPendingOrders(LocalDateTime.now());
        if (!expiredOrders.isEmpty()) {
            expiredOrders.forEach(o -> o.setPaymentStatus("EXPIRED"));
            vipOrderRepository.saveAll(expiredOrders);
            log.info("自动取消超时VIP订单 {} 笔", expiredOrders.size());
        }
    }

    // ========== 私有方法 ==========

    /**
     * 支付成功核心处理（事务内）
     */
    private void handlePaySuccess(VipOrder order, String tradeNo, String notifyData) {
        VipPlan plan = vipPlanRepository.findById(order.getPlanId())
                .orElseThrow(() -> new RuntimeException("套餐不存在, planId=" + order.getPlanId()));

        // 计算VIP到期时间（若用户已是VIP则从当前到期时间顺延）
        List<VipOrder> activeOrders = vipOrderRepository.findActiveVipOrders(order.getUserId(), LocalDateTime.now());
        LocalDateTime vipExpireAt;
        if (!activeOrders.isEmpty()) {
            // 叠加续期
            vipExpireAt = activeOrders.get(0).getVipExpireAt().plusDays(plan.getDurationDays());
        } else {
            vipExpireAt = LocalDateTime.now().plusDays(plan.getDurationDays());
        }

        order.setPaymentStatus("PAID");
        order.setTradeNo(tradeNo);
        order.setPaidAt(LocalDateTime.now());
        order.setVipExpireAt(vipExpireAt);
        if (notifyData != null) {
            order.setNotifyData(notifyData);
        }
        vipOrderRepository.save(order);

        // 赠送成长值（幂等）
        if (!order.getBonusGrowthGranted() && plan.getBonusGrowth() > 0) {
            memberService.addGrowth(
                    order.getUserId(),
                    plan.getBonusGrowth(),
                    "SYSTEM_REWARD",
                    "购买" + plan.getPlanName() + "赠送成长值"
            );
            order.setBonusGrowthGranted(true);
            vipOrderRepository.save(order);
        }

        log.info("VIP支付成功: userId={}, plan={}, vipExpireAt={}", 
                 order.getUserId(), plan.getPlanName(), vipExpireAt);
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "VIP" + timestamp + random;
    }

    /**
     * 将前端传入的支付类型映射为存储的支付方式名称
     */
    private String resolvePaymentMethod(String payType) {
        if (payType == null || payType.isBlank()) return "EPAY_ALIPAY";
        return switch (payType.toLowerCase()) {
            case "wxpay" -> "EPAY_WXPAY";
            case "qqpay" -> "EPAY_QQPAY";
            default -> "EPAY_ALIPAY";
        };
    }

    private boolean isTerminalStatus(String status) {
        return "PAID".equals(status) || "REFUNDED".equals(status) 
            || "CANCELLED".equals(status) || "EXPIRED".equals(status);
    }

    private VipPlanResponse buildPlanResponse(VipPlan plan) {
        BigDecimal pricePerDay = BigDecimal.ZERO;
        if (plan.getDurationDays() > 0) {
            pricePerDay = plan.getDiscountPrice()
                    .divide(BigDecimal.valueOf(plan.getDurationDays()), 2, RoundingMode.HALF_UP);
        }

        String discountLabel = null;
        if (plan.getOriginalPrice().compareTo(BigDecimal.ZERO) > 0
                && plan.getDiscountPrice().compareTo(plan.getOriginalPrice()) < 0) {
            BigDecimal ratio = plan.getDiscountPrice()
                    .multiply(BigDecimal.TEN)
                    .divide(plan.getOriginalPrice(), 1, RoundingMode.HALF_UP);
            discountLabel = ratio + "折";
        }

        return VipPlanResponse.builder()
                .id(plan.getId())
                .planCode(plan.getPlanCode())
                .planName(plan.getPlanName())
                .description(plan.getDescription())
                .originalPrice(plan.getOriginalPrice())
                .discountPrice(plan.getDiscountPrice())
                .durationDays(plan.getDurationDays())
                .benefits(plan.getBenefits())
                .bonusGrowth(plan.getBonusGrowth())
                .badge(plan.getBadge())
                .discountLabel(discountLabel)
                .pricePerDay(pricePerDay)
                .build();
    }

    private VipOrderResponse buildOrderResponse(VipOrder order) {
        Map<String, String> statusNames = Map.of(
                "PENDING",   "待支付",
                "PAID",      "支付成功",
                "FAILED",    "支付失败",
                "REFUNDED",  "已退款",
                "CANCELLED", "已取消",
                "EXPIRED",   "已超时"
        );

        return VipOrderResponse.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .planName(order.getPlanName())
                .amount(order.getAmount())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .paymentStatusName(statusNames.getOrDefault(order.getPaymentStatus(), order.getPaymentStatus()))
                .expireTime(order.getExpireTime())
                .vipExpireAt(order.getVipExpireAt())
                .paidAt(order.getPaidAt())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
