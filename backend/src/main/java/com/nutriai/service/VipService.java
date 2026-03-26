package com.nutriai.service;

import com.alipay.api.response.AlipayTradeQueryResponse;
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
    private final AlipayService alipayService;
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
     * 创建充值订单并生成支付宝支付表单
     */
    @Transactional
    public VipOrderResponse createOrder(Long userId, Long planId) {
        VipPlan plan = vipPlanRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("套餐不存在"));

        if (!plan.getIsActive()) {
            throw new BusinessException("该套餐已下架");
        }

        // 生成唯一订单号
        String orderNo = generateOrderNo();

        // 检查是否有未支付的同套餐订单（防止重复下单）
        // 此处简化处理：直接创建新订单，旧 PENDING 订单自动超时后取消

        // 计算订单有效期（30分钟内完成支付）
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(ORDER_TIMEOUT_MINUTES);

        VipOrder order = VipOrder.builder()
                .orderNo(orderNo)
                .userId(userId)
                .planId(planId)
                .planName(plan.getPlanName())
                .amount(plan.getDiscountPrice())
                .paymentMethod("ALIPAY")
                .paymentStatus("PENDING")
                .expireTime(expireTime)
                .build();
        order = vipOrderRepository.save(order);

        // 调用支付宝生成支付表单
        String payForm = alipayService.createPagePayForm(
                orderNo,
                plan.getDiscountPrice(),
                "NutriAI " + plan.getPlanName(),
                ORDER_TIMEOUT_MINUTES
        );

        log.info("VIP订单创建成功, userId={}, orderNo={}, plan={}", userId, orderNo, plan.getPlanName());

        return VipOrderResponse.builder()
                .id(order.getId())
                .orderNo(orderNo)
                .planName(plan.getPlanName())
                .amount(plan.getDiscountPrice())
                .paymentMethod("ALIPAY")
                .paymentStatus("PENDING")
                .paymentStatusName("待支付")
                .payUrl(payForm)   // 完整 HTML form，前端嵌入页面后自动跳转支付宝
                .expireTime(expireTime)
                .createdAt(order.getCreatedAt())
                .build();
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

        // 向支付宝主动查询
        try {
            AlipayTradeQueryResponse resp = alipayService.queryTrade(orderNo);
            if (resp.isSuccess()) {
                String tradeStatus = resp.getTradeStatus();
                if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                    if (!"PAID".equals(order.getPaymentStatus())) {
                        handlePaySuccess(order, resp.getTradeNo(), null);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("查询支付宝状态异常, orderNo={}: {}", orderNo, e.getMessage());
        }

        // 重新加载
        order = vipOrderRepository.findByOrderNo(orderNo).orElse(order);
        return buildOrderResponse(order);
    }

    /**
     * 处理支付宝异步回调通知
     *
     * @param params 支付宝 POST 的所有参数（Map<String,String>）
     */
    @Transactional
    public String handleAlipayNotify(Map<String, String> params) {
        // 1. 验签
        if (!alipayService.verifyNotify(params)) {
            log.warn("支付宝回调验签失败, params={}", params);
            return "fail";
        }

        String tradeStatus = params.get("trade_status");
        String outTradeNo  = params.get("out_trade_no");
        String tradeNo     = params.get("trade_no");

        if (outTradeNo == null || tradeNo == null) {
            log.warn("支付宝回调缺少必要参数");
            return "fail";
        }

        VipOrder order = vipOrderRepository.findByOrderNo(outTradeNo).orElse(null);
        if (order == null) {
            log.warn("支付宝回调找不到订单, orderNo={}", outTradeNo);
            return "fail";
        }

        // 2. 幂等：已处理则直接返回success
        if ("PAID".equals(order.getPaymentStatus())) {
            log.info("订单已处理过，忽略重复回调, orderNo={}", outTradeNo);
            return "success";
        }

        // 3. 处理支付成功
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            // 裁剪notify_data防止超长
            String notifyData = params.toString();
            if (notifyData.length() > 2000) {
                notifyData = notifyData.substring(0, 2000);
            }
            handlePaySuccess(order, tradeNo, notifyData);
            log.info("支付宝支付成功处理完成, orderNo={}", outTradeNo);
        }

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
