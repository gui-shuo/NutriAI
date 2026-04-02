package com.nutriai.repository;

import com.nutriai.entity.DietPlanHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DietPlanHistoryRepository extends JpaRepository<DietPlanHistory, Long> {
    
    Optional<DietPlanHistory> findByPlanId(String planId);
    
    Page<DietPlanHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    Page<DietPlanHistory> findByUserIdAndIsFavoriteTrueOrderByCreatedAtDesc(Long userId, Pageable pageable);

    void deleteAllByUserId(Long userId);
}
