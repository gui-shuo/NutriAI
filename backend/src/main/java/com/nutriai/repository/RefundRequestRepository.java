package com.nutriai.repository;

import com.nutriai.entity.RefundRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {

    Optional<RefundRequest> findByRefundNo(String refundNo);

    Optional<RefundRequest> findByOrderNo(String orderNo);

    Page<RefundRequest> findByStatus(String status, Pageable pageable);

    Page<RefundRequest> findByUserId(Long userId, Pageable pageable);

    Page<RefundRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByOrderNoAndStatusIn(String orderNo, java.util.List<String> statuses);
}
