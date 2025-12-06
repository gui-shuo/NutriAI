package com.nutriai.controller;

import com.nutriai.dto.admin.*;
import com.nutriai.common.ApiResponse;
import com.nutriai.service.AdminAILogService;
import com.nutriai.service.AdminConfigService;
import com.nutriai.service.AdminDashboardService;
import com.nutriai.service.AdminUserService;
import com.nutriai.service.ConfigOptionsService;
import com.nutriai.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 管理后台控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final AdminDashboardService dashboardService;
    private final AdminUserService userService;
    private final AdminAILogService aiLogService;
    private final AdminConfigService configService;
    private final ConfigOptionsService configOptionsService;
    private final JwtUtil jwtUtil;
    
    // ==================== 数据看板 ====================
    
    /**
     * 获取数据看板统计数据
     */
    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> getDashboardStats() {
        try {
            DashboardStatsDTO stats = dashboardService.getDashboardStats();
            return ResponseEntity.ok(ApiResponse.success("获取成功", stats));
        } catch (Exception e) {
            log.error("获取数据看板统计失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "获取失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取数据看板总览（别名，兼容前端）
     */
    @GetMapping("/dashboard/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> getDashboardOverview() {
        return getDashboardStats();
    }
    
    /**
     * 获取会员分布
     */
    @GetMapping("/dashboard/member-distribution")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DashboardStatsDTO.MemberStats>> getMemberDistribution() {
        try {
            DashboardStatsDTO stats = dashboardService.getDashboardStats();
            return ResponseEntity.ok(ApiResponse.success("获取成功", stats.getMemberStats()));
        } catch (Exception e) {
            log.error("获取会员分布失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "获取失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取用户增长趋势
     */
    @GetMapping("/dashboard/user-growth")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<TrendDataDTO>>> getUserGrowthTrend(
            @RequestParam(defaultValue = "7") int days) {
        try {
            List<TrendDataDTO> trend = dashboardService.getUserGrowthTrend(days);
            return ResponseEntity.ok(ApiResponse.success("获取成功", trend));
        } catch (Exception e) {
            log.error("获取用户增长趋势失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "获取失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取AI使用趋势
     */
    @GetMapping("/dashboard/ai-usage-trend")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<TrendDataDTO>>> getAIUsageTrend(
            @RequestParam(defaultValue = "7") int days) {
        try {
            log.info("获取AI使用趋势: days={}", days);
            List<TrendDataDTO> trend = dashboardService.getAIUsageTrend(days);
            log.info("AI使用趋势数据量: {}", trend.size());
            return ResponseEntity.ok(ApiResponse.success("获取成功", trend));
        } catch (Exception e) {
            log.error("获取AI使用趋势失败: days={}", days, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "获取失败: " + e.getMessage()));
        }
    }
    
    // ==================== 用户管理 ====================
    
    /**
     * 获取用户列表
     */
    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserManagementDTO>>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String memberLevel) {
        try {
            Page<UserManagementDTO> users = userService.getUserList(page, size, keyword, status, memberLevel);
            return ResponseEntity.ok(ApiResponse.success("获取成功", users));
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "获取失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取用户详情
     */
    @GetMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserManagementDTO>> getUserDetail(@PathVariable Long id) {
        try {
            UserManagementDTO user = userService.getUserDetail(id);
            return ResponseEntity.ok(ApiResponse.success("获取成功", user));
        } catch (Exception e) {
            log.error("获取用户详情失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "获取失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新用户状态
     */
    @PutMapping("/users/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> updateUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            userService.updateUserStatus(id, status);
            return ResponseEntity.ok(ApiResponse.success("更新成功"));
        } catch (Exception e) {
            log.error("更新用户状态失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "更新失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新用户会员等级
     */
    @PutMapping("/users/{id}/member-level")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> updateUserMemberLevel(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String memberLevel = request.get("memberLevel");
            userService.updateUserMemberLevel(id, memberLevel);
            return ResponseEntity.ok(ApiResponse.success("更新成功"));
        } catch (Exception e) {
            log.error("更新用户会员等级失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "更新失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新用户角色
     */
    @PutMapping("/users/{id}/role")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String role = request.get("role");
            userService.updateUserRole(id, role);
            return ResponseEntity.ok(ApiResponse.success("更新成功"));
        } catch (Exception e) {
            log.error("更新用户角色失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "更新失败: " + e.getMessage()));
        }
    }
    
    // ==================== AI日志管理 ====================
    
    /**
     * 获取AI日志列表
     */
    @GetMapping("/ai-logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<AIChatLogDTO>>> getAILogList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            log.info("收到AI日志查询请求: page={}, size={}, userId={}, status={}", page, size, userId, status);
            Page<AIChatLogDTO> logs = aiLogService.getAILogList(page, size, userId, status, startDate, endDate);
            log.info("返回AI日志数量: {}", logs.getTotalElements());
            return ResponseEntity.ok(ApiResponse.success("获取成功", logs));
        } catch (Exception e) {
            log.error("获取AI日志列表失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "获取失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取AI日志详情
     */
    @GetMapping("/ai-logs/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AIChatLogDTO>> getAILogDetail(@PathVariable Long id) {
        try {
            AIChatLogDTO log = aiLogService.getLogDetail(id);
            return ResponseEntity.ok(ApiResponse.success("获取成功", log));
        } catch (Exception e) {
            log.error("获取AI日志详情失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "获取失败: " + e.getMessage()));
        }
    }
    
    // ==================== 系统配置 ====================
    
    /**
     * 获取所有可用的配置选项
     */
    @GetMapping("/config/options")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<ConfigOptionDTO>>> getConfigOptions(
            @RequestParam(required = false) String category) {
        try {
            List<ConfigOptionDTO> options = category != null ?
                    configOptionsService.getConfigOptionsByCategory(category) :
                    configOptionsService.getAllConfigOptions();
            return ResponseEntity.ok(ApiResponse.success("获取成功", options));
        } catch (Exception e) {
            log.error("获取配置选项失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "获取失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取所有配置
     */
    @GetMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<SystemConfigDTO>>> getAllConfigs(
            @RequestParam(required = false) String category) {
        try {
            List<SystemConfigDTO> configs = category != null ? 
                    configService.getConfigsByCategory(category) : 
                    configService.getAllConfigs();
            return ResponseEntity.ok(ApiResponse.success("获取成功", configs));
        } catch (Exception e) {
            log.error("获取系统配置失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "获取失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新配置
     */
    @PutMapping("/config/{key}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> updateConfig(
            @PathVariable String key,
            @RequestBody Map<String, String> request) {
        try {
            String value = request.get("value");
            configService.updateConfig(key, value);
            return ResponseEntity.ok(ApiResponse.success("更新成功"));
        } catch (Exception e) {
            log.error("更新系统配置失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "更新失败: " + e.getMessage()));
        }
    }
    
    /**
     * 创建配置
     */
    @PostMapping("/config")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SystemConfigDTO>> createConfig(
            @RequestBody SystemConfigDTO dto) {
        try {
            SystemConfigDTO config = configService.createConfig(dto);
            return ResponseEntity.ok(ApiResponse.success("创建成功", config));
        } catch (Exception e) {
            log.error("创建系统配置失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "创建失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除配置
     */
    @DeleteMapping("/config/{key}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteConfig(@PathVariable String key) {
        try {
            configService.deleteConfig(key);
            log.info("删除系统配置成功: key={}", key);
            return ResponseEntity.ok(ApiResponse.success("删除成功"));
        } catch (Exception e) {
            log.error("删除系统配置失败: key={}", key, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "删除失败: " + e.getMessage()));
        }
    }
    
    // ==================== 公告管理 ====================
    
    /**
     * 获取所有公告
     */
    @GetMapping("/announcements")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<SystemAnnouncementDTO>>> getAllAnnouncements() {
        try {
            List<SystemAnnouncementDTO> announcements = configService.getAllAnnouncements();
            return ResponseEntity.ok(ApiResponse.success("获取成功", announcements));
        } catch (Exception e) {
            log.error("获取公告列表失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "获取失败: " + e.getMessage()));
        }
    }
    
    /**
     * 创建公告
     */
    @PostMapping("/announcements")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SystemAnnouncementDTO>> createAnnouncement(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody SystemAnnouncementDTO dto) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long adminId = jwtUtil.getUserIdFromToken(token);
            
            SystemAnnouncementDTO announcement = configService.createAnnouncement(dto, adminId);
            return ResponseEntity.ok(ApiResponse.success("创建成功", announcement));
        } catch (Exception e) {
            log.error("创建公告失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "创建失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新公告
     */
    @PutMapping("/announcements/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SystemAnnouncementDTO>> updateAnnouncement(
            @PathVariable Long id,
            @RequestBody SystemAnnouncementDTO dto) {
        try {
            SystemAnnouncementDTO announcement = configService.updateAnnouncement(id, dto);
            return ResponseEntity.ok(ApiResponse.success("更新成功", announcement));
        } catch (Exception e) {
            log.error("更新公告失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "更新失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除公告
     */
    @DeleteMapping("/announcements/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteAnnouncement(@PathVariable Long id) {
        try {
            configService.deleteAnnouncement(id);
            return ResponseEntity.ok(ApiResponse.success("删除成功"));
        } catch (Exception e) {
            log.error("删除公告失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "删除失败: " + e.getMessage()));
        }
    }
}
