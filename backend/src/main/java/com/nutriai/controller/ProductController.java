package com.nutriai.controller;

import com.nutriai.common.ApiResponse;
import com.nutriai.entity.NutritionProduct;
import com.nutriai.entity.ProductOrder;
import com.nutriai.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 营养产品商城Controller
 */
@Slf4j
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 获取产品列表（支持分类筛选）
     */
    @GetMapping
    public ApiResponse<Page<NutritionProduct>> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ApiResponse.success(productService.getProducts(category, page, size));
    }

    /**
     * 搜索产品
     */
    @GetMapping("/search")
    public ApiResponse<Page<NutritionProduct>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ApiResponse.success(productService.searchProducts(keyword, page, size));
    }

    /**
     * 获取推荐产品
     */
    @GetMapping("/recommended")
    public ApiResponse<List<NutritionProduct>> getRecommendedProducts() {
        return ApiResponse.success(productService.getRecommendedProducts());
    }

    /**
     * 获取产品详情
     */
    @GetMapping("/{id}")
    public ApiResponse<NutritionProduct> getProductDetail(@PathVariable Long id) {
        return ApiResponse.success(productService.getProductDetail(id));
    }

    /**
     * 获取产品分类列表
     */
    @GetMapping("/categories")
    public ApiResponse<List<String>> getCategories() {
        return ApiResponse.success(productService.getCategories());
    }

    /**
     * 创建产品订单
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/orders")
    public ApiResponse<ProductOrder> createOrder(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        List<Map<String, Object>> items;
        Object itemsObj = body.get("items");
        if (itemsObj instanceof List) {
            items = (List<Map<String, Object>>) itemsObj;
        } else if (itemsObj instanceof Map) {
            // single item sent as object instead of array
            items = List.of((Map<String, Object>) itemsObj);
        } else {
            // mini program flat format: { productId, quantity, ... }
            Object pid = body.get("productId");
            Object qty = body.get("quantity");
            if (pid != null) {
                items = List.of(Map.of("productId", pid, "quantity", qty != null ? qty : 1));
            } else {
                return ApiResponse.error(400, "缺少商品信息");
            }
        }
        String receiverName = body.get("receiverName") != null ? body.get("receiverName").toString() : "";
        String receiverPhone = body.get("receiverPhone") != null ? body.get("receiverPhone").toString() : "";
        String receiverAddress = body.get("receiverAddress") != null ? body.get("receiverAddress").toString() : "";
        String remark = body.get("remark") != null ? body.get("remark").toString() : "";
        return ApiResponse.success(productService.createOrder(userId, items, receiverName, receiverPhone, receiverAddress, remark));
    }

    /**
     * 模拟支付订单
     */
    @PostMapping("/orders/{orderNo}/simulate-pay")
    public ApiResponse<ProductOrder> simulatePayment(
            @PathVariable String orderNo,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(productService.simulatePayment(userId, orderNo));
    }

    /**
     * 模拟发货
     */
    @PostMapping("/orders/{orderNo}/simulate-ship")
    public ApiResponse<ProductOrder> simulateShip(
            @PathVariable String orderNo,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(productService.simulateShip(userId, orderNo));
    }

    /**
     * 确认收货
     */
    @PostMapping("/orders/{orderNo}/confirm-receive")
    public ApiResponse<ProductOrder> confirmReceive(
            @PathVariable String orderNo,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(productService.confirmReceive(userId, orderNo));
    }

    /**
     * 模拟退款
     */
    @PostMapping("/orders/{orderNo}/simulate-refund")
    public ApiResponse<ProductOrder> simulateRefund(
            @PathVariable String orderNo,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(productService.simulateRefund(userId, orderNo));
    }

    /**
     * 用户取消订单
     */
    @PostMapping("/orders/{orderNo}/cancel")
    public ApiResponse<ProductOrder> cancelOrder(
            @PathVariable String orderNo,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(productService.cancelOrder(userId, orderNo));
    }

    /**
     * 获取订单历史（支持状态过滤）
     */
    @GetMapping("/orders")
    public ApiResponse<Page<ProductOrder>> getOrderHistory(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(productService.getOrderHistory(userId, status, page, size));
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
}
