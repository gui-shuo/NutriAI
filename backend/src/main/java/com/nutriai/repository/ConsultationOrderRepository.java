package com.nutriai.repository;

import com.nutriai.entity.ConsultationOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultationOrderRepository extends JpaRepository<ConsultationOrder, Long> {

    Optional<ConsultationOrder> findByOrderNo(String orderNo);

    Page<ConsultationOrder> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT o FROM ConsultationOrder o WHERE o.paymentStatus = 'PENDING' AND o.expireTime < :now")
    List<ConsultationOrder> findExpiredPendingOrders(LocalDateTime now);

    @Query("SELECT o FROM ConsultationOrder o WHERE o.userId = :userId AND o.status IN ('WAITING', 'IN_PROGRESS') ORDER BY o.createdAt DESC")
    List<ConsultationOrder> findActiveConsultations(Long userId);
}
