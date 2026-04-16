package com.nutriai.controller;

import com.nutriai.service.RefundService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 退款/售后接口
 */
@RestController
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    /** 用户申请退款 */
    @PostMapping("/refunds")
    public ResponseEntity<?> apply(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = getUserId(request);
        String orderNo = (String) body.get("orderNo");
        String orderType = (String) body.get("orderType");
        BigDecimal amount = new BigDecimal(body.get("refundAmount").toString());
        String reason = (String) body.get("reason");
        String images = body.containsKey("images") ? (String) body.get("images") : null;
        return ResponseEntity.ok(refundService.applyRefund(userId, orderNo, orderType, amount, reason, images));
    }

    /** 用户查看自己的退款记录 */
    @GetMapping("/refunds/my")
    public ResponseEntity<?> myRefunds(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       HttpServletRequest request) {
        Long userId = getUserId(request);
        return ResponseEntity.ok(refundService.getUserRefunds(userId,
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    // ---- Admin ----

    @GetMapping("/admin/refunds")
    public ResponseEntity<?> adminList(@RequestParam(required = false) String status,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(refundService.adminList(status,
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @PostMapping("/admin/refunds/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(refundService.approve(id, body.get("adminRemark")));
    }

    @PostMapping("/admin/refunds/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(refundService.reject(id, body.get("adminRemark")));
    }

    @PostMapping("/admin/refunds/{id}/complete")
    public ResponseEntity<?> complete(@PathVariable Long id) {
        return ResponseEntity.ok(refundService.complete(id));
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
}
