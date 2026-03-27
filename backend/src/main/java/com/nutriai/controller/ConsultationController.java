package com.nutriai.controller;

import com.nutriai.common.ApiResponse;
import com.nutriai.entity.ConsultationOrder;
import com.nutriai.entity.Nutritionist;
import com.nutriai.service.ConsultationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 营养师在线咨询Controller
 */
@Slf4j
@RestController
@RequestMapping("/consultation")
@RequiredArgsConstructor
public class ConsultationController {

    private final ConsultationService consultationService;

    /**
     * 获取所有营养师列表
     */
    @GetMapping("/nutritionists")
    public ApiResponse<List<Nutritionist>> getNutritionists() {
        return ApiResponse.success(consultationService.getActiveNutritionists());
    }

    /**
     * 获取在线营养师列表
     */
    @GetMapping("/nutritionists/online")
    public ApiResponse<List<Nutritionist>> getOnlineNutritionists() {
        return ApiResponse.success(consultationService.getOnlineNutritionists());
    }

    /**
     * 获取营养师详情
     */
    @GetMapping("/nutritionists/{id}")
    public ApiResponse<Nutritionist> getNutritionistDetail(@PathVariable Long id) {
        return ApiResponse.success(consultationService.getNutritionistDetail(id));
    }

    /**
     * 创建咨询订单
     */
    @PostMapping("/orders")
    public ApiResponse<ConsultationOrder> createConsultation(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        Long nutritionistId = Long.valueOf(body.get("nutritionistId").toString());
        String description = body.get("description") != null ? body.get("description").toString() : "";
        String consultationType = body.get("consultationType") != null ? body.get("consultationType").toString() : "TEXT";
        return ApiResponse.success(consultationService.createConsultation(userId, nutritionistId, description, consultationType));
    }

    /**
     * 模拟支付咨询订单
     */
    @PostMapping("/orders/{orderNo}/simulate-pay")
    public ApiResponse<ConsultationOrder> simulatePayment(
            @PathVariable String orderNo,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(consultationService.simulatePayConsultation(userId, orderNo));
    }

    /**
     * 发送咨询消息
     */
    @PostMapping("/orders/{orderNo}/messages")
    public ApiResponse<ConsultationOrder> sendMessage(
            @PathVariable String orderNo,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        String content = body.get("content");
        String role = body.getOrDefault("role", "user");
        return ApiResponse.success(consultationService.sendMessage(userId, orderNo, content, role));
    }

    /**
     * 模拟营养师回复
     */
    @PostMapping("/orders/{orderNo}/reply")
    public ApiResponse<ConsultationOrder> simulateReply(
            @PathVariable String orderNo,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(consultationService.simulateNutritionistReply(userId, orderNo));
    }

    /**
     * 完成咨询
     */
    @PostMapping("/orders/{orderNo}/complete")
    public ApiResponse<ConsultationOrder> completeConsultation(
            @PathVariable String orderNo,
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        Integer rating = body != null && body.get("rating") != null ? Integer.valueOf(body.get("rating").toString()) : null;
        String review = body != null && body.get("review") != null ? body.get("review").toString() : null;
        return ApiResponse.success(consultationService.completeConsultation(userId, orderNo, rating, review));
    }

    /**
     * 模拟退款
     */
    @PostMapping("/orders/{orderNo}/simulate-refund")
    public ApiResponse<ConsultationOrder> simulateRefund(
            @PathVariable String orderNo,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(consultationService.simulateRefund(userId, orderNo));
    }

    /**
     * 获取咨询订单历史
     */
    @GetMapping("/orders")
    public ApiResponse<Page<ConsultationOrder>> getOrderHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(consultationService.getConsultationHistory(userId, page, size));
    }

    /**
     * 获取活跃咨询（进行中）
     */
    @GetMapping("/orders/active")
    public ApiResponse<List<ConsultationOrder>> getActiveConsultations(HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(consultationService.getActiveConsultations(userId));
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
}
