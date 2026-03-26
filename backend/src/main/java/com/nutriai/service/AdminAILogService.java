package com.nutriai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriai.dto.admin.AIChatLogDTO;
import com.nutriai.entity.AIChatHistory;
import com.nutriai.entity.AIChatLog;
import com.nutriai.entity.User;
import com.nutriai.repository.AIChatHistoryRepository;
import com.nutriai.repository.AIChatLogRepository;
import com.nutriai.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 管理后台AI日志服务
 */
@Slf4j
@Service
public class AdminAILogService {
    
    private final AIChatLogRepository chatLogRepository;
    private final AIChatHistoryRepository chatHistoryRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public AdminAILogService(AIChatLogRepository chatLogRepository,
                             AIChatHistoryRepository chatHistoryRepository,
                             UserRepository userRepository,
                             ObjectMapper objectMapper) {
        this.chatLogRepository = chatLogRepository;
        this.chatHistoryRepository = chatHistoryRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 分页查询AI日志（从ai_chat_history表查询）
     */
    public Page<AIChatLogDTO> getAILogList(int page, int size, Long userId, String status, 
                                           LocalDateTime startDate, LocalDateTime endDate) {
        log.info("查询AI日志: page={}, size={}, userId={}, status={}", page, size, userId, status);
        
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        
        // 查询ai_chat_history表
        Page<AIChatHistory> historyPage;
        if (userId != null) {
            historyPage = chatHistoryRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageable);
        } else {
            historyPage = chatHistoryRepository.findAllByOrderByUpdatedAtDesc(pageable);
        }
        
        log.info("查询到 {} 条会话历史", historyPage.getTotalElements());
        
        // 将会话历史转换为日志DTO列表
        List<AIChatLogDTO> logDTOs = new ArrayList<>();
        for (AIChatHistory history : historyPage.getContent()) {
            logDTOs.addAll(convertHistoryToLogDTOs(history));
        }
        
        // 创建新的Page对象
        return new PageImpl<>(logDTOs, pageable, historyPage.getTotalElements());
    }
    
    /**
     * 获取日志详情
     */
    public AIChatLogDTO getLogDetail(Long logId) {
        AIChatLog log = chatLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("日志不存在"));
        return convertToDTO(log);
    }
    
    /**
     * 将会话历史转换为日志DTO列表
     */
    private List<AIChatLogDTO> convertHistoryToLogDTOs(AIChatHistory history) {
        List<AIChatLogDTO> dtos = new ArrayList<>();
        
        if (history.getMessages() == null || history.getMessages().isBlank()) {
            log.warn("会话历史消息为空: historyId={}", history.getId());
            return dtos;
        }
        
        try {
            // 解析messages JSON
            JsonNode messagesNode = objectMapper.readTree(history.getMessages());
            
            // 查询用户名
            String username = null;
            Optional<User> userOpt = userRepository.findById(history.getUserId());
            if (userOpt.isPresent()) {
                username = userOpt.get().getUsername();
            }
            
            // 遍历消息，创建日志DTO
            if (messagesNode.isArray()) {
                String lastUserMessage = null;
                for (JsonNode messageNode : messagesNode) {
                    JsonNode roleNode = messageNode.get("role");
                    JsonNode contentNode = messageNode.get("content");
                    if (roleNode == null || contentNode == null) continue;
                    
                    String role = roleNode.asText();
                    String content = contentNode.asText();
                    
                    if ("user".equals(role)) {
                        lastUserMessage = content;
                    } else if ("assistant".equals(role) && lastUserMessage != null) {
                        // 创建一条日志记录
                        AIChatLogDTO dto = AIChatLogDTO.builder()
                                .id(history.getId())  // 使用会话ID
                                .userId(history.getUserId())
                                .username(username)
                                .sessionId(history.getId().toString())
                                .userMessage(lastUserMessage)
                                .aiResponse(content)
                                .model("qwen-max")  // 默认模型
                                .tokensUsed(0)  // 历史数据没有token信息
                                .responseTime(0)  // 历史数据没有响应时间
                                .status("success")
                                .errorMessage(null)
                                .createdAt(history.getUpdatedAt())
                                .build();
                        dtos.add(dto);
                        lastUserMessage = null;
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析会话历史失败: historyId={}", history.getId(), e);
        }
        
        return dtos;
    }
    
    /**
     * 转换为DTO
     */
    private AIChatLogDTO convertToDTO(AIChatLog log) {
        // 查询用户名
        String username = null;
        Optional<User> userOpt = userRepository.findById(log.getUserId());
        if (userOpt.isPresent()) {
            username = userOpt.get().getUsername();
        }
        
        return AIChatLogDTO.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .username(username)
                .sessionId(log.getSessionId())
                .userMessage(log.getUserMessage())
                .aiResponse(log.getAiResponse())
                .model(log.getModel())
                .tokensUsed(log.getTokensUsed())
                .responseTime(log.getResponseTime())
                .status(log.getStatus())
                .errorMessage(log.getErrorMessage())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
