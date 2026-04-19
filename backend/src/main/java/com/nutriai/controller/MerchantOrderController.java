package com.nutriai.controller;

import com.nutriai.common.ApiResponse;
import com.nutriai.entity.MealOrder;
import com.nutriai.entity.Merchant;
import com.nutriai.service.MealService;
import com.nutriai.service.MerchantService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 商家端 - 订单管理
 */
@Slf4j
@RestController
@RequestMapping("/merchant/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MERCHANT')")
public class MerchantOrderController {

    private final MealService mealService;
    private final MerchantService merchantService;

    /**
     * 获取商家的订单列表
     */
    @GetMapping
    public ApiResponse<Page<MealOrder>> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        return ApiResponse.success(mealService.getMerchantOrders(merchantId, status, page, size));
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/{orderNo}")
    public ApiResponse<MealOrder> getOrderDetail(
            @PathVariable String orderNo,
            HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        return ApiResponse.success(mealService.getMerchantOrderDetail(merchantId, orderNo));
    }

    /**
     * 接单（PAID → PREPARING）
     */
    @PostMapping("/{orderNo}/accept")
    public ApiResponse<MealOrder> acceptOrder(
            @PathVariable String orderNo,
            HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        return ApiResponse.success(mealService.merchantUpdateOrderStatus(merchantId, orderNo, "PREPARING"));
    }

    /**
     * 出餐完成（PREPARING → READY）
     */
    @PostMapping("/{orderNo}/ready")
    public ApiResponse<MealOrder> markReady(
            @PathVariable String orderNo,
            HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        return ApiResponse.success(mealService.merchantUpdateOrderStatus(merchantId, orderNo, "READY"));
    }

    /**
     * 核验取餐码
     */
    @PostMapping("/verify-pickup")
    public ApiResponse<MealOrder> verifyPickup(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        String pickupCode = body.get("pickupCode");
        return ApiResponse.success(mealService.merchantVerifyPickup(merchantId, pickupCode));
    }

    /**
     * 获取订单统计
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats(HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        return ApiResponse.success(mealService.getMerchantOrderStats(merchantId));
    }

    private Long getMerchantId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Merchant merchant = merchantService.getMerchantByOwnerId(userId);
        return merchant.getId();
    }
}
