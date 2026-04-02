package com.nutriai.repository;

import com.nutriai.entity.VipOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VipOrderRepository extends JpaRepository<VipOrder, Long> {

    Optional<VipOrder> findByOrderNo(String orderNo);

    Optional<VipOrder> findByTradeNo(String tradeNo);

    Page<VipOrder> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 查询用户最新一条已支付且未过期的VIP订单
     */
    @Query("SELECT o FROM VipOrder o WHERE o.userId = :userId " +
           "AND o.paymentStatus = 'PAID' " +
           "AND o.vipExpireAt > :now " +
           "ORDER BY o.vipExpireAt DESC")
    List<VipOrder> findActiveVipOrders(@Param("userId") Long userId,
                                       @Param("now") LocalDateTime now);

    /**
     * 查询待支付且已超时的订单
     */
    @Query("SELECT o FROM VipOrder o WHERE o.paymentStatus = 'PENDING' " +
           "AND o.expireTime < :now")
    List<VipOrder> findExpiredPendingOrders(@Param("now") LocalDateTime now);

    /**
     * 统计用户成功支付订单数
     */
    long countByUserIdAndPaymentStatus(Long userId, String paymentStatus);

    void deleteAllByUserId(Long userId);
}
