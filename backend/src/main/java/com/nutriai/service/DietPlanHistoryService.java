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
        Pageable pageable = PageRequest.of(page - 1, size);
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
}
