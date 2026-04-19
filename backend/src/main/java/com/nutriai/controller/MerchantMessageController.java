package com.nutriai.controller;

import com.nutriai.common.ApiResponse;
import com.nutriai.entity.MerchantMessage;
import com.nutriai.repository.MerchantMessageRepository;
import com.nutriai.service.MerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 商家消息控制器 - 商家端使用
 */
@RestController
@RequestMapping("/merchant/messages")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('MERCHANT')")
public class MerchantMessageController {

    private final MerchantMessageRepository messageRepository;
    private final MerchantService merchantService;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(auth.getName());
    }

    private Long getMerchantId() {
        return merchantService.getMerchantByOwnerId(getCurrentUserId()).getId();
    }

    /**
     * 获取商家的会话列表
     */
    @GetMapping("/conversations")
    public ApiResponse<List<MerchantMessage>> getConversations() {
        Long merchantId = getMerchantId();
        List<MerchantMessage> conversations = messageRepository.findMerchantConversations(merchantId);
        return ApiResponse.success("获取成功", conversations);
    }

    /**
     * 获取会话消息历史
     */
    @GetMapping("/history")
    public ApiResponse<Page<MerchantMessage>> getHistory(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long merchantId = getMerchantId();
        String conversationId = "user_" + userId + "_merchant_" + merchantId;
        Page<MerchantMessage> messages = messageRepository.findByConversationIdOrderByCreatedAtDesc(
                conversationId, PageRequest.of(page, size));
        return ApiResponse.success("获取成功", messages);
    }

    /**
     * 商家发送消息
     */
    @PostMapping("/send")
    @Transactional
    public ApiResponse<MerchantMessage> sendMessage(@RequestBody Map<String, Object> body) {
        Long merchantId = getMerchantId();
        Long userId = Long.parseLong(body.get("userId").toString());
        String content = body.get("content").toString();

        String conversationId = "user_" + userId + "_merchant_" + merchantId;

        MerchantMessage message = MerchantMessage.builder()
                .conversationId(conversationId)
                .senderType(MerchantMessage.SenderType.MERCHANT)
                .senderId(getCurrentUserId())
                .merchantId(merchantId)
                .userId(userId)
                .content(content)
                .isRead(false)
                .build();

        messageRepository.save(message);
        return ApiResponse.success("发送成功", message);
    }

    /**
     * 标记会话已读
     */
    @PostMapping("/read")
    @Transactional
    public ApiResponse<Void> markRead(@RequestBody Map<String, Object> body) {
        Long merchantId = getMerchantId();
        Long userId = Long.parseLong(body.get("userId").toString());
        String conversationId = "user_" + userId + "_merchant_" + merchantId;
        // 标记用户发来的消息为已读
        messageRepository.markAsRead(conversationId, MerchantMessage.SenderType.USER);
        return ApiResponse.success("标记成功", null);
    }

    /**
     * 获取未读消息数
     */
    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount() {
        Long merchantId = getMerchantId();
        long count = messageRepository.countMerchantUnread(merchantId);
        return ApiResponse.success("获取成功", count);
    }
}
