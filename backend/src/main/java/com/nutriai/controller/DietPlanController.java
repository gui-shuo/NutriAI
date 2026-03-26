package com.nutriai.controller;

import com.nutriai.common.ApiResponse;
import com.nutriai.dto.*;
import com.nutriai.service.DietPlanService;
import com.nutriai.service.DietPlanTaskService;
import com.nutriai.service.DietPlanHistoryService;
import com.nutriai.service.PdfExportService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 饮食计划Controller
 */
@Slf4j
@RestController
@RequestMapping("/diet-plan")
@RequiredArgsConstructor
public class DietPlanController {
    
    private final DietPlanService dietPlanService;
    private final DietPlanTaskService taskService;
    private final DietPlanHistoryService historyService;
    private final PdfExportService pdfExportService;
    
    // 线程安全的LRU缓存，最多保留100条，防止内存泄漏
    private static final int MAX_CACHE_SIZE = 100;
    private final Map<String, DietPlanResponse> planCache = Collections.synchronizedMap(
            new LinkedHashMap<String, DietPlanResponse>(MAX_CACHE_SIZE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, DietPlanResponse> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            });
    
    @PostConstruct
    public void init() {
        log.info("✓✓✓ DietPlanController已加载并初始化！");
        log.info("✓✓✓ 映射路径: /diet-plan (context-path=/api)");
    }
    
