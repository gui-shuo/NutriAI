package com.nutriai.controller;

import com.nutriai.common.ApiResponse;
import com.nutriai.dto.admin.SystemAnnouncementDTO;
import com.nutriai.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 公告控制器（用户端）
 */
@Slf4j
@RestController
@RequestMapping("/announcements")
@RequiredArgsConstructor
public class AnnouncementController {
    
    private final AnnouncementService announcementService;
    
    /**
     * 获取公开的公告列表（分页）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<SystemAnnouncementDTO>>> getPublicAnnouncements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<SystemAnnouncementDTO> announcements = announcementService.getPublicAnnouncements(page, size);
            return ResponseEntity.ok(ApiResponse.success("获取成功", announcements));
        } catch (Exception e) {
            log.error("获取公告列表失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "获取失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取活动公告列表（不分页）
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<Page<SystemAnnouncementDTO>>> getActiveAnnouncements() {
        try {
            // 默认获取前10条活动公告
            Page<SystemAnnouncementDTO> announcements = announcementService.getPublicAnnouncements(1, 10);
            return ResponseEntity.ok(ApiResponse.success("获取成功", announcements));
        } catch (Exception e) {
            log.error("获取活动公告失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "获取失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取公告详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SystemAnnouncementDTO>> getAnnouncementDetail(@PathVariable Long id) {
        try {
            SystemAnnouncementDTO announcement = announcementService.getAnnouncementById(id);
            if (announcement != null && announcement.getIsActive()) {
                return ResponseEntity.ok(ApiResponse.success("获取成功", announcement));
            } else {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "公告不存在或已禁用"));
            }
        } catch (Exception e) {
            log.error("获取公告详情失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "获取失败: " + e.getMessage()));
        }
    }
}
