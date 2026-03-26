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
     * 创建充值订单（返回支付宝支付表单HTML）
     */
    @PostMapping("/orders")
    public ApiResponse<VipOrderResponse> createOrder(
            @Valid @RequestBody VipOrderRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        VipOrderResponse resp = vipService.createOrder(userId, request.getPlanId());
        return ApiResponse.success(resp);
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
     * 支付宝异步回调通知（无需登录，由支付宝服务器主动调用）
     * URL 须配置在 SecurityConfig 白名单中
     */
    @PostMapping("/alipay/notify")
    public String alipayNotify(HttpServletRequest request) {
        Map<String, String> params = extractRequestParams(request);
        log.info("收到支付宝回调, tradeStatus={}, orderNo={}",
                params.get("trade_status"), params.get("out_trade_no"));
        return vipService.handleAlipayNotify(params);
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
