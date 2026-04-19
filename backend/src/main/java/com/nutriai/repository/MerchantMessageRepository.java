package com.nutriai.repository;

import com.nutriai.entity.MerchantMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MerchantMessageRepository extends JpaRepository<MerchantMessage, Long> {

    Page<MerchantMessage> findByConversationIdOrderByCreatedAtDesc(String conversationId, Pageable pageable);

    // 获取商家的所有会话（每个会话最新一条）
    @Query(value = "SELECT m.* FROM merchant_messages m " +
            "INNER JOIN (SELECT conversation_id, MAX(id) as max_id FROM merchant_messages WHERE merchant_id = :merchantId GROUP BY conversation_id) latest " +
            "ON m.id = latest.max_id ORDER BY m.created_at DESC",
            nativeQuery = true)
    List<MerchantMessage> findMerchantConversations(@Param("merchantId") Long merchantId);

    // 获取用户的所有会话（每个会话最新一条）
    @Query(value = "SELECT m.* FROM merchant_messages m " +
            "INNER JOIN (SELECT conversation_id, MAX(id) as max_id FROM merchant_messages WHERE user_id = :userId GROUP BY conversation_id) latest " +
            "ON m.id = latest.max_id ORDER BY m.created_at DESC",
            nativeQuery = true)
    List<MerchantMessage> findUserConversations(@Param("userId") Long userId);

    // 标记会话消息已读
    @Modifying
    @Query("UPDATE MerchantMessage m SET m.isRead = true WHERE m.conversationId = :conversationId AND m.senderType = :senderType AND m.isRead = false")
    int markAsRead(@Param("conversationId") String conversationId, @Param("senderType") MerchantMessage.SenderType senderType);

    // 商家未读消息数
    @Query("SELECT COUNT(m) FROM MerchantMessage m WHERE m.merchantId = :merchantId AND m.senderType = 'USER' AND m.isRead = false")
    long countMerchantUnread(@Param("merchantId") Long merchantId);

    // 用户未读消息数
    @Query("SELECT COUNT(m) FROM MerchantMessage m WHERE m.userId = :userId AND m.senderType = 'MERCHANT' AND m.isRead = false")
    long countUserUnread(@Param("userId") Long userId);
}
