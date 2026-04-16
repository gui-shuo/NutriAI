package com.nutriai.service;

import com.nutriai.entity.RefundRequest;
import com.nutriai.exception.BusinessException;
import com.nutriai.repository.RefundRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 退款/售后服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRequestRepository refundRepo;

    /** 用户申请退款 */
    @Transactional
    public RefundRequest applyRefund(Long userId, String orderNo, String orderType,
                                     java.math.BigDecimal refundAmount,
                                     String reason, String imagesJson) {
        // 防重：同一订单已有待处理申请则拒绝
        if (refundRepo.existsByOrderNoAndStatusIn(orderNo,
                Arrays.asList("PENDING", "APPROVED", "PROCESSING"))) {
            throw new BusinessException("该订单已有退款申请正在处理中");
        }

        String refundNo = "RF" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        // Parse images JSON array if provided
        List<String> imageList = new java.util.ArrayList<>();
        if (imagesJson != null && !imagesJson.isBlank()) {
            try {
                // Simple split for comma-separated URLs or JSON array string
                imagesJson = imagesJson.trim();
                if (imagesJson.startsWith("[")) {
                    imagesJson = imagesJson.substring(1, imagesJson.length() - 1);
                }
                for (String s : imagesJson.split(",")) {
                    String url = s.trim().replaceAll("^\"|\"$", "");
                    if (!url.isEmpty()) imageList.add(url);
                }
            } catch (Exception ignored) {}
        }

        RefundRequest req = RefundRequest.builder()
                .refundNo(refundNo)
                .orderNo(orderNo)
                .orderType(orderType)
                .userId(userId)
                .refundAmount(refundAmount)
                .reason(reason)
                .images(imageList)
                .status("PENDING")
                .build();
        return refundRepo.save(req);
    }

    /** 用户查看自己的退款记录 */
    public Page<RefundRequest> getUserRefunds(Long userId, Pageable pageable) {
        return refundRepo.findByUserId(userId, pageable);
    }

    /** 管理员列表 */
    public Page<RefundRequest> adminList(String status, Pageable pageable) {
        if (status == null || status.isEmpty()) {
            return refundRepo.findAllByOrderByCreatedAtDesc(pageable);
        }
        return refundRepo.findByStatus(status, pageable);
    }

    /** 管理员审核：通过 */
    @Transactional
    public RefundRequest approve(Long id, String adminRemark) {
        RefundRequest req = findAndCheck(id, "PENDING");
        req.setStatus("APPROVED");
        req.setAdminRemark(adminRemark);
        req.setProcessedAt(LocalDateTime.now());
        return refundRepo.save(req);
    }

    /** 管理员审核：拒绝 */
    @Transactional
    public RefundRequest reject(Long id, String adminRemark) {
        RefundRequest req = findAndCheck(id, "PENDING");
        req.setStatus("REJECTED");
        req.setAdminRemark(adminRemark);
        req.setProcessedAt(LocalDateTime.now());
        return refundRepo.save(req);
    }

    /** 标记退款完成（已原路退款） */
    @Transactional
    public RefundRequest complete(Long id) {
        RefundRequest req = findAndCheck(id, "APPROVED");
        req.setStatus("COMPLETED");
        req.setProcessedAt(LocalDateTime.now());
        return refundRepo.save(req);
    }

    private RefundRequest findAndCheck(Long id, String expectedStatus) {
        RefundRequest req = refundRepo.findById(id)
                .orElseThrow(() -> new BusinessException("退款申请不存在"));
        if (!expectedStatus.equals(req.getStatus())) {
            throw new BusinessException("退款申请状态不允许此操作，当前状态: " + req.getStatus());
        }
        return req;
    }
}
