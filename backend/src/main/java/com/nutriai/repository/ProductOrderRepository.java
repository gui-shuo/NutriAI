package com.nutriai.repository;

import com.nutriai.entity.ProductOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductOrderRepository extends JpaRepository<ProductOrder, Long> {

    Optional<ProductOrder> findByOrderNo(String orderNo);

    Optional<ProductOrder> findByOrderNoAndUserId(String orderNo, Long userId);

    Page<ProductOrder> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<ProductOrder> findByUserIdAndOrderStatusOrderByCreatedAtDesc(Long userId, String status, Pageable pageable);

    @Query("SELECT o FROM ProductOrder o WHERE o.paymentStatus = 'PENDING' AND o.expireTime < :now")
    List<ProductOrder> findExpiredPendingOrders(LocalDateTime now);

    /** 15天自动确认收货 */
    @Query("SELECT o FROM ProductOrder o WHERE o.orderStatus = 'SHIPPED' AND o.shippedAt < :cutoff")
    List<ProductOrder> findShippedBeforeCutoff(LocalDateTime cutoff);

    // ---- Admin ----
    Page<ProductOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<ProductOrder> findByOrderStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    @Query("SELECT o FROM ProductOrder o WHERE " +
           "(:status IS NULL OR o.orderStatus = :status) AND " +
           "(:keyword IS NULL OR o.orderNo LIKE %:keyword% OR o.receiverName LIKE %:keyword% OR o.receiverPhone LIKE %:keyword%)" +
           " ORDER BY o.createdAt DESC")
    Page<ProductOrder> adminSearch(@Param("status") String status, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT COUNT(o) FROM ProductOrder o WHERE o.orderStatus = :status")
    long countByOrderStatus(@Param("status") String status);

    @Query("SELECT COALESCE(SUM(o.totalAmount - o.discountAmount), 0) FROM ProductOrder o WHERE o.paymentStatus = 'PAID' AND o.paidAt BETWEEN :start AND :end")
    BigDecimal sumRevenueByPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    void deleteAllByUserId(Long userId);
}

