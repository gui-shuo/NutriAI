package com.nutriai.repository;

import com.nutriai.entity.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCodeAndIsActiveTrue(String code);

    @Query("SELECT c FROM Coupon c WHERE c.isActive = true " +
           "AND (c.applicableType = 'ALL' OR c.applicableType = :type) " +
           "AND (c.startTime IS NULL OR c.startTime <= :now) " +
           "AND (c.endTime IS NULL OR c.endTime >= :now) " +
           "AND (c.totalCount = -1 OR c.usedCount < c.totalCount)")
    List<Coupon> findAvailableByType(String type, LocalDateTime now);

    Page<Coupon> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
