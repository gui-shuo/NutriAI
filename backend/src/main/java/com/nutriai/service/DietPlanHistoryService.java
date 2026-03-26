package com.nutriai.service;

import com.nutriai.dto.DietPlanResponse;
import com.nutriai.dto.HistoryListItem;
import com.nutriai.entity.DietPlanHistory;
import com.nutriai.repository.DietPlanHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 饮食计划历史记录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DietPlanHistoryService {
    
    private final DietPlanHistoryRepository historyRepository;
    
    /**
     * 获取历史记录列表
     */
    public Page<HistoryListItem> getHistoryList(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size);
        return historyRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(HistoryListItem::fromEntity);
    }
    
    /**
     * 获取历史记录详情
     */
    public DietPlanResponse getHistoryDetail(String planId, Long userId) {
        return historyRepository.findByPlanId(planId)
                .filter(history -> history.getUserId().equals(userId))
                .map(history -> DietPlanResponse.builder()
                        .planId(history.getPlanId())
                        .title(history.getTitle())
                        .days(history.getDays())
                        .goalDescription(history.getGoal())
                        .markdownContent(history.getMarkdownContent())
                        .isFavorite(history.getIsFavorite() != null ? history.getIsFavorite() : false)
                        .build())
                .orElse(null);
    }
    
    /**
     * 删除历史记录
     */
    public boolean deleteHistory(String planId, Long userId) {
        return historyRepository.findByPlanId(planId)
                .filter(history -> history.getUserId().equals(userId))
                .map(history -> {
                    historyRepository.delete(history);
                    log.info("用户 {} 删除了饮食计划 {}", userId, planId);
                    return true;
                })
                .orElse(false);
    }
    
    /**
     * 收藏/取消收藏
     */
    @Transactional
    public boolean toggleFavorite(String planId, Long userId) {
        return historyRepository.findByPlanId(planId)
                .filter(history -> history.getUserId().equals(userId))
                .map(history -> {
                    boolean newState = !(history.getIsFavorite() != null && history.getIsFavorite());
                    history.setIsFavorite(newState);
                    historyRepository.save(history);
                    log.info("用户 {} {}了饮食计划 {}", userId, newState ? "收藏" : "取消收藏", planId);
                    return newState;
                })
                .orElseThrow(() -> new RuntimeException("计划不存在"));
    }
    
    /**
     * 获取收藏列表
     */
    public Page<HistoryListItem> getFavoriteList(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size);
        return historyRepository.findByUserIdAndIsFavoriteTrueOrderByCreatedAtDesc(userId, pageable)
                .map(HistoryListItem::fromEntity);
    }
    
    /**
     * 更新计划内容（用于修改建议功能）
     */
    @Transactional
    public void updatePlanContent(String planId, Long userId, String newMarkdownContent, String newTitle) {
        historyRepository.findByPlanId(planId)
                .filter(history -> history.getUserId().equals(userId))
                .ifPresent(history -> {
                    history.setMarkdownContent(newMarkdownContent);
                    if (newTitle != null) {
                        history.setTitle(newTitle);
                    }
                    historyRepository.save(history);
                    log.info("用户 {} 更新了饮食计划 {}", userId, planId);
                });
    }
}
