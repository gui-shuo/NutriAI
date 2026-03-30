package com.nutriai.controller;

import com.nutriai.common.ApiResponse;
import com.nutriai.entity.UserAddress;
import com.nutriai.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户收货地址控制器
 */
@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
public class UserAddressController {

    private final UserAddressService addressService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserAddress>>> getAddresses(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<UserAddress> list = addressService.getUserAddresses(userId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserAddress>> addAddress(
            HttpServletRequest request, @RequestBody UserAddress address) {
        Long userId = (Long) request.getAttribute("userId");
        UserAddress saved = addressService.addAddress(userId, address);
        return ResponseEntity.ok(ApiResponse.success("地址添加成功", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserAddress>> updateAddress(
            HttpServletRequest request, @PathVariable Long id, @RequestBody UserAddress address) {
        Long userId = (Long) request.getAttribute("userId");
        UserAddress updated = addressService.updateAddress(userId, id, address);
        return ResponseEntity.ok(ApiResponse.success("地址更新成功", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        addressService.deleteAddress(userId, id);
        return ResponseEntity.ok(ApiResponse.success("地址删除成功", null));
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<ApiResponse<UserAddress>> setDefault(
            HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        UserAddress address = addressService.setDefault(userId, id);
        return ResponseEntity.ok(ApiResponse.success("默认地址设置成功", address));
    }
}
