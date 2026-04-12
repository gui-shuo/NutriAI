package com.nutriai.service;

import com.nutriai.dto.admin.SystemAnnouncementDTO;
import com.nutriai.entity.AnnouncementRead;
import com.nutriai.entity.SystemAnnouncement;
import com.nutriai.repository.AnnouncementReadRepository;
import com.nutriai.repository.SystemAnnouncementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 公告服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementService {
    
    private final SystemAnnouncementRepository announcementRepository;
    private final AnnouncementReadRepository announcementReadRepository;
    
    /**
     * 获取公开的公告列表（分页），不含已读状态
     */
    public Page<SystemAnnouncementDTO> getPublicAnnouncements(int page, int size) {
        return getPublicAnnouncements(page, size, null);
    }

    /**
     * 获取公开的公告列表（分页），含已读状态
     */
    public Page<SystemAnnouncementDTO> getPublicAnnouncements(int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "priority", "createdAt"));
        
        Page<SystemAnnouncement> announcementPage = announcementRepository
                .findByIsActiveTrueOrderByPriorityDescCreatedAtDesc(pageable);
        
        Set<Long> readIds = userId != null
                ? announcementReadRepository.findReadAnnouncementIdsByUserId(userId)
                : Set.of();
        
        return announcementPage.map(a -> convertToDTO(a, readIds.contains(a.getId())));
    }
    
    /**
     * 根据ID获取公告
     */
    public SystemAnnouncementDTO getAnnouncementById(Long id) {
        return announcementRepository.findById(id)
                .map(a -> convertToDTO(a, false))
                .orElse(null);
    }

    /**
     * 获取未读公告数量
     */
    public long getUnreadCount(Long userId) {
        return announcementReadRepository.countUnreadByUserId(userId);
    }

    /**
     * 标记公告为已读
     */
    @Transactional
    public void markAsRead(Long userId, Long announcementId) {
        if (announcementReadRepository.findByUserIdAndAnnouncementId(userId, announcementId).isEmpty()) {
            AnnouncementRead read = AnnouncementRead.builder()
                    .userId(userId)
                    .announcementId(announcementId)
                    .build();
            announcementReadRepository.save(read);
        }
    }

    /**
     * 标记所有公告为已读
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        Set<Long> readIds = announcementReadRepository.findReadAnnouncementIdsByUserId(userId);
        announcementRepository.findByIsActiveTrueOrderByPriorityDescCreatedAtDesc()
                .stream()
                .filter(a -> !readIds.contains(a.getId()))
                .forEach(a -> {
                    AnnouncementRead read = AnnouncementRead.builder()
                            .userId(userId)
                            .announcementId(a.getId())
                            .build();
                    announcementReadRepository.save(read);
                });
    }
    
    private SystemAnnouncementDTO convertToDTO(SystemAnnouncement announcement, boolean isRead) {
        return SystemAnnouncementDTO.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .content(announcement.getContent())
                .type(announcement.getType())
                .priority(announcement.getPriority())
                .isActive(announcement.getIsActive())
                .startTime(announcement.getStartTime())
                .endTime(announcement.getEndTime())
                .createdAt(announcement.getCreatedAt())
                .updatedAt(announcement.getUpdatedAt())
                .isRead(isRead)
                .build();
    }
}
