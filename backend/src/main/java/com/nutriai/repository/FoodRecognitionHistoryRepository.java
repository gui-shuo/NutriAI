package com.nutriai.repository;

import com.nutriai.entity.FoodRecognitionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 食物识别历史Repository
 */
@Repository
public interface FoodRecognitionHistoryRepository extends JpaRepository<FoodRecognitionHistory, Long> {
    
    /**
     * 根据用户ID查询识别历史
     */
    List<FoodRecognitionHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * 根据用户ID查询最近N条记录
     */
    List<FoodRecognitionHistory> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);

    void deleteAllByUserId(Long userId);
}
