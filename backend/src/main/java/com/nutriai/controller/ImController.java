package com.nutriai.controller;

import com.nutriai.common.ApiResponse;
import com.nutriai.config.TencentImConfig;
import com.nutriai.entity.ConsultationOrder;
import com.nutriai.entity.Nutritionist;
import com.nutriai.entity.User;
import com.nutriai.repository.ConsultationOrderRepository;
import com.nutriai.repository.NutritionistRepository;
import com.nutriai.repository.UserRepository;
import com.nutriai.service.TencentImService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 即时通信IM配置接口
 * 为前端提供TIM SDK所需的登录凭证和会话信息
 */
@Slf4j
@RestController
@RequestMapping("/im")
@RequiredArgsConstructor
public class ImController {

    private final TencentImConfig imConfig;
    private final TencentImService imService;
    private final UserRepository userRepository;
    private final NutritionistRepository nutritionistRepository;
    private final ConsultationOrderRepository consultationOrderRepository;

    /**
     * 获取IM登录配置（SDKAppID + UserSig）
     * 前端调用此接口获取登录TIM SDK所需的凭证
     */
    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> getImConfig(HttpServletRequest request) {
        if (!imConfig.isEnabled()) {
            return ApiResponse.error(503, "即时通信服务未启用");
        }

        Long userId = getUserId(request);
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ApiResponse.error(404, "用户不存在");
        }

        String imUserId = imService.toImUserId(userId);

        // 导入IM账号（幂等操作）
        imService.importAccount(imUserId,
                user.getNickname() != null ? user.getNickname() : user.getUsername(),
                user.getAvatar());

        // 生成UserSig
        String userSig = imService.generateUserSig(imUserId);

        Map<String, Object> config = new HashMap<>();
        config.put("sdkAppId", imConfig.getSdkAppId());
        config.put("userId", imUserId);
        config.put("userSig", userSig);

        return ApiResponse.success(config);
    }

    /**
     * 获取咨询会话的IM配置（含对方IM用户ID）
     * 前端用此接口获取特定咨询订单中对方的IM标识，用于建立C2C会话
     */
    @GetMapping("/config/consultation/{orderNo}")
    public ApiResponse<Map<String, Object>> getConsultationImConfig(
            @PathVariable String orderNo,
            HttpServletRequest request) {
        if (!imConfig.isEnabled()) {
            return ApiResponse.error(503, "即时通信服务未启用");
        }

        Long userId = getUserId(request);
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ApiResponse.error(404, "用户不存在");
        }

        ConsultationOrder order = consultationOrderRepository.findByOrderNo(orderNo).orElse(null);
        if (order == null) {
            return ApiResponse.error(404, "咨询订单不存在");
        }

        String imUserId = imService.toImUserId(userId);
        String peerImUserId;

        // 判断当前用户是咨询用户还是营养师
        if (order.getUserId().equals(userId)) {
            // 当前用户是咨询方 → 对方是营养师
            Nutritionist nutritionist = nutritionistRepository.findById(order.getNutritionistId()).orElse(null);
            if (nutritionist == null || nutritionist.getUserId() == null) {
                return ApiResponse.error(400, "营养师未绑定系统账号，无法使用即时通信");
            }
            peerImUserId = imService.toImUserId(nutritionist.getUserId());

            // 确保营养师IM账号已导入
            User nutritionistUser = userRepository.findById(nutritionist.getUserId()).orElse(null);
            if (nutritionistUser != null) {
                imService.importAccount(peerImUserId,
                        nutritionist.getName(),
                        nutritionist.getAvatar());
            }
        } else {
            // 当前用户是营养师 → 对方是咨询用户
            // 验证当前用户确实是该订单的营养师
            Nutritionist nutritionist = nutritionistRepository.findByUserId(userId).orElse(null);
            if (nutritionist == null || !nutritionist.getId().equals(order.getNutritionistId())) {
                return ApiResponse.error(403, "无权访问该咨询");
            }
            peerImUserId = imService.toImUserId(order.getUserId());

            // 确保用户IM账号已导入
            User consultUser = userRepository.findById(order.getUserId()).orElse(null);
            if (consultUser != null) {
                imService.importAccount(peerImUserId,
                        consultUser.getNickname() != null ? consultUser.getNickname() : consultUser.getUsername(),
                        consultUser.getAvatar());
            }
        }

        // 导入当前用户IM账号
        imService.importAccount(imUserId,
                user.getNickname() != null ? user.getNickname() : user.getUsername(),
                user.getAvatar());

        // 生成UserSig
        String userSig = imService.generateUserSig(imUserId);

        Map<String, Object> config = new HashMap<>();
        config.put("sdkAppId", imConfig.getSdkAppId());
        config.put("userId", imUserId);
        config.put("userSig", userSig);
        config.put("peerUserId", peerImUserId);
        config.put("orderNo", orderNo);

        log.info("IM咨询配置: user={} peer={} order={}", imUserId, peerImUserId, orderNo);
        return ApiResponse.success(config);
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
}
