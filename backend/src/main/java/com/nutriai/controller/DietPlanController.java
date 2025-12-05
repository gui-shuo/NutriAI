package com.nutriai.controller;

import com.nutriai.common.ApiResponse;
import com.nutriai.dto.*;
import com.nutriai.service.DietPlanService;
import com.nutriai.service.DietPlanTaskService;
import com.nutriai.service.DietPlanHistoryService;
import com.nutriai.service.PdfExportService;
import com.nutriai.util.JwtUtil;
import jakarta.annotation.PostConstruct;
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
import java.util.HashMap;
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
    private final JwtUtil jwtUtil;
    
    // 临时存储生成的计划（实际项目中应该存储到数据库）
    private final Map<String, DietPlanResponse> planCache = new HashMap<>();
    
    @PostConstruct
    public void init() {
        log.info("✓✓✓ DietPlanController已加载并初始化！");
        log.info("✓✓✓ 映射路径: /diet-plan (context-path=/api)");
    }
    
    /**
     * 测试端点
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        log.info("测试端点被调用");
        return ResponseEntity.ok("DietPlanController is working!");
    }
    
    /**
     * 生成饮食计划（异步）
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateDietPlan(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody DietPlanRequest request) {
        
        log.info("收到饮食计划生成请求: {} 天, 目标: {}", request.getDays(), request.getGoal());
        
        try {
            // 从token获取用户ID
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(token);
            
            // 创建异步任务
            String taskId = taskService.createTask(userId, request);
            
            Map<String, String> result = new HashMap<>();
            result.put("taskId", taskId);
            result.put("status", "pending");
            
            return ResponseEntity.ok(ApiResponse.success("任务已创建", result));
            
        } catch (Exception e) {
            log.error("创建任务失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "创建任务失败: " + e.getMessage()));
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
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(token);
            
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
                    .body(ApiResponse.error(500, "取消任务失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取历史记录列表
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<HistoryListItem>>> getHistory(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(token);
            
            Page<HistoryListItem> history = historyService.getHistoryList(userId, page, size);
            
            return ResponseEntity.ok(ApiResponse.success("获取成功", history));
            
        } catch (Exception e) {
            log.error("获取历史记录失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "获取历史记录失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取历史记录详情
     */
    @GetMapping("/history/{planId}")
    public ResponseEntity<ApiResponse<DietPlanResponse>> getHistoryDetail(
            @PathVariable String planId,
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(token);
            
            DietPlanResponse detail = historyService.getHistoryDetail(planId, userId);
            
            if (detail == null) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "计划不存在"));
            }
            
            // 同时缓存到planCache，用于PDF导出
            planCache.put(planId, detail);
            
            return ResponseEntity.ok(ApiResponse.success("获取成功", detail));
            
        } catch (Exception e) {
            log.error("获取历史记录详情失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "获取历史记录详情失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除历史记录
     */
    @DeleteMapping("/{planId}")
    public ResponseEntity<ApiResponse<Void>> deleteHistory(
            @PathVariable String planId,
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(token);
            
            boolean deleted = historyService.deleteHistory(planId, userId);
            
            if (deleted) {
                // 同时从缓存中删除
                planCache.remove(planId);
                return ResponseEntity.ok(ApiResponse.success("删除成功", null));
            } else {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "计划不存在"));
            }
            
        } catch (Exception e) {
            log.error("删除历史记录失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "删除失败: " + e.getMessage()));
        }
    }
    
    /**
     * 导出饮食计划为PDF（会员功能）
     */
    @GetMapping("/export-pdf/{planId}")
    public ResponseEntity<?> exportPdf(
            @PathVariable String planId,
            @RequestHeader("Authorization") String authHeader) {
        
        log.info("收到PDF导出请求: planId={}", planId);
        log.info("当前缓存中的计划数量: {}", planCache.size());
        log.info("缓存中的planId列表: {}", planCache.keySet());
        
        try {
            // 验证用户身份
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(token);
            log.info("用户ID: {}", userId);
            
            // TODO: 检查会员权限
            // if (!membershipService.isGoldMember(userId)) {
            //     return ResponseEntity.status(403)
            //         .body(ApiResponse.error(403, "此功能仅限黄金会员使用"));
            // }
            
            // 从缓存获取计划
            DietPlanResponse plan = planCache.get(planId);
            if (plan == null) {
                log.error("计划不存在: planId={}, 缓存中有: {}", planId, planCache.keySet());
                return ResponseEntity.status(404)
                    .body(ApiResponse.error(404, "计划不存在或已过期，请重新生成计划"));
            }
            
            log.info("找到计划: {}, 开始生成PDF", plan.getTitle());
            
            // 生成PDF
            byte[] pdfBytes = pdfExportService.exportDietPlanToPdf(plan);
            
            log.info("PDF生成成功，大小: {} bytes", pdfBytes.length);
            
            // 设置响应头
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
            log.error("异常类型: {}", e.getClass().getName());
            if (e.getCause() != null) {
                log.error("异常原因: {}", e.getCause().getMessage());
            }
            return ResponseEntity.status(500)
                .body(ApiResponse.error(500, "导出失败: " + e.getMessage()));
        }
    }
}
