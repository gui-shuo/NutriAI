package com.nutriai.service;

import com.nutriai.dto.AIChatFavoriteDTO;
import com.nutriai.dto.AIChatHistoryDTO;
import com.nutriai.entity.AIChatFavorite;
import com.nutriai.entity.AIChatHistory;
import com.nutriai.repository.AIChatFavoriteRepository;
import com.nutriai.repository.AIChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI聊天服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIChatService {
    
    private final AIChatHistoryRepository historyRepository;
    private final AIChatFavoriteRepository favoriteRepository;
    
    /**
     * 保存或更新聊天历史
     */
    @Transactional
    public AIChatHistoryDTO saveHistory(Long userId, Long historyId, String title, String messages) {
        AIChatHistory history;
        
        if (historyId != null) {
            // 更新现有记录
            history = historyRepository.findByIdAndUserId(historyId, userId)
                    .orElseThrow(() -> new RuntimeException("历史记录不存在"));
            history.setTitle(title);
            history.setMessages(messages);
            log.info("更新聊天历史: userId={}, historyId={}", userId, historyId);
        } else {
            // 创建新记录
            history = AIChatHistory.builder()
                    .userId(userId)
                    .title(title)
                    .messages(messages)
                    .build();
            log.info("创建聊天历史: userId={}", userId);
        }
        
        history = historyRepository.save(history);
        log.info("保存聊天历史成功: historyId={}", history.getId());
        
        return toHistoryDTO(history);
    }
    
    /**
     * 获取历史记录列表
     */
    public Page<AIChatHistoryDTO> getHistoryList(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size);
        return historyRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toHistoryDTO);
    }
    
    /**
     * 获取所有历史记录
     */
    public List<AIChatHistoryDTO> getAllHistory(Long userId) {
        return historyRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toHistoryDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取历史记录详情
     */
    public AIChatHistoryDTO getHistoryDetail(Long id, Long userId) {
        return historyRepository.findByIdAndUserId(id, userId)
                .map(this::toHistoryDTO)
                .orElse(null);
    }
    
    /**
     * 删除历史记录
     */
    @Transactional
    public boolean deleteHistory(Long id, Long userId) {
        return historyRepository.findByIdAndUserId(id, userId)
                .map(history -> {
                    historyRepository.delete(history);
                    log.info("删除聊天历史: userId={}, historyId={}", userId, id);
                    return true;
                })
                .orElse(false);
    }
    
    /**
     * 添加收藏
     */
    @Transactional
    public AIChatFavoriteDTO addFavorite(Long userId, String messageContent, String messageRole) {
        AIChatFavorite favorite = AIChatFavorite.builder()
                .userId(userId)
                .messageContent(messageContent)
                .messageRole(messageRole)
                .build();
        
        favorite = favoriteRepository.save(favorite);
        log.info("添加收藏: userId={}, favoriteId={}", userId, favorite.getId());
        
        return toFavoriteDTO(favorite);
    }
    
    /**
     * 获取收藏列表
     */
    public Page<AIChatFavoriteDTO> getFavoriteList(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size);
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toFavoriteDTO);
    }
    
    /**
     * 获取所有收藏
     */
    public List<AIChatFavoriteDTO> getAllFavorites(Long userId) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toFavoriteDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 删除收藏
     */
    @Transactional
    public boolean deleteFavorite(Long id, Long userId) {
        return favoriteRepository.findByIdAndUserId(id, userId)
                .map(favorite -> {
                    favoriteRepository.delete(favorite);
                    log.info("删除收藏: userId={}, favoriteId={}", userId, id);
                    return true;
                })
                .orElse(false);
    }
    
    // 转换方法
    private AIChatHistoryDTO toHistoryDTO(AIChatHistory history) {
        return AIChatHistoryDTO.builder()
                .id(history.getId())
                .title(history.getTitle())
                .messages(history.getMessages())
                .createdAt(history.getCreatedAt())
                .updatedAt(history.getUpdatedAt())
                .build();
    }
    
    private AIChatFavoriteDTO toFavoriteDTO(AIChatFavorite favorite) {
        return AIChatFavoriteDTO.builder()
                .id(favorite.getId())
                .messageContent(favorite.getMessageContent())
                .messageRole(favorite.getMessageRole())
                .createdAt(favorite.getCreatedAt())
                .build();
    }
}
