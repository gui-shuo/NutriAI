package com.nutriai.controller;

import com.nutriai.common.ApiResponse;
import com.nutriai.entity.Merchant;
import com.nutriai.service.MerchantService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 商家端 - 店铺管理（商家管理自己的店铺信息）
 */
@Slf4j
@RestController
@RequestMapping("/merchant/store")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MERCHANT')")
public class MerchantStoreController {

    private final MerchantService merchantService;

    /**
     * 获取我的店铺信息
     */
    @GetMapping
    public ApiResponse<Merchant> getMyStore(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Merchant merchant = merchantService.getMerchantByOwnerId(userId);
        return ApiResponse.success(merchant);
    }

    /**
     * 更新店铺基本信息
     */
    @PutMapping
    public ApiResponse<Merchant> updateMyStore(
            @RequestBody Merchant update,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Merchant merchant = merchantService.getMerchantByOwnerId(userId);
        // 不允许修改 ownerId 和 status（status 由管理员控制）
        update.setOwnerId(null);
        update.setStatus(null);
        return ApiResponse.success(merchantService.updateMerchant(merchant.getId(), update));
    }

    /**
     * 更新营业状态（开店/关店）
     */
    @PutMapping("/business-status")
    public ApiResponse<Merchant> updateBusinessStatus(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Merchant merchant = merchantService.getMerchantByOwnerId(userId);
        String status = body.get("status");
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            return ApiResponse.error(400, "无效的营业状态");
        }
        Merchant upd = new Merchant();
        upd.setStatus(status);
        return ApiResponse.success(merchantService.updateMerchant(merchant.getId(), upd));
    }
}
