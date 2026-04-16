package com.nutriai.service;

import com.nutriai.entity.NutritionProduct;
import com.nutriai.entity.ProductOrder;
import com.nutriai.exception.BusinessException;
import com.nutriai.repository.NutritionProductRepository;
import com.nutriai.repository.ProductOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 营养产品商城服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final NutritionProductRepository productRepository;
    private final ProductOrderRepository orderRepository;
    private final MemberService memberService;

    /**
     * 获取产品列表（分页，可按分类筛选）
     */
    public Page<NutritionProduct> getProducts(String category, int page, int size) {
        if (category != null && !category.isBlank() && !"ALL".equals(category)) {
            return productRepository.findOnSaleByCategory(category, PageRequest.of(page, size));
        }
        return productRepository.findOnSale(PageRequest.of(page, size));
    }

    /**
     * 搜索产品
     */
    public Page<NutritionProduct> searchProducts(String keyword, int page, int size) {
        return productRepository.searchByKeyword(keyword, PageRequest.of(page, size));
    }

    /**
     * 获取推荐产品
     */
    public List<NutritionProduct> getRecommendedProducts() {
        return productRepository.findOnSaleAndRecommended();
    }

    /**
     * 获取产品详情
     */
    public NutritionProduct getProductDetail(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("产品不存在"));
    }

    /**
     * 获取所有活跃分类
     */
    public List<String> getCategories() {
        return productRepository.findActiveCategories();
    }

    /**
     * 创建订单
     */
    @Transactional
    public ProductOrder createOrder(Long userId, List<Map<String, Object>> items, 
                                     String receiverName, String receiverPhone, 
                                     String receiverAddress, String remark) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("订单商品不能为空");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQuantity = 0;
        List<Map<String, Object>> orderItems = new ArrayList<>();

        for (Map<String, Object> item : items) {
            Long productId = Long.valueOf(item.get("productId").toString());
            int quantity = Integer.parseInt(item.get("quantity").toString());

            NutritionProduct product = productRepository.findById(productId)
                    .orElseThrow(() -> new BusinessException("产品不存在: " + productId));

            if (!"ACTIVE".equals(product.getStatus()) && !"ON_SALE".equals(product.getStatus())) {
                throw new BusinessException("产品已下架: " + product.getName());
            }

            if (product.getStock() < quantity) {
                throw new BusinessException("库存不足: " + product.getName());
            }

            BigDecimal itemTotal = product.getSalePrice().multiply(BigDecimal.valueOf(quantity));
            totalAmount = totalAmount.add(itemTotal);
            totalQuantity += quantity;

            Map<String, Object> orderItem = new HashMap<>();
            orderItem.put("productId", productId);
            orderItem.put("productName", product.getName());
            orderItem.put("imageUrl", product.getImageUrl());
            orderItem.put("price", product.getSalePrice());
            orderItem.put("quantity", quantity);
            orderItem.put("subtotal", itemTotal);
            orderItems.add(orderItem);
        }

        String orderNo = generateOrderNo("PRD");

        ProductOrder order = ProductOrder.builder()
                .orderNo(orderNo)
                .userId(userId)
                .items(orderItems)
                .totalQuantity(totalQuantity)
                .totalAmount(totalAmount)
                .receiverName(receiverName)
                .receiverPhone(receiverPhone)
                .receiverAddress(receiverAddress)
                .remark(remark)
                .paymentStatus("PENDING")
                .orderStatus("PENDING_PAYMENT")
                .expireTime(LocalDateTime.now().plusMinutes(30))
                .build();

        order = orderRepository.save(order);
        log.info("产品订单创建成功, userId={}, orderNo={}, amount={}", userId, orderNo, totalAmount);
        return order;
    }

    /**
     * 模拟支付
     */
    @Transactional
    public ProductOrder simulatePayment(Long userId, String orderNo) {
        ProductOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException("订单不存在"));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }

        if ("PAID".equals(order.getPaymentStatus())) {
            return order;
        }

        if (!"PENDING".equals(order.getPaymentStatus())) {
            throw new BusinessException("订单状态不允许支付");
        }

        // 扣减库存
        for (Map<String, Object> item : order.getItems()) {
            Long productId = Long.valueOf(item.get("productId").toString());
            int quantity = Integer.parseInt(item.get("quantity").toString());
            NutritionProduct product = productRepository.findById(productId).orElse(null);
            if (product != null) {
                product.setStock(Math.max(0, product.getStock() - quantity));
                product.setSalesCount(product.getSalesCount() + quantity);
                productRepository.save(product);
            }
        }

        order.setPaymentStatus("PAID");
        order.setOrderStatus("PAID");
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);

        // 赠送成长值
        memberService.addGrowth(userId, 10, "SYSTEM_REWARD", "购买营养产品");

        log.info("产品订单支付成功(模拟), userId={}, orderNo={}", userId, orderNo);
        return order;
    }

    /**
     * 模拟发货
     */
    @Transactional
    public ProductOrder simulateShip(Long userId, String orderNo) {
        ProductOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException("订单不存在"));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }

        if (!"PAID".equals(order.getOrderStatus())) {
            throw new BusinessException("订单状态不允许发货");
        }

        order.setOrderStatus("SHIPPED");
        order.setShippedAt(LocalDateTime.now());
        order.setTrackingNo("SF" + System.currentTimeMillis());
        orderRepository.save(order);

        log.info("产品订单已发货(模拟), userId={}, orderNo={}", userId, orderNo);
        return order;
    }

    /**
     * 确认收货
     */
    @Transactional
    public ProductOrder confirmReceive(Long userId, String orderNo) {
        ProductOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException("订单不存在"));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }

        if (!"SHIPPED".equals(order.getOrderStatus())) {
            throw new BusinessException("订单状态不允许确认收货");
        }

        order.setOrderStatus("COMPLETED");
        order.setCompletedAt(LocalDateTime.now());
        orderRepository.save(order);

        log.info("产品订单已完成, userId={}, orderNo={}", userId, orderNo);
        return order;
    }

    /**
     * 模拟退款
     */
    @Transactional
    public ProductOrder simulateRefund(Long userId, String orderNo) {
        ProductOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException("订单不存在"));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }

        if (!"PAID".equals(order.getPaymentStatus())) {
            throw new BusinessException("仅已支付的订单可退款");
        }

        if ("COMPLETED".equals(order.getOrderStatus())) {
            throw new BusinessException("已完成的订单不支持退款");
        }

        // 恢复库存
        for (Map<String, Object> item : order.getItems()) {
            Long productId = Long.valueOf(item.get("productId").toString());
            int quantity = Integer.parseInt(item.get("quantity").toString());
            NutritionProduct product = productRepository.findById(productId).orElse(null);
            if (product != null) {
                product.setStock(product.getStock() + quantity);
                product.setSalesCount(Math.max(0, product.getSalesCount() - quantity));
                productRepository.save(product);
            }
        }

        order.setPaymentStatus("REFUNDED");
        order.setOrderStatus("REFUNDED");
        orderRepository.save(order);

        log.info("产品订单退款成功(模拟), userId={}, orderNo={}", userId, orderNo);
        return order;
    }

    /**
     * 获取用户订单历史（支持状态过滤）
     */
    public Page<ProductOrder> getOrderHistory(Long userId, String status, int page, int size) {
        if (status != null && !status.isEmpty()) {
            return orderRepository.findByUserIdAndOrderStatusOrderByCreatedAtDesc(userId, status, PageRequest.of(page, size));
        }
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    /**
     * 用户取消订单
     */
    @Transactional
    public ProductOrder cancelOrder(Long userId, String orderNo) {
        ProductOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException("订单不存在"));
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        if (!"PENDING_PAYMENT".equals(order.getOrderStatus()) && !"PAID".equals(order.getOrderStatus())) {
            throw new BusinessException("当前订单状态不可取消");
        }
        order.setOrderStatus("CANCELLED");
        order.setCancelReason("用户主动取消");
        order.setCancelledAt(LocalDateTime.now());
        // 恢复库存
        for (Map<String, Object> item : order.getItems()) {
            try {
                Long productId = Long.valueOf(item.get("productId").toString());
                int quantity = Integer.parseInt(item.get("quantity").toString());
                NutritionProduct product = productRepository.findById(productId).orElse(null);
                if (product != null) {
                    product.setStock(product.getStock() + quantity);
                    productRepository.save(product);
                }
            } catch (Exception ignored) {}
        }
        return orderRepository.save(order);
    }

    /**
     * 定时清理超时未支付订单
     */
    @Scheduled(fixedDelay = 120_000)
    public void cancelExpiredOrders() {
        List<ProductOrder> expired = orderRepository.findExpiredPendingOrders(LocalDateTime.now());
        if (!expired.isEmpty()) {
            expired.forEach(o -> {
                o.setPaymentStatus("EXPIRED");
                o.setOrderStatus("CANCELLED");
                o.setCancelReason("超时未支付自动取消");
                o.setCancelledAt(LocalDateTime.now());
            });
            orderRepository.saveAll(expired);
            log.info("自动取消超时产品订单 {} 笔", expired.size());
        }
    }

    /**
     * 15天自动确认收货
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void autoConfirmReceipt() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(15);
        List<ProductOrder> orders = orderRepository.findShippedBeforeCutoff(cutoff);
        if (!orders.isEmpty()) {
            orders.forEach(o -> {
                o.setOrderStatus("COMPLETED");
                o.setCompletedAt(LocalDateTime.now());
            });
            orderRepository.saveAll(orders);
            log.info("自动确认收货 {} 笔产品订单", orders.size());
        }
    }

    /**
     * 管理员发货
     */
    @Transactional
    public ProductOrder shipOrder(String orderNo, String shippingCompany, String trackingNo) {
        ProductOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException("订单不存在"));
        if (!"PAID".equals(order.getOrderStatus())) {
            throw new BusinessException("订单状态不允许发货，当前状态: " + order.getOrderStatus());
        }
        order.setOrderStatus("SHIPPED");
        order.setShippedAt(LocalDateTime.now());
        order.setShippingCompany(shippingCompany);
        order.setTrackingNo(trackingNo);
        order.setAutoConfirmAt(LocalDateTime.now().plusDays(15));
        orderRepository.save(order);
        log.info("管理员发货: orderNo={}, company={}, trackingNo={}", orderNo, shippingCompany, trackingNo);
        return order;
    }

    /**
     * 管理员更新订单状态
     */
    @Transactional
    public ProductOrder adminUpdateOrderStatus(String orderNo, String newStatus) {
        ProductOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException("订单不存在"));
        order.setOrderStatus(newStatus);
        if ("COMPLETED".equals(newStatus) && order.getCompletedAt() == null) {
            order.setCompletedAt(LocalDateTime.now());
        }
        if ("CANCELLED".equals(newStatus) && order.getCancelledAt() == null) {
            order.setCancelledAt(LocalDateTime.now());
        }
        return orderRepository.save(order);
    }

    /**
     * 产品订单统计
     */
    public Map<String, Object> getOrderStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalOrders", orderRepository.count());
        stats.put("pendingPayment", orderRepository.countByOrderStatus("PENDING_PAYMENT"));
        stats.put("paid", orderRepository.countByOrderStatus("PAID"));
        stats.put("shipped", orderRepository.countByOrderStatus("SHIPPED"));
        stats.put("completed", orderRepository.countByOrderStatus("COMPLETED"));
        stats.put("cancelled", orderRepository.countByOrderStatus("CANCELLED"));

        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        BigDecimal monthRevenue = orderRepository.sumRevenueByPeriod(monthStart, LocalDateTime.now());
        stats.put("monthRevenue", monthRevenue != null ? monthRevenue : BigDecimal.ZERO);
        return stats;
    }

    private String generateOrderNo(String prefix) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return prefix + timestamp + random;
    }
}
