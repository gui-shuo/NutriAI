package com.nutriai.service;

import com.nutriai.entity.MealItem;
import com.nutriai.entity.MealOrder;
import com.nutriai.exception.BusinessException;
import com.nutriai.repository.MealItemRepository;
import com.nutriai.repository.MealOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 营养餐服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MealService {

    private final MealItemRepository mealItemRepository;
    private final MealOrderRepository mealOrderRepository;

    /**
     * 获取可用餐品列表（分页，可按分类/餐类筛选）
     */
    public Page<MealItem> getAvailableMeals(String category, String mealType, int page, int size) {
        if (category != null && !category.isBlank() && !"ALL".equals(category)) {
            return mealItemRepository.findByCategoryAndIsAvailableTrueOrderBySortOrderAsc(category, PageRequest.of(page, size));
        }
        if (mealType != null && !mealType.isBlank() && !"ALL".equals(mealType)) {
            return mealItemRepository.findByMealTypeAndIsAvailableTrueOrderBySortOrderAsc(mealType, PageRequest.of(page, size));
        }
        return mealItemRepository.findByIsAvailableTrueOrderBySortOrderAsc(PageRequest.of(page, size));
    }

    /**
     * 搜索餐品
     */
    public Page<MealItem> searchMeals(String keyword, int page, int size) {
        return mealItemRepository.searchByKeyword(keyword, PageRequest.of(page, size));
    }

    /**
     * 获取推荐餐品
     */
    public List<MealItem> getRecommendedMeals() {
        return mealItemRepository.findByIsRecommendedTrueAndIsAvailableTrueOrderBySortOrderAsc();
    }

    /**
     * 获取餐品详情
     */
    public MealItem getMealDetail(Long id) {
        return mealItemRepository.findById(id)
                .orElseThrow(() -> new BusinessException("餐品不存在"));
    }

    /**
     * 获取所有可用分类
     */
    public List<String> getCategories() {
        return mealItemRepository.findDistinctCategories();
    }

    /**
     * 创建营养餐订单
     */
    @Transactional
    public MealOrder createOrder(Long userId, List<Map<String, Object>> items,
                                  String fulfillmentType, String pickupTime, String pickupLocation,
                                  String receiverName, String receiverPhone,
                                  String receiverAddress, String remark,
                                  Long merchantId, String merchantName) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("订单商品不能为空");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQuantity = 0;
        List<Map<String, Object>> orderItems = new ArrayList<>();

        for (Map<String, Object> item : items) {
            Object idObj = item.get("mealItemId") != null ? item.get("mealItemId") : item.get("mealId");
            if (idObj == null) {
                throw new BusinessException("订单商品缺少餐品ID");
            }
            Long mealItemId = Long.valueOf(idObj.toString());
            Object qtyObj = item.get("quantity");
            int quantity = qtyObj != null ? Integer.parseInt(qtyObj.toString()) : 1;

            MealItem mealItem = mealItemRepository.findById(mealItemId)
                    .orElseThrow(() -> new BusinessException("餐品不存在: " + mealItemId));

            if (!Boolean.TRUE.equals(mealItem.getIsAvailable())) {
                throw new BusinessException("餐品已下架: " + mealItem.getName());
            }

            if (mealItem.getStock() < quantity) {
                throw new BusinessException("库存不足: " + mealItem.getName());
            }

            BigDecimal itemTotal = mealItem.getSalePrice().multiply(BigDecimal.valueOf(quantity));
            totalAmount = totalAmount.add(itemTotal);
            totalQuantity += quantity;

            Map<String, Object> orderItem = new HashMap<>();
            orderItem.put("mealItemId", mealItemId);
            orderItem.put("name", mealItem.getName());
            orderItem.put("imageUrl", mealItem.getImageUrl());
            orderItem.put("price", mealItem.getSalePrice());
            orderItem.put("quantity", quantity);
            orderItem.put("subtotal", itemTotal);
            orderItems.add(orderItem);
        }

        String orderNo = generateOrderNo("MEAL");

        MealOrder order = MealOrder.builder()
                .orderNo(orderNo)
                .userId(userId)
                .merchantId(merchantId)
                .merchantName(merchantName)
                .items(orderItems)
                .totalQuantity(totalQuantity)
                .totalAmount(totalAmount)
                .fulfillmentType(fulfillmentType != null ? fulfillmentType : "PICKUP")
                .pickupTime(pickupTime)
                .pickupLocation(pickupLocation != null ? pickupLocation : "营养工作室")
                .receiverName(receiverName)
                .receiverPhone(receiverPhone)
                .receiverAddress(receiverAddress)
                .remark(remark)
                .paymentStatus("PENDING")
                .orderStatus("PENDING_PAYMENT")
                .build();

        order = mealOrderRepository.save(order);
        log.info("营养餐订单创建成功, userId={}, orderNo={}, amount={}", userId, orderNo, totalAmount);
        return order;
    }

    /**
     * 模拟支付（标记为已支付+备餐中）
     */
    @Transactional
    public MealOrder simulatePayment(Long userId, String orderNo) {
        MealOrder order = mealOrderRepository.findByOrderNoAndUserId(orderNo, userId)
                .orElseThrow(() -> new BusinessException("订单不存在"));

        if ("PAID".equals(order.getPaymentStatus())) {
            return order;
        }

        if (!"PENDING".equals(order.getPaymentStatus())) {
            throw new BusinessException("订单状态不允许支付");
        }

        // 扣减库存、增加销量
        for (Map<String, Object> item : order.getItems()) {
            Long mealItemId = Long.valueOf(item.get("mealItemId").toString());
            int quantity = Integer.parseInt(item.get("quantity").toString());
            MealItem mealItem = mealItemRepository.findById(mealItemId).orElse(null);
            if (mealItem != null) {
                mealItem.setStock(Math.max(0, mealItem.getStock() - quantity));
                mealItem.setSalesCount(mealItem.getSalesCount() + quantity);
                mealItemRepository.save(mealItem);
            }
        }

        order.setPaymentStatus("PAID");
        order.setOrderStatus("PREPARING");
        order.setPaymentMethod("SIMULATE");
        order.setPaidAt(LocalDateTime.now());

        // Generate 6-digit pickup code
        String pickupCode = generatePickupCode();
        order.setPickupCode(pickupCode);

        mealOrderRepository.save(order);

        log.info("营养餐订单支付成功(模拟), userId={}, orderNo={}, pickupCode={}", userId, orderNo, pickupCode);
        return order;
    }

    /**
     * 更新订单状态（管理员操作: PREPARING → READY → PICKED_UP/DELIVERED → COMPLETED）
     */
    @Transactional
    public MealOrder updateOrderStatus(String orderNo, String newStatus) {
        MealOrder order = mealOrderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException("订单不存在"));

        String currentStatus = order.getOrderStatus();

        // 验证状态流转合法性
        boolean valid = switch (newStatus) {
            case "PREPARING" -> "PAID".equals(currentStatus);
            case "READY" -> "PREPARING".equals(currentStatus);
            case "PICKED_UP" -> "READY".equals(currentStatus) && "PICKUP".equals(order.getFulfillmentType());
            case "DELIVERED" -> "READY".equals(currentStatus) && "DELIVERY".equals(order.getFulfillmentType());
            case "COMPLETED" -> "PICKED_UP".equals(currentStatus) || "DELIVERED".equals(currentStatus);
            default -> false;
        };

        if (!valid) {
            throw new BusinessException("订单状态不允许从 " + currentStatus + " 变更为 " + newStatus);
        }

        order.setOrderStatus(newStatus);

        if ("READY".equals(newStatus)) {
            order.setReadyAt(LocalDateTime.now());
        } else if ("COMPLETED".equals(newStatus)) {
            order.setCompletedAt(LocalDateTime.now());
        }

        mealOrderRepository.save(order);
        log.info("营养餐订单状态更新, orderNo={}, {} -> {}", orderNo, currentStatus, newStatus);
        return order;
    }

    /**
     * 取消订单
     */
    @Transactional
    public MealOrder cancelOrder(Long userId, String orderNo, String reason) {
        MealOrder order = mealOrderRepository.findByOrderNoAndUserId(orderNo, userId)
                .orElseThrow(() -> new BusinessException("订单不存在"));

        if ("COMPLETED".equals(order.getOrderStatus()) || "CANCELLED".equals(order.getOrderStatus())) {
            throw new BusinessException("订单状态不允许取消");
        }

        // 如果已支付，恢复库存
        if ("PAID".equals(order.getPaymentStatus())) {
            for (Map<String, Object> item : order.getItems()) {
                Long mealItemId = Long.valueOf(item.get("mealItemId").toString());
                int quantity = Integer.parseInt(item.get("quantity").toString());
                MealItem mealItem = mealItemRepository.findById(mealItemId).orElse(null);
                if (mealItem != null) {
                    mealItem.setStock(mealItem.getStock() + quantity);
                    mealItem.setSalesCount(Math.max(0, mealItem.getSalesCount() - quantity));
                    mealItemRepository.save(mealItem);
                }
            }
            order.setPaymentStatus("REFUNDED");
        }

        order.setOrderStatus("CANCELLED");
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelReason(reason);
        mealOrderRepository.save(order);

        log.info("营养餐订单已取消, userId={}, orderNo={}, reason={}", userId, orderNo, reason);
        return order;
    }

    /**
     * 获取用户订单历史
     */
    public Page<MealOrder> getOrderHistory(Long userId, int page, int size) {
        return mealOrderRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    /**
     * 获取订单详情
     */
    public MealOrder getOrderDetail(Long userId, String orderNo) {
        return mealOrderRepository.findByOrderNoAndUserId(orderNo, userId)
                .orElseThrow(() -> new BusinessException("订单不存在"));
    }

    /**
     * 管理员：获取所有订单
     */
    public Page<MealOrder> getAllOrders(String status, int page, int size) {
        if (status != null && !status.isBlank() && !"ALL".equals(status)) {
            return mealOrderRepository.findByOrderStatusOrderByCreatedAtDesc(status, PageRequest.of(page, size));
        }
        return mealOrderRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }

    /**
     * 管理员：订单统计
     */
    public Map<String, Object> getOrderStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", mealOrderRepository.count());
        stats.put("pendingPayment", mealOrderRepository.countByOrderStatus("PENDING_PAYMENT"));
        stats.put("preparing", mealOrderRepository.countByOrderStatus("PREPARING"));
        stats.put("ready", mealOrderRepository.countByOrderStatus("READY"));
        stats.put("completed", mealOrderRepository.countByOrderStatus("COMPLETED"));
        stats.put("cancelled", mealOrderRepository.countByOrderStatus("CANCELLED"));
        return stats;
    }

    /**
     * 管理员：通过取餐码核验订单
     */
    @Transactional
    public MealOrder verifyPickupCode(String pickupCode) {
        MealOrder order = mealOrderRepository.findByPickupCode(pickupCode)
                .orElseThrow(() -> new BusinessException("取餐码无效"));

        if (!"READY".equals(order.getOrderStatus()) && !"PREPARING".equals(order.getOrderStatus())) {
            if ("PICKED_UP".equals(order.getOrderStatus()) || "COMPLETED".equals(order.getOrderStatus())) {
                throw new BusinessException("此订单已完成取餐");
            }
            throw new BusinessException("订单状态不允许核验: " + order.getOrderStatus());
        }

        order.setOrderStatus("PICKED_UP");
        order.setPickupCodeVerifiedAt(LocalDateTime.now());
        order.setCompletedAt(LocalDateTime.now());
        order.setOrderStatus("COMPLETED");
        mealOrderRepository.save(order);

        log.info("取餐码核验成功, orderNo={}, pickupCode={}", order.getOrderNo(), pickupCode);
        return order;
    }

    private String generatePickupCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        // Ensure uniqueness among active orders
        String codeStr = String.valueOf(code);
        while (mealOrderRepository.findByPickupCode(codeStr).filter(
                o -> !"COMPLETED".equals(o.getOrderStatus()) && !"CANCELLED".equals(o.getOrderStatus())
        ).isPresent()) {
            code = 100000 + random.nextInt(900000);
            codeStr = String.valueOf(code);
        }
        return codeStr;
    }

    private String generateOrderNo(String prefix) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return prefix + timestamp + random;
    }

    // ==================== 商家端方法 ====================

    /**
     * 获取商家的订单列表
     */
    public Page<MealOrder> getMerchantOrders(Long merchantId, String status, int page, int size) {
        if (status != null && !status.isBlank() && !"ALL".equals(status)) {
            return mealOrderRepository.findByMerchantIdAndOrderStatusOrderByCreatedAtDesc(
                    merchantId, status, PageRequest.of(page, size));
        }
        return mealOrderRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId, PageRequest.of(page, size));
    }

    /**
     * 获取商家订单详情
     */
    public MealOrder getMerchantOrderDetail(Long merchantId, String orderNo) {
        return mealOrderRepository.findByOrderNoAndMerchantId(orderNo, merchantId)
                .orElseThrow(() -> new BusinessException("订单不存在"));
    }

    /**
     * 商家更新订单状态
     */
    @Transactional
    public MealOrder merchantUpdateOrderStatus(Long merchantId, String orderNo, String newStatus) {
        MealOrder order = mealOrderRepository.findByOrderNoAndMerchantId(orderNo, merchantId)
                .orElseThrow(() -> new BusinessException("订单不存在"));
        return updateOrderStatus(order.getOrderNo(), newStatus);
    }

    /**
     * 商家核验取餐码
     */
    @Transactional
    public MealOrder merchantVerifyPickup(Long merchantId, String pickupCode) {
        MealOrder order = mealOrderRepository.findByPickupCodeAndMerchantId(pickupCode, merchantId)
                .orElseThrow(() -> new BusinessException("取餐码无效或不属于本店"));

        if (!"READY".equals(order.getOrderStatus()) && !"PREPARING".equals(order.getOrderStatus())) {
            if ("PICKED_UP".equals(order.getOrderStatus()) || "COMPLETED".equals(order.getOrderStatus())) {
                throw new BusinessException("此订单已完成取餐");
            }
            throw new BusinessException("订单状态不允许核验: " + order.getOrderStatus());
        }

        order.setOrderStatus("COMPLETED");
        order.setPickupCodeVerifiedAt(LocalDateTime.now());
        order.setCompletedAt(LocalDateTime.now());
        mealOrderRepository.save(order);

        log.info("商家核验取餐码成功, merchantId={}, orderNo={}, pickupCode={}", merchantId, order.getOrderNo(), pickupCode);
        return order;
    }

    /**
     * 获取商家订单统计
     */
    public Map<String, Object> getMerchantOrderStats(Long merchantId) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("pendingPayment", mealOrderRepository.countByMerchantIdAndOrderStatus(merchantId, "PENDING_PAYMENT"));
        stats.put("paid", mealOrderRepository.countByMerchantIdAndOrderStatus(merchantId, "PAID"));
        stats.put("preparing", mealOrderRepository.countByMerchantIdAndOrderStatus(merchantId, "PREPARING"));
        stats.put("ready", mealOrderRepository.countByMerchantIdAndOrderStatus(merchantId, "READY"));
        stats.put("completed", mealOrderRepository.countByMerchantIdAndOrderStatus(merchantId, "COMPLETED"));
        stats.put("cancelled", mealOrderRepository.countByMerchantIdAndOrderStatus(merchantId, "CANCELLED"));
        return stats;
    }
}
