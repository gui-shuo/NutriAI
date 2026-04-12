package com.nutriai.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统公告DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemAnnouncementDTO {
    private Long id;
    private String title;
    private String content;
    private String type;            // info/warning/error
    private Integer priority;
    private Boolean isActive;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long createdBy;
    private String createdByName;   // 创建人姓名（关联查询）
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isRead;         // 当前用户是否已读
}
