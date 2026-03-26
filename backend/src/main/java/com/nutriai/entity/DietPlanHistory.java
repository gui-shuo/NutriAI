package com.nutriai.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 饮食计划历史记录
 */
@Data
@Entity
@Table(name = "diet_plan_history")
public class DietPlanHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "plan_id", nullable = false, unique = true, length = 50)
    private String planId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "title", nullable = false, length = 200)
    private String title;
    
    @Column(name = "days", nullable = false)
    private Integer days;
    
    @Column(name = "goal", length = 50)
    private String goal;
    
    @Column(name = "markdown_content", columnDefinition = "LONGTEXT")
    private String markdownContent;
    
    @Column(name = "is_favorite", nullable = false)
    private Boolean isFavorite = false;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
