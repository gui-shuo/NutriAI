package com.nutriai.controller;

import com.nutriai.common.ApiResponse;
import com.nutriai.dto.AIChatFavoriteDTO;
import com.nutriai.dto.AIChatHistoryDTO;
import com.nutriai.service.AIChatService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI聊天Controller
 */
@Slf4j
@RestController
@RequestMapping("/ai-chat")
@RequiredArgsConstructor
public class AIChatController {
    
    private final AIChatService aiChatService;
    
    /**
     * 保存或更新聊天历史
     */
    @PostMapping("/history")
    public ResponseEntity<ApiResponse<AIChatHistoryDTO>> saveHistory(
            HttpServletRequest httpRequest,
            @RequestBody Map<String, Object> request) {
        
        Long userId = getUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "用户未登录"));
        }
        
        try {
            String title = (String) request.get("title");
            String messages = (String) request.get("messages");
            Long historyId = null;
            if (request.get("id") != null) {
                try {
                    historyId = Long.parseLong(request.get("id").toString());
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error(400, "无效的历史记录ID"));
                }
            }
            
            AIChatHistoryDTO history = aiChatService.saveHistory(userId, historyId, title, messages);
            return ResponseEntity.ok(ApiResponse.success("保存成功", history));
        } catch (Exception e) {
            log.error("保存聊天历史失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "保存失败，请稍后重试"));
        }
    }
    
    /**
     * 获取历史记录列表（分页）
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<AIChatHistoryDTO>>> getHistoryList(
            HttpServletRequest httpRequest,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Long userId = getUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "用户未登录"));
        }
        
        try {
            Page<AIChatHistoryDTO> history = aiChatService.getHistoryList(userId, page, size);
            return ResponseEntity.ok(ApiResponse.success("获取成功", history));
        } catch (Exception e) {
            log.error("获取历史记录失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "获取失败，请稍后重试"));
        }
    }
    
    /**
     * 获取所有历史记录
     */
    @GetMapping("/history/all")
    public ResponseEntity<ApiResponse<List<AIChatHistoryDTO>>> getAllHistory(
            HttpServletRequest httpRequest) {
        
        Long userId = getUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "用户未登录"));
        }
        
        try {
            List<AIChatHistoryDTO> history = aiChatService.getAllHistory(userId);
            return ResponseEntity.ok(ApiResponse.success("获取成功", history));
        } catch (Exception e) {
            log.error("获取所有历史记录失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "获取失败，请稍后重试"));
        }
    }
    
    /**
     * 获取历史记录详情
     */
    @GetMapping("/history/{id}")
    public ResponseEntity<ApiResponse<AIChatHistoryDTO>> getHistoryDetail(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        
        Long userId = getUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "用户未登录"));
        }
        
        try {
            AIChatHistoryDTO history = aiChatService.getHistoryDetail(id, userId);
            if (history == null) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "历史记录不存在"));
            }
            return ResponseEntity.ok(ApiResponse.success("获取成功", history));
        } catch (Exception e) {
            log.error("获取历史记录详情失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "获取失败，请稍后重试"));
        }
    }
    
    /**
     * 删除历史记录
     */
    @DeleteMapping("/history/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHistory(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        
        Long userId = getUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "用户未登录"));
        }
        
        try {
            boolean deleted = aiChatService.deleteHistory(id, userId);
            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success("删除成功", null));
            } else {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "历史记录不存在"));
            }
        } catch (Exception e) {
            log.error("删除历史记录失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "删除失败，请稍后重试"));
        }
    }
    
    /**
     * 添加收藏
     */
    @PostMapping("/favorite")
    public ResponseEntity<ApiResponse<AIChatFavoriteDTO>> addFavorite(
            HttpServletRequest httpRequest,
            @RequestBody Map<String, String> request) {
        
        Long userId = getUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "用户未登录"));
        }
        
        try {
            String messageContent = request.get("messageContent");
            String messageRole = request.get("messageRole");
            
            if (messageContent == null || messageContent.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "收藏内容不能为空"));
            }
            
            AIChatFavoriteDTO favorite = aiChatService.addFavorite(userId, messageContent, messageRole);
            return ResponseEntity.ok(ApiResponse.success("收藏成功", favorite));
        } catch (Exception e) {
            log.error("添加收藏失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "收藏失败，请稍后重试"));
        }
    }
    
    /**
     * 获取收藏列表（分页）
     */
    @GetMapping("/favorite")
    public ResponseEntity<ApiResponse<Page<AIChatFavoriteDTO>>> getFavoriteList(
            HttpServletRequest httpRequest,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Long userId = getUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "用户未登录"));
        }
        
        try {
            Page<AIChatFavoriteDTO> favorites = aiChatService.getFavoriteList(userId, page, size);
            return ResponseEntity.ok(ApiResponse.success("获取成功", favorites));
        } catch (Exception e) {
            log.error("获取收藏列表失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "获取失败，请稍后重试"));
        }
    }
    
    /**
     * 获取所有收藏
     */
    @GetMapping("/favorite/all")
    public ResponseEntity<ApiResponse<List<AIChatFavoriteDTO>>> getAllFavorites(
            HttpServletRequest httpRequest) {
        
        Long userId = getUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "用户未登录"));
        }
        
        try {
            List<AIChatFavoriteDTO> favorites = aiChatService.getAllFavorites(userId);
            return ResponseEntity.ok(ApiResponse.success("获取成功", favorites));
        } catch (Exception e) {
            log.error("获取所有收藏失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "获取失败，请稍后重试"));
        }
    }
    
    /**
     * 删除收藏
     */
    @DeleteMapping("/favorite/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFavorite(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        
        Long userId = getUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "用户未登录"));
        }
        
        try {
            boolean deleted = aiChatService.deleteFavorite(id, userId);
            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success("删除成功", null));
            } else {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "收藏不存在"));
            }
        } catch (Exception e) {
            log.error("删除收藏失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "删除失败，请稍后重试"));
        }
    }
    
    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
}
