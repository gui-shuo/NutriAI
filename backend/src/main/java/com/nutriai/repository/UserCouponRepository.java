package com.nutriai.repository;

import com.nutriai.entity.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    List<UserCoupon> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId);

    int countByUserIdAndCouponId(Long userId, Long couponId);

    @Query("SELECT uc FROM UserCoupon uc WHERE uc.userId = :userId AND uc.status = 'UNUSED' AND uc.expireAt > :now ORDER BY uc.expireAt ASC")
    List<UserCoupon> findValidByUserId(Long userId, LocalDateTime now);

    @Query("SELECT uc FROM UserCoupon uc WHERE uc.status = 'UNUSED' AND uc.expireAt <= :now")
    List<UserCoupon> findExpiredUnused(LocalDateTime now);
}
