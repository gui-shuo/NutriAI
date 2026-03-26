package com.nutriai.repository;

import com.nutriai.entity.AIChatLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI聊天日志Repository
 */
@Repository
public interface AIChatLogRepository extends JpaRepository<AIChatLog, Long> {
    
    /**
     * 根据用户ID查询日志（分页）
     */
    Page<AIChatLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * 根据状态查询日志（分页）
     */
    Page<AIChatLog> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    
    /**
     * 根据用户ID和状态查询日志（分页）
     */
    Page<AIChatLog> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status, Pageable pageable);
    
    /**
     * 根据时间范围查询日志（分页）
     */
    Page<AIChatLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startDate, 
            LocalDateTime endDate, 
            Pageable pageable
    );
    
    /**
     * 复合条件查询
     */
    @Query("SELECT l FROM AIChatLog l WHERE " +
           "(:userId IS NULL OR l.userId = :userId) AND " +
           "(:status IS NULL OR l.status = :status) AND " +
           "(:startDate IS NULL OR l.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR l.createdAt <= :endDate) " +
           "ORDER BY l.createdAt DESC")
    Page<AIChatLog> findByConditions(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
    
    /**
     * 统计总日志数
     */
    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 统计成功的日志数
     */
    long countByStatusAndCreatedAtBetween(String status, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 统计用户的日志数
     */
    long countByUserId(Long userId);

    /**
     * 统计指定用户在时间范围内的日志数
     */
    long countByUserIdAndCreatedAtBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 获取平均响应时间
     */
    @Query("SELECT AVG(l.responseTime) FROM AIChatLog l WHERE l.status = 'success' AND l.createdAt BETWEEN :startDate AND :endDate")
    Double getAverageResponseTime(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * 获取平均token使用量
     */
    @Query("SELECT AVG(l.tokensUsed) FROM AIChatLog l WHERE l.status = 'success' AND l.createdAt BETWEEN :startDate AND :endDate")
    Double getAverageTokensUsed(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * 按日期统计对话数
     */
    @Query("SELECT DATE(l.createdAt) as date, COUNT(l) as count FROM AIChatLog l " +
           "WHERE l.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(l.createdAt) ORDER BY date")
    List<Object[]> countByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
