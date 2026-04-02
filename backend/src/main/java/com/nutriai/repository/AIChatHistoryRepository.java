package com.nutriai.repository;

import com.nutriai.entity.AIChatHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * AI聊天历史记录Repository
 */
@Repository
public interface AIChatHistoryRepository extends JpaRepository<AIChatHistory, Long> {
    
    /**
     * 根据用户ID查询历史记录（分页）
     */
    Page<AIChatHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * 根据用户ID查询历史记录（按更新时间排序，分页）
     */
    Page<AIChatHistory> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * 查询所有历史记录（按更新时间排序，分页）
     */
    Page<AIChatHistory> findAllByOrderByUpdatedAtDesc(Pageable pageable);
    
    /**
     * 根据用户ID查询所有历史记录
     */
    List<AIChatHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * 根据ID和用户ID查询
     */
    Optional<AIChatHistory> findByIdAndUserId(Long id, Long userId);
    
    /**
     * 删除用户的所有历史记录
     */
    void deleteByUserId(Long userId);
    
    /**
     * 统计指定时间后创建的记录数
     */
    long countByCreatedAtAfter(LocalDateTime dateTime);
    
    /**
     * 统计指定时间范围内创建的记录数
     */
    long countByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计用户的会话数
     */
    long countByUserId(Long userId);
}
