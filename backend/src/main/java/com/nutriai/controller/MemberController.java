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
        Long userId = getUserId(httpRequest);
        log.debug("获取用户 {} 的会员信息", userId);
        MemberInfoResponse memberInfo = memberService.getMemberInfo(userId);
        return ApiResponse.success(memberInfo);
    }

    /**
     * 获取成长值记录（分页）
     */
    @GetMapping("/growth-records")
    public ApiResponse<Page<GrowthRecordResponse>> getGrowthRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        return ApiResponse.success(memberService.getGrowthRecords(userId, page, size));
    }

    /**
     * 每日签到（返回获得的成长值，0 表示今日已签到）
     */
    @PostMapping("/sign-in")
    public ApiResponse<Integer> dailySignIn(HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        int earned = memberService.dailySignIn(userId);
        if (earned > 0) {
            return ApiResponse.success("签到成功，获得 " + earned + " 成长值", earned);
        } else {
            return ApiResponse.success("今日已签到", 0);
        }
    }

    /**
     * 生成邀请链接
     */
    @GetMapping("/invitation/generate")
    public ApiResponse<GenerateInvitationResponse> generateInvitationLink(HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        return ApiResponse.success(memberService.generateInvitationLink(userId));
    }

    /**
     * 查询邀请记录（分页）
     */
    @GetMapping("/invitation/records")
    public ApiResponse<Page<InvitationResponse>> getInvitationRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        return ApiResponse.success(memberService.getInvitationRecords(userId, page, size));
    }

    // ---- 私有工具方法 ----

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
}

