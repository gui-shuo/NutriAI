package com.nutriai.controller;

import com.nutriai.common.ApiResponse;
import com.nutriai.dto.vip.*;
import com.nutriai.service.VipService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VIP充值会员Controller
 */
@Slf4j
@RestController
@RequestMapping("/vip")
@RequiredArgsConstructor
public class VipController {

    private final VipService vipService;

    /**
     * 获取所有VIP套餐（公开接口，无需登录即可查看）
     */
    @GetMapping("/plans")
    public ApiResponse<List<VipPlanResponse>> getPlans() {
        return ApiResponse.success(vipService.getActivePlans());
    }

    /**
     * 创建充值订单
     */
    @PostMapping("/orders")
    public ApiResponse<VipOrderResponse> createOrder(
            @Valid @RequestBody VipOrderRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        VipOrderResponse resp = vipService.createOrder(userId, request.getPlanId(), request.getPayType());
        return ApiResponse.success(resp);
    }

    /**
     * 模拟支付确认（前端点击"确认支付"后调用）
     */
    @PostMapping("/orders/{orderNo}/simulate-pay")
    public ApiResponse<VipOrderResponse> simulatePayment(
            @PathVariable String orderNo,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        return ApiResponse.success(vipService.simulatePayment(userId, orderNo));
    }

    /**
     * 模拟退款
     */
    @PostMapping("/orders/{orderNo}/simulate-refund")
    public ApiResponse<VipOrderResponse> simulateRefund(
            @PathVariable String orderNo,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        return ApiResponse.success(vipService.simulateRefund(userId, orderNo));
    }

    /**
     * 主动查询订单支付状态（前端轮询）
     */
    @GetMapping("/orders/{orderNo}/status")
    public ApiResponse<VipOrderResponse> queryOrderStatus(
            @PathVariable String orderNo,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        return ApiResponse.success(vipService.queryOrderStatus(userId, orderNo));
    }

    /**
     * 获取当前用户VIP状态
     */
    @GetMapping("/status")
    public ApiResponse<VipStatusResponse> getVipStatus(HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        return ApiResponse.success(vipService.getVipStatus(userId));
    }

    /**
     * 获取订单历史
     */
    @GetMapping("/orders")
    public ApiResponse<Page<VipOrderResponse>> getOrderHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        return ApiResponse.success(vipService.getOrderHistory(userId, page, size));
    }

    /**
     * 易支付异步回调通知（无需登录，由支付平台服务器主动调用）
     * URL 须配置在 SecurityConfig 白名单中
     */
    @PostMapping("/epay/notify")
    public String ePayNotify(HttpServletRequest request) {
        Map<String, String> params = extractRequestParams(request);
        log.info("收到易支付回调, tradeStatus={}, orderNo={}",
                params.get("trade_status"), params.get("out_trade_no"));
        return vipService.handleEPayNotify(params);
    }

    /**
     * 易支付同步跳转（用户支付完成后浏览器回跳）
     * 不做业务处理，仅验签后重定向到前端
     */
    @GetMapping("/epay/return")
    public void ePayReturn(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) throws Exception {
        Map<String, String> params = extractRequestParams(request);
        log.info("收到易支付同步回跳, orderNo={}", params.get("out_trade_no"));
        // 重定向到前端会员页面
        String orderNo = params.get("out_trade_no");
        String frontendUrl = System.getenv("CORS_ALLOWED_ORIGINS");
        if (frontendUrl == null || frontendUrl.isBlank()) {
            frontendUrl = "http://localhost:5173";
        }
        // 取第一个origin（可能是逗号分隔列表）
        if (frontendUrl.contains(",")) {
            frontendUrl = frontendUrl.split(",")[0].trim();
        }
        response.sendRedirect(frontendUrl + "/membership?orderNo=" + (orderNo != null ? orderNo : ""));
    }

    // ---- 私有工具方法 ----

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    /**
     * 将 HttpServletRequest 中的所有参数提取为 Map<String,String>
     * （支持支付宝回调中单值参数模式）
     */
    private Map<String, String> extractRequestParams(HttpServletRequest request) {
        Map<String, String> result = new HashMap<>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            result.put(name, request.getParameter(name));
        }
        return result;
    }
}
