package com.nutriai.controller;

import com.nutriai.common.ApiResponse;
import com.nutriai.service.MerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 公共接口 - 门店/商家查询（无需登录）
 * 支持按用户位置计算距离并排序返回最近门店
 */
@Slf4j
@RestController
@RequestMapping("/merchants")
@RequiredArgsConstructor
public class PublicMerchantController {

    private final MerchantService merchantService;

    /**
     * 获取附近门店列表（按距离排序）
     *
     * @param latitude  用户纬度
     * @param longitude 用户经度
     * @param limit     返回数量，默认10
     */
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getNearbyMerchants(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                merchantService.getNearbyMerchants(latitude, longitude, limit)));
    }

    /**
     * 获取所有营业中的门店列表（不排序/按sortOrder排序）
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getActiveMerchants() {
        return ResponseEntity.ok(ApiResponse.success(
                merchantService.getActiveMerchantList()));
    }

    /**
     * 获取门店详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMerchantDetail(
            @PathVariable Long id,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {
        return ResponseEntity.ok(ApiResponse.success(
                merchantService.getMerchantDetail(id, latitude, longitude)));
    }
}
