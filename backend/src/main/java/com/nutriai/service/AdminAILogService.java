package com.nutriai.service;

import com.nutriai.dto.admin.AIChatLogDTO;
import com.nutriai.entity.AIChatLog;
import com.nutriai.entity.User;
import com.nutriai.repository.AIChatLogRepository;
import com.nutriai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理后台AI日志服务 — 直接查询 ai_chat_log 表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAILogService {
    
    private final AIChatLogRepository chatLogRepository;
    private final UserRepository userRepository;

    // 用户名缓存（避免 N+1 查询）
    private final Map<Long, String> usernameCache = new ConcurrentHashMap<>();
    
    /**
     * 分页查询AI日志
     */
    public Page<AIChatLogDTO> getAILogList(int page, int size, Long userId, String status, 
                                           LocalDateTime startDate, LocalDateTime endDate) {
        log.info("查询AI日志: page={}, size={}, userId={}, status={}", page, size, userId, status);
        
        Pageable pageable = PageRequest.of(page - 1, size);
        
        Page<AIChatLog> logPage = chatLogRepository.findByConditions(userId, status, startDate, endDate, pageable);
        
        log.info("查询到 {} 条AI日志", logPage.getTotalElements());
        
        return logPage.map(this::convertToDTO);
    }
    
    /**
     * 获取日志详情
     */
    public AIChatLogDTO getLogDetail(Long logId) {
        AIChatLog chatLog = chatLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("日志不存在"));
        return convertToDTO(chatLog);
    }
    
    private AIChatLogDTO convertToDTO(AIChatLog logEntry) {
        String username = usernameCache.computeIfAbsent(logEntry.getUserId(), uid -> {
            Optional<User> u = userRepository.findById(uid);
            return u.map(User::getUsername).orElse("未知用户");
        });
        
        return AIChatLogDTO.builder()
                .id(logEntry.getId())
                .userId(logEntry.getUserId())
                .username(username)
                .sessionId(logEntry.getSessionId())
                .userMessage(logEntry.getUserMessage())
                .aiResponse(logEntry.getAiResponse())
                .model(logEntry.getModel())
                .tokensUsed(logEntry.getTokensUsed())
                .responseTime(logEntry.getResponseTime())
                .status(logEntry.getStatus())
                .errorMessage(logEntry.getErrorMessage())
                .createdAt(logEntry.getCreatedAt())
                .build();
    }
}
