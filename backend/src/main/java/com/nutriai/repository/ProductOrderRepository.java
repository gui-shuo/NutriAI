package com.nutriai.repository;

import com.nutriai.entity.ProductOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductOrderRepository extends JpaRepository<ProductOrder, Long> {

    Optional<ProductOrder> findByOrderNo(String orderNo);

    Page<ProductOrder> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT o FROM ProductOrder o WHERE o.paymentStatus = 'PENDING' AND o.expireTime < :now")
    List<ProductOrder> findExpiredPendingOrders(LocalDateTime now);

    void deleteAllByUserId(Long userId);
}
