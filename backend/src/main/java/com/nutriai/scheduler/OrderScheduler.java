package com.nutriai.scheduler;

import com.nutriai.entity.MealOrder;
import com.nutriai.entity.NutritionProduct;
import com.nutriai.entity.ProductOrder;
import com.nutriai.repository.MealOrderRepository;
import com.nutriai.repository.NutritionProductRepository;
import com.nutriai.repository.ProductOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 订单异步任务调度器
 * - 30分钟自动取消未支付的产品订单
 * - 15天自动确认收货
 * - 营养餐24小时超时自动取消
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderScheduler {

    private final ProductOrderRepository productOrderRepository;
    private final MealOrderRepository mealOrderRepository;
    private final NutritionProductRepository productRepository;

    /**
     * 每5分钟检查并自动取消超时未支付的产品订单
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    @Transactional
    public void autoCancelExpiredProductOrders() {
        List<ProductOrder> expired = productOrderRepository.findExpiredPendingOrders(LocalDateTime.now());
        if (expired.isEmpty()) return;

        log.info("[订单调度] 自动取消超时产品订单 {} 个", expired.size());
        for (ProductOrder order : expired) {
            try {
                order.setPaymentStatus("CANCELLED");
                order.setOrderStatus("CANCELLED");
                order.setCancelReason("支付超时自动取消");
                order.setCancelledAt(LocalDateTime.now());
                productOrderRepository.save(order);
                log.info("[订单调度] 产品订单自动取消: {}", order.getOrderNo());
            } catch (Exception e) {
                log.error("[订单调度] 取消产品订单失败: {}, err={}", order.getOrderNo(), e.getMessage());
            }
        }
    }

    /**
     * 每天凌晨2点检查并自动确认15天前发货的产品订单
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void autoConfirmProductOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(15);
        List<ProductOrder> shipped = productOrderRepository.findShippedBeforeCutoff(cutoff);
        if (shipped.isEmpty()) return;

        log.info("[订单调度] 15天自动确认收货产品订单 {} 个", shipped.size());
        for (ProductOrder order : shipped) {
            try {
                order.setOrderStatus("COMPLETED");
                productOrderRepository.save(order);
                log.info("[订单调度] 产品订单自动确认: {}", order.getOrderNo());
            } catch (Exception e) {
                log.error("[订单调度] 自动确认产品订单失败: {}, err={}", order.getOrderNo(), e.getMessage());
            }
        }
    }

    /**
     * 每小时检查库存预警（库存 <= 10 的产品）
     */
    @Scheduled(fixedDelay = 60 * 60 * 1000)
    public void checkLowInventory() {
        List<NutritionProduct> lowStock = productRepository.findByStockLessThanEqualAndStatusIn(
                10, List.of("ACTIVE", "ON_SALE"), PageRequest.of(0, 50)).getContent();
        if (!lowStock.isEmpty()) {
            log.warn("[库存预警] 以下产品库存低于10件: {}",
                    lowStock.stream().map(p -> p.getName() + "(" + p.getStock() + ")").toList());
        }
    }

    /**
     * 每天凌晨3点自动取消24小时内未取餐的营养餐订单（状态为PAID但未完成）
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void autoCloseStaleMealOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        // 找到超过24小时仍为PAID状态的营养餐订单
        List<MealOrder> stale = mealOrderRepository
                .findByOrderStatusOrderByCreatedAtDesc("PAID", PageRequest.of(0, 100))
                .stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isBefore(cutoff))
                .toList();

        if (!stale.isEmpty()) {
            log.info("[订单调度] 自动关闭过期营养餐订单 {} 个", stale.size());
            for (MealOrder order : stale) {
                try {
                    order.setOrderStatus("COMPLETED");
                    mealOrderRepository.save(order);
                } catch (Exception e) {
                    log.error("[订单调度] 关闭营养餐订单失败: {}", order.getOrderNo());
                }
            }
        }
    }
}
