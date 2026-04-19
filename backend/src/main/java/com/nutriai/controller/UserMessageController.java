package com.nutriai.controller;

import com.nutriai.common.ApiResponse;
import com.nutriai.entity.MerchantMessage;
import com.nutriai.repository.MerchantMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 用户消息控制器 - 用户端与商家通信
 */
@RestController
@RequestMapping("/user/messages")
@RequiredArgsConstructor
@Slf4j
public class UserMessageController {

    private final MerchantMessageRepository messageRepository;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(auth.getName());
    }

    /**
     * 获取用户的会话列表
     */
    @GetMapping("/conversations")
    public ApiResponse<List<MerchantMessage>> getConversations() {
        Long userId = getCurrentUserId();
        List<MerchantMessage> conversations = messageRepository.findUserConversations(userId);
        return ApiResponse.success("获取成功", conversations);
    }

    /**
     * 获取会话消息历史
     */
    @GetMapping("/history")
    public ApiResponse<Page<MerchantMessage>> getHistory(
            @RequestParam Long merchantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getCurrentUserId();
        String conversationId = "user_" + userId + "_merchant_" + merchantId;
        Page<MerchantMessage> messages = messageRepository.findByConversationIdOrderByCreatedAtDesc(
                conversationId, PageRequest.of(page, size));
        return ApiResponse.success("获取成功", messages);
    }

    /**
     * 用户发送消息给商家
     */
    @PostMapping("/send")
    @Transactional
    public ApiResponse<MerchantMessage> sendMessage(@RequestBody Map<String, Object> body) {
        Long userId = getCurrentUserId();
        Long merchantId = Long.parseLong(body.get("merchantId").toString());
        String content = body.get("content").toString();

        String conversationId = "user_" + userId + "_merchant_" + merchantId;

        MerchantMessage message = MerchantMessage.builder()
                .conversationId(conversationId)
                .senderType(MerchantMessage.SenderType.USER)
                .senderId(userId)
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
        Long userId = getCurrentUserId();
        Long merchantId = Long.parseLong(body.get("merchantId").toString());
        String conversationId = "user_" + userId + "_merchant_" + merchantId;
        // 标记商家发来的消息为已读
        messageRepository.markAsRead(conversationId, MerchantMessage.SenderType.MERCHANT);
        return ApiResponse.success("标记成功", null);
    }

    /**
     * 获取未读消息数
     */
    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount() {
        Long userId = getCurrentUserId();
        long count = messageRepository.countUserUnread(userId);
        return ApiResponse.success("获取成功", count);
    }
}
