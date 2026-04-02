package com.nutriai.repository;

import com.nutriai.entity.GrowthRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 成长值记录Repository
 */
@Repository
public interface GrowthRecordRepository extends JpaRepository<GrowthRecord, Long> {
    
    /**
     * 根据用户ID分页查询
     */
    Page<GrowthRecord> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * 根据会员ID分页查询
     */
    Page<GrowthRecord> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);
    
    /**
     * 根据用户ID和日期范围查询
     */
    @Query("SELECT gr FROM GrowthRecord gr WHERE gr.userId = :userId " +
           "AND gr.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY gr.createdAt DESC")
    List<GrowthRecord> findByUserIdAndDateRange(@Param("userId") Long userId,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);
    
    /**
     * 统计用户总成长值
     */
    @Query("SELECT COALESCE(SUM(gr.growthValue), 0) FROM GrowthRecord gr WHERE gr.userId = :userId")
    Integer sumGrowthByUserId(@Param("userId") Long userId);
    
    /**
     * 按月统计成长值
     */
    @Query("SELECT FUNCTION('DATE_FORMAT', gr.createdAt, '%Y-%m') as month, " +
           "SUM(gr.growthValue) as total " +
           "FROM GrowthRecord gr " +
           "WHERE gr.userId = :userId AND gr.createdAt >= :startDate " +
           "GROUP BY FUNCTION('DATE_FORMAT', gr.createdAt, '%Y-%m') " +
           "ORDER BY month")
    List<Object[]> getMonthlyGrowthStats(@Param("userId") Long userId, 
                                         @Param("startDate") LocalDateTime startDate);

    void deleteAllByUserId(Long userId);
}
