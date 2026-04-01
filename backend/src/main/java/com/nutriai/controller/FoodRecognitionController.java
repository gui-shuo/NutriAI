package com.nutriai.controller;

import com.nutriai.common.ApiResponse;
import com.nutriai.dto.FoodRecognitionResult;
import com.nutriai.entity.FoodRecognitionHistory;
import com.nutriai.service.FoodRecognitionServiceV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

/**
 * 食物识别Controller
 */
@Slf4j
@RestController
@RequestMapping("/food-recognition")
@RequiredArgsConstructor
public class FoodRecognitionController {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp");
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024L; // 5MB

    private final FoodRecognitionServiceV2 recognitionService;

    /** 从 SecurityContext 获取已认证用户ID（JwtAuthenticationFilter 已解析并注入） */
    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /**
     * 通过食物名称识别（文本输入）
     */
    @PostMapping("/recognize-by-name")
    public ResponseEntity<ApiResponse<FoodRecognitionResult>> recognizeByName(
            @RequestParam("foodName") String foodName) {

        String trimmed = foodName == null ? "" : foodName.trim();
        if (trimmed.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "食物名称不能为空"));
        }
        if (trimmed.length() > 100) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "食物名称过长"));
        }

        log.info("收到食物名称识别请求: {}", trimmed);

        try {
            Long userId = getCurrentUserId();
            FoodRecognitionResult result = recognitionService.recognizeByName(userId, trimmed);
            return ResponseEntity.ok(ApiResponse.success("识别成功", result));

        } catch (Exception e) {
            log.error("食物名称识别失败, foodName={}", trimmed, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "识别失败，请稍后重试"));
        }
    }

    /**
     * 通过图片识别 - 对接天聚数行食物营养识别API
     */
    @PostMapping("/recognize-by-image")
    public ResponseEntity<ApiResponse<FoodRecognitionResult>> recognizeByImage(
            @RequestParam("image") MultipartFile image) {

        // 服务端文件安全校验
        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "请选择图片文件"));
        }
        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "仅支持 JPG/PNG/GIF/WebP/BMP 格式"));
        }
        if (image.getSize() > MAX_IMAGE_SIZE) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "图片大小不能超过 5MB"));
        }

        log.info("收到图片识别请求，文件大小: {} bytes，类型: {}", image.getSize(), contentType);

        try {
            Long userId = getCurrentUserId();
            FoodRecognitionResult result = recognitionService.recognizeByImage(userId, image);
            return ResponseEntity.ok(ApiResponse.success("识别成功", result));

        } catch (UnsupportedOperationException e) {
            log.warn("图片识别功能未配置: {}", e.getMessage());
            return ResponseEntity.status(501).body(ApiResponse.error(501, e.getMessage()));

        } catch (IllegalStateException e) {
            // 未识别到食物（业务异常，返回200+自定义code以便前端区分）
            log.info("图片未能识别到食物: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(4001, e.getMessage()));

        } catch (Exception e) {
            log.error("图片识别失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "识别失败，请稍后重试"));
        }
    }

    /**
     * 获取识别历史
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<FoodRecognitionHistory>>> getHistory() {

        try {
            Long userId = getCurrentUserId();
            List<FoodRecognitionHistory> history = recognitionService.getHistory(userId);
            return ResponseEntity.ok(ApiResponse.success(history));
            
        } catch (Exception e) {
            log.error("获取识别历史失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "获取历史失败，请稍后重试"));
        }
    }

    /**
     * 删除识别历史记录
     */
    @DeleteMapping("/history/{id}")
    public ResponseEntity<ApiResponse<String>> deleteHistory(@PathVariable Long id) {

        try {
            Long userId = getCurrentUserId();
            recognitionService.deleteHistory(id, userId);
            return ResponseEntity.ok(ApiResponse.success("删除成功"));

        } catch (RuntimeException e) {
            log.warn("删除识别历史失败: id={}, reason={}", id, e.getMessage());
            return ResponseEntity.status(403)
                    .body(ApiResponse.error(403, e.getMessage()));

        } catch (Exception e) {
            log.error("删除识别历史失败: id={}", id, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "删除失败，请稍后重试"));
        }
    }
}
