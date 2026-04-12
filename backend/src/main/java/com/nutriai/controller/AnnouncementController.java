package com.nutriai.controller;

import com.nutriai.common.ApiResponse;
import com.nutriai.dto.admin.SystemAnnouncementDTO;
import com.nutriai.service.AnnouncementService;
import com.nutriai.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 公告控制器（用户端）
 */
@Slf4j
@RestController
@RequestMapping("/announcements")
@RequiredArgsConstructor
public class AnnouncementController {
    
    private final AnnouncementService announcementService;
    private final JwtUtil jwtUtil;
    
    /**
     * 获取公开的公告列表（分页），登录用户包含已读状态
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<SystemAnnouncementDTO>>> getPublicAnnouncements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        try {
            Long userId = extractUserIdSilently(request);
            Page<SystemAnnouncementDTO> announcements = announcementService.getPublicAnnouncements(page, size, userId);
            return ResponseEntity.ok(ApiResponse.success("获取成功", announcements));
        } catch (Exception e) {
            log.error("获取公告列表失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "获取失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取未读公告数量（需登录）
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(HttpServletRequest request) {
        try {
            Long userId = extractUserIdSilently(request);
            if (userId == null) {
                return ResponseEntity.ok(ApiResponse.success("获取成功", Map.of("count", 0L)));
            }
            long count = announcementService.getUnreadCount(userId);
            return ResponseEntity.ok(ApiResponse.success("获取成功", Map.of("count", count)));
        } catch (Exception e) {
            log.error("获取未读数量失败", e);
            return ResponseEntity.ok(ApiResponse.success("获取成功", Map.of("count", 0L)));
        }
    }
    
    /**
     * 标记单条公告为已读（需登录）
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = extractUserIdSilently(request);
            if (userId == null) {
                return ResponseEntity.status(401).body(ApiResponse.error(401, "请先登录"));
            }
            announcementService.markAsRead(userId, id);
            return ResponseEntity.ok(ApiResponse.success("已标记为已读", null));
        } catch (Exception e) {
            log.error("标记已读失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "操作失败: " + e.getMessage()));
        }
    }
    
    /**
     * 标记所有公告为已读（需登录）
     */
    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(HttpServletRequest request) {
        try {
            Long userId = extractUserIdSilently(request);
            if (userId == null) {
                return ResponseEntity.status(401).body(ApiResponse.error(401, "请先登录"));
            }
            announcementService.markAllAsRead(userId);
            return ResponseEntity.ok(ApiResponse.success("已全部标记为已读", null));
        } catch (Exception e) {
            log.error("全部标记已读失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "操作失败: " + e.getMessage()));
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

    /**
     * 从请求中安全提取userId，无token时返回null
     */
    private Long extractUserIdSilently(HttpServletRequest request) {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                return jwtUtil.getUserIdFromToken(token);
            }
        } catch (Exception ignored) {}
        return null;
    }
}
