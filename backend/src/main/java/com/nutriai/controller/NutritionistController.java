package com.nutriai.controller;

import com.nutriai.common.ApiResponse;
import com.nutriai.entity.ConsultationOrder;
import com.nutriai.entity.Nutritionist;
import com.nutriai.service.ConsultationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 营养师端Controller - 供营养师角色用户使用
 */
@Slf4j
@RestController
@RequestMapping("/nutritionist")
@RequiredArgsConstructor
@PreAuthorize("hasRole('NUTRITIONIST')")
public class NutritionistController {

    private final ConsultationService consultationService;

    /**
     * 获取当前营养师信息
     */
    @GetMapping("/profile")
    public ApiResponse<Nutritionist> getProfile(HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(consultationService.getNutritionistByUserId(userId));
    }

    /**
     * 更新在线状态
     */
    @PutMapping("/status")
    public ApiResponse<Nutritionist> updateStatus(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        Nutritionist nutritionist = consultationService.getNutritionistByUserId(userId);
        String status = body.get("status");
        return ApiResponse.success(consultationService.updateNutritionistStatus(nutritionist.getId(), status));
    }

    /**
     * 获取我的咨询列表
     */
    @GetMapping("/consultations")
    public ApiResponse<Page<ConsultationOrder>> getConsultations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        Nutritionist nutritionist = consultationService.getNutritionistByUserId(userId);
        return ApiResponse.success(consultationService.getNutritionistConsultations(nutritionist.getId(), page, size, status));
    }

    /**
     * 获取活跃的咨询
     */
    @GetMapping("/consultations/active")
    public ApiResponse<List<ConsultationOrder>> getActiveConsultations(HttpServletRequest request) {
        Long userId = getUserId(request);
        Nutritionist nutritionist = consultationService.getNutritionistByUserId(userId);
        return ApiResponse.success(consultationService.getNutritionistActiveConsultations(nutritionist.getId()));
    }

    /**
     * 回复咨询消息
     */
    @PostMapping("/consultations/{orderNo}/reply")
    public ApiResponse<ConsultationOrder> replyConsultation(
            @PathVariable String orderNo,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        Nutritionist nutritionist = consultationService.getNutritionistByUserId(userId);
        String content = body.get("content");
        return ApiResponse.success(consultationService.nutritionistReply(nutritionist.getId(), orderNo, content));
    }

    /**
     * 营养师完成咨询
     */
    @PostMapping("/consultations/{orderNo}/complete")
    public ApiResponse<ConsultationOrder> completeConsultation(
            @PathVariable String orderNo,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        Nutritionist nutritionist = consultationService.getNutritionistByUserId(userId);
        String summary = body != null ? body.get("summary") : null;
        return ApiResponse.success(consultationService.nutritionistCompleteConsultation(nutritionist.getId(), orderNo, summary));
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
}
