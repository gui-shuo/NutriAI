package com.nutriai.repository;

import com.nutriai.entity.FoodRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 饮食记录Repository
 */
@Repository
public interface FoodRecordRepository extends JpaRepository<FoodRecord, Long> {
    
    /**
     * 根据用户ID分页查询饮食记录（按记录时间倒序）
     */
    Page<FoodRecord> findByUserIdOrderByRecordTimeDesc(Long userId, Pageable pageable);
    
    /**
     * 根据用户ID和时间范围查询饮食记录
     */
    List<FoodRecord> findByUserIdAndRecordTimeBetween(
            Long userId, 
            LocalDateTime startTime, 
            LocalDateTime endTime
    );
    
    /**
     * 根据用户ID和餐次类型查询
     */
    Page<FoodRecord> findByUserIdAndMealTypeOrderByRecordTimeDesc(
            Long userId, 
            FoodRecord.MealType mealType, 
            Pageable pageable
    );
    
    /**
     * 根据用户ID和时间范围分页查询
     */
    Page<FoodRecord> findByUserIdAndRecordTimeBetweenOrderByRecordTimeDesc(
            Long userId, 
            LocalDateTime startTime, 
            LocalDateTime endTime, 
            Pageable pageable
    );
    
    /**
     * 根据用户ID、餐次类型和时间范围分页查询
     */
    Page<FoodRecord> findByUserIdAndMealTypeAndRecordTimeBetweenOrderByRecordTimeDesc(
            Long userId,
            FoodRecord.MealType mealType,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Pageable pageable
    );
    
    /**
     * 统计指定时间范围内的记录数量
     */
    @Query("SELECT COUNT(f) FROM FoodRecord f WHERE f.userId = :userId " +
           "AND f.recordTime BETWEEN :startTime AND :endTime")
    Long countByUserIdAndTimeRange(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
    
    /**
     * 删除用户的所有饮食记录
     */
    void deleteByUserId(Long userId);
}
