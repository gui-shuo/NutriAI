package com.nutriai.controller;

import com.nutriai.entity.Coupon;
import com.nutriai.entity.UserCoupon;
import com.nutriai.service.CouponService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 优惠券接口
 */
@RestController
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    /** 获取用户优惠券列表 */
    @GetMapping("/coupons/my")
    public ResponseEntity<?> getMyCoupons(@RequestParam(required = false) String status,
                                          HttpServletRequest request) {
        Long userId = getUserId(request);
        return ResponseEntity.ok(couponService.getUserCoupons(userId, status));
    }

    /** 领取优惠券 */
    @PostMapping("/coupons/{couponId}/claim")
    public ResponseEntity<?> claim(@PathVariable Long couponId, HttpServletRequest request) {
        Long userId = getUserId(request);
        UserCoupon uc = couponService.claimCoupon(userId, couponId);
        return ResponseEntity.ok(uc);
    }

    /** 通过优惠码领取 */
    @PostMapping("/coupons/claim-by-code")
    public ResponseEntity<?> claimByCode(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = getUserId(request);
        UserCoupon uc = couponService.claimByCode(userId, body.get("code"));
        return ResponseEntity.ok(uc);
    }

    /** 获取可用优惠券（按类型） */
    @GetMapping("/coupons/available")
    public ResponseEntity<?> getAvailable(@RequestParam(defaultValue = "ALL") String type) {
        return ResponseEntity.ok(couponService.getAvailableForType(type));
    }

    // ---- Admin ----

    @GetMapping("/admin/coupons")
    public ResponseEntity<?> adminList(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(couponService.adminList(
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @PostMapping("/admin/coupons")
    public ResponseEntity<?> adminCreate(@RequestBody Coupon coupon) {
        return ResponseEntity.ok(couponService.adminCreate(coupon));
    }

    @PutMapping("/admin/coupons/{id}")
    public ResponseEntity<?> adminUpdate(@PathVariable Long id, @RequestBody Coupon coupon) {
        return ResponseEntity.ok(couponService.adminUpdate(id, coupon));
    }

    @PutMapping("/admin/coupons/{id}/toggle")
    public ResponseEntity<?> adminToggle(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        couponService.adminToggle(id, body.getOrDefault("active", true));
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/admin/coupons/{id}")
    public ResponseEntity<?> adminDelete(@PathVariable Long id) {
        couponService.adminDelete(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
}
