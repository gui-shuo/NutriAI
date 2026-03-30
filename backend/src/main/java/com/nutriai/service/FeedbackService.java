package com.nutriai.service;

import com.nutriai.dto.FeedbackDTO;
import com.nutriai.entity.Feedback;
import com.nutriai.entity.User;
import com.nutriai.repository.FeedbackRepository;
import com.nutriai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * 用户提交反馈
     */
    public FeedbackDTO submitFeedback(Long userId, FeedbackDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Feedback feedback = Feedback.builder()
                .userId(userId)
                .username(user.getUsername())
                .type(dto.getType())
                .title(dto.getTitle())
                .content(dto.getContent())
                .screenshotUrls(dto.getScreenshotUrls())
                .contactInfo(dto.getContactInfo())
                .status("PENDING")
                .build();

        feedback = feedbackRepository.save(feedback);
        log.info("用户 {} 提交反馈: {}", user.getUsername(), feedback.getTitle());

        // 异步发送邮件通知管理员
        sendFeedbackNotification(feedback, user);

        return toDTO(feedback);
    }

    /**
     * 获取用户自己的反馈列表
     */
    public Page<FeedbackDTO> getUserFeedbacks(Long userId, int page, int size) {
        return feedbackRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(this::toDTO);
    }

    /**
     * 管理员获取所有反馈（支持筛选）
     */
    public Page<FeedbackDTO> getAllFeedbacks(String status, String type, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);

        if (status != null && type != null) {
            return feedbackRepository.findByStatusAndTypeOrderByCreatedAtDesc(status, type, pageRequest)
                    .map(this::toDTO);
        } else if (status != null) {
            return feedbackRepository.findByStatusOrderByCreatedAtDesc(status, pageRequest)
                    .map(this::toDTO);
        } else if (type != null) {
            return feedbackRepository.findByTypeOrderByCreatedAtDesc(type, pageRequest)
                    .map(this::toDTO);
        } else {
            return feedbackRepository.findAllByOrderByCreatedAtDesc(pageRequest)
                    .map(this::toDTO);
        }
    }

    /**
     * 管理员回复反馈
     */
    public FeedbackDTO replyFeedback(Long feedbackId, String reply, String status) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("反馈不存在"));

        feedback.setAdminReply(reply);
        feedback.setRepliedAt(LocalDateTime.now());
        if (status != null) {
            feedback.setStatus(status);
        } else {
            feedback.setStatus("RESOLVED");
        }

        feedback = feedbackRepository.save(feedback);
        log.info("管理员回复反馈 #{}: status={}", feedbackId, feedback.getStatus());

        // 异步发送邮件通知用户
        sendReplyNotificationToUser(feedback);

        return toDTO(feedback);
    }

    /**
     * 管理员更新反馈状态
     */
    public FeedbackDTO updateStatus(Long feedbackId, String status) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("反馈不存在"));

        feedback.setStatus(status);
        feedback = feedbackRepository.save(feedback);

        return toDTO(feedback);
    }

    /**
     * 获取反馈统计
     */
    public Map<String, Long> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", feedbackRepository.count());
        stats.put("pending", feedbackRepository.countByStatus("PENDING"));
        stats.put("processing", feedbackRepository.countByStatus("PROCESSING"));
        stats.put("resolved", feedbackRepository.countByStatus("RESOLVED"));
        stats.put("closed", feedbackRepository.countByStatus("CLOSED"));
        return stats;
    }

    /**
     * 异步发送反馈通知邮件
     */
    @Async
    public void sendFeedbackNotification(Feedback feedback, User user) {
        try {
            emailService.sendFeedbackNotification(
                    feedback.getTitle(),
                    feedback.getType(),
                    feedback.getContent(),
                    user.getUsername(),
                    feedback.getContactInfo()
            );
        } catch (Exception e) {
            log.error("发送反馈通知邮件失败: {}", e.getMessage());
        }
    }

    /**
     * 异步发送回复通知邮件给用户
     */
    @Async
    public void sendReplyNotificationToUser(Feedback feedback) {
        try {
            // 优先使用反馈中的联系方式（如果是邮箱），否则查找用户邮箱
            String toEmail = null;
            String contactInfo = feedback.getContactInfo();
            if (contactInfo != null && contactInfo.contains("@")) {
                toEmail = contactInfo.trim();
            }

            User user = userRepository.findById(feedback.getUserId()).orElse(null);
            String username = feedback.getUsername();

            if (toEmail == null && user != null && user.getEmail() != null && !user.getEmail().isEmpty()) {
                toEmail = user.getEmail();
            }
            if (user != null && user.getNickname() != null && !user.getNickname().isEmpty()) {
                username = user.getNickname();
            }

            if (toEmail == null) {
                log.warn("无法发送反馈回复邮件: 用户 {} 无有效邮箱", feedback.getUserId());
                return;
            }

            emailService.sendFeedbackReplyNotification(
                    toEmail, username,
                    feedback.getTitle(), feedback.getType(),
                    feedback.getContent(), feedback.getAdminReply(),
                    feedback.getStatus()
            );
            log.info("反馈回复邮件已发送: feedbackId={}, to={}", feedback.getId(), toEmail);
        } catch (Exception e) {
            log.error("发送反馈回复邮件失败: feedbackId={}, error={}", feedback.getId(), e.getMessage());
        }
    }

    private FeedbackDTO toDTO(Feedback f) {
        return FeedbackDTO.builder()
                .id(f.getId())
                .userId(f.getUserId())
                .username(f.getUsername())
                .type(f.getType())
                .title(f.getTitle())
                .content(f.getContent())
                .screenshotUrls(f.getScreenshotUrls())
                .contactInfo(f.getContactInfo())
                .status(f.getStatus())
                .adminReply(f.getAdminReply())
                .repliedAt(f.getRepliedAt())
                .createdAt(f.getCreatedAt())
                .updatedAt(f.getUpdatedAt())
                .build();
    }
}
