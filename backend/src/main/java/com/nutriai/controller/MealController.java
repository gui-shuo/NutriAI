package com.nutriai.controller;

import com.nutriai.common.ApiResponse;
import com.nutriai.entity.MealItem;
import com.nutriai.entity.MealOrder;
import com.nutriai.service.MealService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 营养餐Controller
 */
@Slf4j
@RestController
@RequestMapping("/meals")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;

    /**
     * 获取可用餐品列表（支持分类/餐类筛选）
     */
    @GetMapping
    public ApiResponse<Page<MealItem>> getMeals(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String mealType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ApiResponse.success(mealService.getAvailableMeals(category, mealType, page, size));
    }

    /**
     * 搜索餐品
     */
    @GetMapping("/search")
    public ApiResponse<Page<MealItem>> searchMeals(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ApiResponse.success(mealService.searchMeals(keyword, page, size));
    }

    /**
     * 获取推荐餐品
     */
    @GetMapping("/recommended")
    public ApiResponse<List<MealItem>> getRecommendedMeals() {
        return ApiResponse.success(mealService.getRecommendedMeals());
    }

    /**
     * 获取餐品详情
     */
    @GetMapping("/{id}")
    public ApiResponse<MealItem> getMealDetail(@PathVariable Long id) {
        return ApiResponse.success(mealService.getMealDetail(id));
    }

    /**
     * 获取餐品分类列表
     */
    @GetMapping("/categories")
    public ApiResponse<List<String>> getCategories() {
        return ApiResponse.success(mealService.getCategories());
    }

    /**
     * 创建营养餐订单
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/orders")
    public ApiResponse<MealOrder> createOrder(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = getUserId(request);

        List<Map<String, Object>> items;
        Object itemsObj = body.get("items");
        if (itemsObj instanceof List) {
            items = (List<Map<String, Object>>) itemsObj;
        } else if (itemsObj instanceof Map) {
            items = List.of((Map<String, Object>) itemsObj);
        } else {
            Object mid = body.get("mealItemId");
            Object qty = body.get("quantity");
            if (mid != null) {
                items = List.of(Map.of("mealItemId", mid, "quantity", qty != null ? qty : 1));
            } else {
                return ApiResponse.error(400, "缺少商品信息");
            }
        }

        String fulfillmentType = getStringField(body, "fulfillmentType", "deliveryMethod", "PICKUP");
        if ("pickup".equalsIgnoreCase(fulfillmentType)) fulfillmentType = "PICKUP";
        else if ("delivery".equalsIgnoreCase(fulfillmentType)) fulfillmentType = "DELIVERY";
        String pickupTime = getStringField(body, "pickupTime", null, null);
        String pickupLocation = getStringField(body, "pickupLocation", null, null);
        String receiverName = getStringField(body, "receiverName", null, "");
        String receiverPhone = getStringField(body, "receiverPhone", "phone", "");
        String receiverAddress = getStringField(body, "receiverAddress", "address", "");
        String remark = getStringField(body, "remark", "notes", "");

        // 门店信息
        Long merchantId = null;
        Object merchantIdObj = body.get("merchantId");
        if (merchantIdObj != null) {
            merchantId = Long.valueOf(merchantIdObj.toString());
        }
        String merchantName = getStringField(body, "merchantName", null, null);

        return ApiResponse.success(mealService.createOrder(userId, items, fulfillmentType,
                pickupTime, pickupLocation, receiverName, receiverPhone, receiverAddress, remark,
                merchantId, merchantName));
    }

    /**
     * 获取用户订单历史
     */
    @GetMapping("/orders")
    public ApiResponse<Page<MealOrder>> getOrderHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(mealService.getOrderHistory(userId, page, size));
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/orders/{orderNo}")
    public ApiResponse<MealOrder> getOrderDetail(
            @PathVariable String orderNo,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(mealService.getOrderDetail(userId, orderNo));
    }

    /**
     * 模拟支付
     */
    @PostMapping("/orders/{orderNo}/simulate-pay")
    public ApiResponse<MealOrder> simulatePayment(
            @PathVariable String orderNo,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(mealService.simulatePayment(userId, orderNo));
    }

    /**
     * 取消订单
     */
    @PostMapping("/orders/{orderNo}/cancel")
    public ApiResponse<MealOrder> cancelOrder(
            @PathVariable String orderNo,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        String reason = (body != null && body.get("reason") != null) ? body.get("reason") : "用户取消";
        return ApiResponse.success(mealService.cancelOrder(userId, orderNo, reason));
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    private String getStringField(Map<String, Object> body, String primary, String fallback, String defaultVal) {
        Object val = body.get(primary);
        if (val == null && fallback != null) val = body.get(fallback);
        return val != null ? val.toString() : defaultVal;
    }
}
