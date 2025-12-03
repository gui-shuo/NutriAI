package com.nutriai.controller;

import com.nutriai.common.ApiResponse;
import com.nutriai.dto.member.*;
import com.nutriai.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * 会员Controller
 */
@Slf4j
@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {
    
    private final MemberService memberService;
    
    /**
     * 获取会员信息
     */
    @GetMapping("/info")
    public ApiResponse<MemberInfoResponse> getMemberInfo(HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            log.info("获取用户{}的会员信息", userId);
            MemberInfoResponse memberInfo = memberService.getMemberInfo(userId);
            return ApiResponse.success(memberInfo);
        } catch (Exception e) {
            log.error("获取会员信息失败", e);
            return ApiResponse.error("获取会员信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取成长值记录
     */
    @GetMapping("/growth-records")
    public ApiResponse<Page<GrowthRecordResponse>> getGrowthRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromToken(httpRequest);
        Page<GrowthRecordResponse> records = memberService.getGrowthRecords(userId, page, size);
        return ApiResponse.success(records);
    }
    
    /**
     * 生成邀请链接
     */
    @GetMapping("/invitation/generate")
    public ApiResponse<GenerateInvitationResponse> generateInvitationLink(HttpServletRequest httpRequest) {
        Long userId = getUserIdFromToken(httpRequest);
        GenerateInvitationResponse response = memberService.generateInvitationLink(userId);
        return ApiResponse.success(response);
    }
    
    /**
     * 查询邀请记录
     */
    @GetMapping("/invitation/records")
    public ApiResponse<Page<InvitationResponse>> getInvitationRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromToken(httpRequest);
        Page<InvitationResponse> records = memberService.getInvitationRecords(userId, page, size);
        return ApiResponse.success(records);
    }
    
    /**
     * 手动升级等级（管理员）
     */
    @PostMapping("/upgrade/{userId}")
    public ApiResponse<Void> upgradeLevel(@PathVariable Long userId) {
        // 这里应该添加管理员权限检查
        var member = memberService.getMemberInfo(userId);
        // TODO: 调用升级逻辑
        return ApiResponse.success();
    }
    
    /**
     * 从Token中获取用户ID
     */
    private Long getUserIdFromToken(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
}
