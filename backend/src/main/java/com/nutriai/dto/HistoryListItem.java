package com.nutriai.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 历史记录列表项
 */
@Data
public class HistoryListItem {
    
    private String planId;
    private String title;
    private Integer days;
    private String goal;
    private Boolean isFavorite;
    private LocalDateTime createdAt;
    
    public static HistoryListItem fromEntity(com.nutriai.entity.DietPlanHistory history) {
        HistoryListItem item = new HistoryListItem();
        item.setPlanId(history.getPlanId());
        item.setTitle(history.getTitle());
        item.setDays(history.getDays());
        item.setGoal(history.getGoal());
        item.setIsFavorite(history.getIsFavorite() != null ? history.getIsFavorite() : false);
        item.setCreatedAt(history.getCreatedAt());
        return item;
    }
}
