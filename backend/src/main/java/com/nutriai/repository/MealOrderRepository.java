package com.nutriai.repository;

import com.nutriai.entity.MealOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MealOrderRepository extends JpaRepository<MealOrder, Long> {

    Optional<MealOrder> findByOrderNo(String orderNo);

    Optional<MealOrder> findByOrderNoAndUserId(String orderNo, Long userId);

    Page<MealOrder> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<MealOrder> findByUserIdAndOrderStatusOrderByCreatedAtDesc(Long userId, String orderStatus, Pageable pageable);

    // Admin queries
    Page<MealOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<MealOrder> findByOrderStatusOrderByCreatedAtDesc(String orderStatus, Pageable pageable);

    @Query("SELECT COUNT(o) FROM MealOrder o WHERE o.orderStatus = :status")
    long countByOrderStatus(@Param("status") String status);

    Optional<MealOrder> findByPickupCode(String pickupCode);

    // Merchant queries
    Page<MealOrder> findByMerchantIdOrderByCreatedAtDesc(Long merchantId, Pageable pageable);

    Page<MealOrder> findByMerchantIdAndOrderStatusOrderByCreatedAtDesc(Long merchantId, String orderStatus, Pageable pageable);

    @Query("SELECT COUNT(o) FROM MealOrder o WHERE o.merchantId = :merchantId AND o.orderStatus = :status")
    long countByMerchantIdAndOrderStatus(@Param("merchantId") Long merchantId, @Param("status") String status);

    Optional<MealOrder> findByOrderNoAndMerchantId(String orderNo, Long merchantId);

    Optional<MealOrder> findByPickupCodeAndMerchantId(String pickupCode, Long merchantId);
}
