package com.nutriai.controller;

import com.nutriai.common.ApiResponse;
import com.nutriai.dto.user.*;
import com.nutriai.service.HealthProfileService;
import com.nutriai.service.OssService;
import com.nutriai.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private final OssService ossService;
    private final HealthProfileService healthProfileService;
    
    /**
     * 获取当前用户资料
     */
    @GetMapping("/profile")
    public ApiResponse<UserProfileResponse> getProfile(HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        UserProfileResponse profile = userService.getUserProfile(userId);
        return ApiResponse.success(profile);
    }
    
    /**
     * 更新用户资料
     */
    @PutMapping("/profile")
    public ApiResponse<UserProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromToken(httpRequest);
        UserProfileResponse profile = userService.updateUserProfile(userId, request);
        return ApiResponse.success("资料更新成功", profile);
    }
    
    /**
     * 修改密码
     */
    @PutMapping("/password")
    public ApiResponse<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromToken(httpRequest);
        userService.changePassword(userId, request);
        
        ApiResponse<Void> response = ApiResponse.success();
        response.setMessage("密码修改成功");
        return response;
    }
    
    /**
     * 发送短信验证码
     */
    @PostMapping("/sms/send")
    public ApiResponse<SmsCodeResponse> sendSmsCode(@Valid @RequestBody SendSmsRequest request) {
        SmsCodeResponse response = userService.sendSmsCode(request.getPhone());
        return ApiResponse.success(response);
    }
    
    /**
     * 修改手机号
     */
    @PutMapping("/phone")
    public ApiResponse<Void> changePhone(
            @Valid @RequestBody ChangePhoneRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromToken(httpRequest);
        userService.changePhone(userId, request);
        
        ApiResponse<Void> response = ApiResponse.success();
        response.setMessage("手机号修改成功");
        return response;
    }
    
    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    public ApiResponse<String> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromToken(httpRequest);
        
        // 上传文件到OSS
        String avatarUrl = ossService.uploadAvatar(file);
        
        // 更新用户头像
        UpdateProfileRequest updateRequest = UpdateProfileRequest.builder()
                .avatar(avatarUrl)
                .build();
        userService.updateUserProfile(userId, updateRequest);
        
        return ApiResponse.success("头像上传成功", avatarUrl);
    }
    
    /**
     * 获取健康档案
     */
    @GetMapping("/health-profile")
    public ApiResponse<HealthProfileResponse> getHealthProfile(HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        HealthProfileResponse profile = healthProfileService.getHealthProfile(userId);
        return ApiResponse.success(profile);
    }
    
    /**
     * 保存/更新健康档案
     */
    @PutMapping("/health-profile")
    public ApiResponse<HealthProfileResponse> saveHealthProfile(
            @Valid @RequestBody HealthProfileRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromToken(httpRequest);
        HealthProfileResponse profile = healthProfileService.saveHealthProfile(userId, request);
        return ApiResponse.success("健康档案保存成功", profile);
    }
    
    /**
     * 从Token中获取用户ID（由JwtAuthenticationFilter注入）
     */
    private Long getUserIdFromToken(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
}