    /**
     * 生成饮食计划（异步）
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateDietPlan(
            @Valid @RequestBody DietPlanRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("收到饮食计划生成请求: {} 天, 目标: {}", request.getDays(), request.getGoal());
        
        Long userId = getUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "用户未登录"));
        }
        
        try {
            String taskId = taskService.createTask(userId, request);
            
            Map<String, String> result = new HashMap<>();
            result.put("taskId", taskId);
            result.put("status", "pending");
            
            return ResponseEntity.ok(ApiResponse.success("任务已创建", result));
            
        } catch (Exception e) {
            log.error("创建任务失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "创建任务失败，请稍后重试"));
        }
    }
    
    /**
     * 查询任务状态
     */
    @GetMapping("/task/{taskId}/status")
    public ResponseEntity<ApiResponse<TaskStatusResponse>> getTaskStatus(
            @PathVariable String taskId) {
        
        TaskStatusResponse status = taskService.getTaskStatus(taskId);
        
        if (status == null) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(404, "任务不存在"));
        }
        
        return ResponseEntity.ok(ApiResponse.success("获取成功", status));
    }
    
    /**
     * 取消任务
     */
    @PostMapping("/task/{taskId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelTask(
            @PathVariable String taskId,
            HttpServletRequest httpRequest) {
        
        Long userId = getUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "用户未登录"));
        }
        
        try {
            boolean cancelled = taskService.cancelTask(taskId, userId);
            
            if (cancelled) {
                return ResponseEntity.ok(ApiResponse.success("任务已取消", null));
            } else {
                return ResponseEntity.status(400)
                        .body(ApiResponse.error(400, "无法取消任务"));
            }
            
        } catch (Exception e) {
            log.error("取消任务失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "取消任务失败，请稍后重试"));
        }
    }
    
    /**
     * 获取历史记录列表
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<HistoryListItem>>> getHistory(
            HttpServletRequest httpRequest,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Long userId = getUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "用户未登录"));
        }
        
        try {
            Page<HistoryListItem> history = historyService.getHistoryList(userId, page, size);
            return ResponseEntity.ok(ApiResponse.success("获取成功", history));
        } catch (Exception e) {
            log.error("获取历史记录失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "获取历史记录失败，请稍后重试"));
        }
    }
    
    /**
     * 获取历史记录详情
     */
    @GetMapping("/history/{planId}")
    public ResponseEntity<ApiResponse<DietPlanResponse>> getHistoryDetail(
            @PathVariable String planId,
            HttpServletRequest httpRequest) {
        
        Long userId = getUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "用户未登录"));
        }
        
        try {
            DietPlanResponse detail = historyService.getHistoryDetail(planId, userId);
            
            if (detail == null) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "计划不存在"));
            }
            
            planCache.put(planId, detail);
            return ResponseEntity.ok(ApiResponse.success("获取成功", detail));
        } catch (Exception e) {
            log.error("获取历史记录详情失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "获取历史记录详情失败，请稍后重试"));
        }
    }
    
    /**
     * 删除历史记录
     */
    @DeleteMapping("/{planId}")
    public ResponseEntity<ApiResponse<Void>> deleteHistory(
            @PathVariable String planId,
            HttpServletRequest httpRequest) {
        
        Long userId = getUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "用户未登录"));
        }
        
        try {
            boolean deleted = historyService.deleteHistory(planId, userId);
            
            if (deleted) {
                planCache.remove(planId);
                return ResponseEntity.ok(ApiResponse.success("删除成功", null));
            } else {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "计划不存在"));
            }
        } catch (Exception e) {
            log.error("删除历史记录失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "删除失败，请稍后重试"));
        }
    }
    
    /**
     * 导出饮食计划为PDF
     */
    @GetMapping("/export-pdf/{planId}")
    public ResponseEntity<?> exportPdf(
            @PathVariable String planId,
            HttpServletRequest httpRequest) {
        
        log.info("收到PDF导出请求: planId={}", planId);
        
        Long userId = getUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "用户未登录"));
        }
        
        try {
            DietPlanResponse plan = planCache.get(planId);
            if (plan == null) {
                log.info("缓存未命中，从数据库加载: planId={}", planId);
                plan = historyService.getHistoryDetail(planId, userId);
                if (plan != null) {
                    planCache.put(planId, plan);
                }
            }
            
            if (plan == null) {
                log.error("计划不存在: planId={}", planId);
                return ResponseEntity.status(404)
                    .body(ApiResponse.error(404, "计划不存在或已过期，请重新生成计划"));
            }
            
            log.info("找到计划: {}, 开始生成PDF", plan.getTitle());
            
            byte[] pdfBytes = pdfExportService.exportDietPlanToPdf(plan);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                ContentDisposition.attachment()
                    .filename(plan.getTitle() + ".pdf", StandardCharsets.UTF_8)
                    .build()
            );
            
            log.info("PDF导出成功: {} ({} bytes)", plan.getTitle(), pdfBytes.length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
                
        } catch (Exception e) {
            log.error("PDF导出失败: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error(500, "导出失败，请稍后重试"));
        }
    }
    
    /**
     * 收藏/取消收藏饮食计划
     */
    @PostMapping("/{planId}/favorite")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleFavorite(
            @PathVariable String planId,
            HttpServletRequest httpRequest) {
        
        Long userId = getUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "用户未登录"));
        }
        
        try {
            boolean isFavorite = historyService.toggleFavorite(planId, userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("planId", planId);
            result.put("isFavorite", isFavorite);
            
            return ResponseEntity.ok(ApiResponse.success(
                    isFavorite ? "已收藏" : "已取消收藏", result));
            
        } catch (Exception e) {
            log.error("收藏操作失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "操作失败，请稍后重试"));
        }
    }
    
    /**
     * 获取收藏列表
     */
    @GetMapping("/favorites")
    public ResponseEntity<ApiResponse<Page<HistoryListItem>>> getFavorites(
            HttpServletRequest httpRequest,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Long userId = getUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "用户未登录"));
        }
        
        try {
            Page<HistoryListItem> favorites = historyService.getFavoriteList(userId, page, size);
            return ResponseEntity.ok(ApiResponse.success("获取成功", favorites));
        } catch (Exception e) {
            log.error("获取收藏列表失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "获取收藏列表失败，请稍后重试"));
        }
    }
    
    /**
     * 对已生成的饮食计划提出修改建议
     */
    @PostMapping("/{planId}/modify")
    public ResponseEntity<ApiResponse<Map<String, String>>> modifyDietPlan(
            @PathVariable String planId,
            HttpServletRequest httpRequest,
            @RequestBody Map<String, String> body) {
        
        String suggestion = body.get("suggestion");
        if (suggestion == null || suggestion.trim().isEmpty()) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error(400, "修改建议不能为空"));
        }
        
        Long userId = getUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "用户未登录"));
        }
        
        try {
            DietPlanResponse originalPlan = historyService.getHistoryDetail(planId, userId);
            if (originalPlan == null) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "计划不存在"));
            }
            
            String taskId = taskService.createModifyTask(userId, planId, originalPlan, suggestion);
            
            Map<String, String> result = new HashMap<>();
            result.put("taskId", taskId);
            result.put("status", "pending");
            
            return ResponseEntity.ok(ApiResponse.success("修改任务已创建", result));
            
        } catch (Exception e) {
            log.error("创建修改任务失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "创建修改任务失败，请稍后重试"));
        }
    }
    
    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
}
