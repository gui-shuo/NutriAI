package com.nutriai.service;

import com.nutriai.entity.ConsultationOrder;
import com.nutriai.entity.Nutritionist;
import com.nutriai.exception.BusinessException;
import com.nutriai.repository.ConsultationOrderRepository;
import com.nutriai.repository.NutritionistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 营养师咨询服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationService {

    private final NutritionistRepository nutritionistRepository;
    private final ConsultationOrderRepository consultationOrderRepository;
    private final MemberService memberService;
    private final TencentImService tencentImService;

    /**
     * 获取所有启用的营养师列表
     */
    public List<Nutritionist> getActiveNutritionists() {
        return nutritionistRepository.findByIsActiveTrueOrderBySortOrderAsc();
    }

    /**
     * 获取在线营养师列表
     */
    public List<Nutritionist> getOnlineNutritionists() {
        return nutritionistRepository.findByIsActiveTrueAndStatusOrderBySortOrderAsc("ONLINE");
    }

    /**
     * 获取营养师详情
     */
    public Nutritionist getNutritionistDetail(Long id) {
        return nutritionistRepository.findById(id)
                .orElseThrow(() -> new BusinessException("营养师不存在"));
    }

    /**
     * 创建咨询订单
     */
    @Transactional
    public ConsultationOrder createConsultation(Long userId, Long nutritionistId, String description, String consultationType) {
        Nutritionist nutritionist = nutritionistRepository.findById(nutritionistId)
                .orElseThrow(() -> new BusinessException("营养师不存在"));

        if (!nutritionist.getIsActive()) {
            throw new BusinessException("该营养师暂不可用");
        }

        String orderNo = generateOrderNo("CON");

        ConsultationOrder order = ConsultationOrder.builder()
                .orderNo(orderNo)
                .userId(userId)
                .nutritionistId(nutritionistId)
                .nutritionistName(nutritionist.getName())
                .amount(nutritionist.getConsultationFee())
                .consultationType(consultationType != null ? consultationType : "TEXT")
                .description(description)
                .status("WAITING")
                .paymentStatus("PENDING")
                .expireTime(LocalDateTime.now().plusMinutes(30))
                .messages(new ArrayList<>())
                .build();

        order = consultationOrderRepository.save(order);
        log.info("咨询订单创建成功, userId={}, orderNo={}, nutritionist={}", userId, orderNo, nutritionist.getName());
        return order;
    }

    /**
     * 模拟支付咨询订单
     */
    @Transactional
    public ConsultationOrder simulatePayConsultation(Long userId, String orderNo) {
        ConsultationOrder order = consultationOrderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException("订单不存在"));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }

        if ("PAID".equals(order.getPaymentStatus())) {
            return order;
        }

        if (!"PENDING".equals(order.getPaymentStatus())) {
            throw new BusinessException("订单状态不允许支付");
        }

        order.setPaymentStatus("PAID");
        order.setPaidAt(LocalDateTime.now());
        order.setStatus("IN_PROGRESS");
        order.setStartedAt(LocalDateTime.now());
        consultationOrderRepository.save(order);

        // 增加营养师咨询次数
        Nutritionist nutritionist = nutritionistRepository.findById(order.getNutritionistId()).orElse(null);
        if (nutritionist != null) {
            nutritionist.setConsultationCount(nutritionist.getConsultationCount() + 1);
            nutritionistRepository.save(nutritionist);
        }

        // 赠送成长值
        memberService.addGrowth(userId, 15, "SYSTEM_REWARD", "完成营养师咨询预约");

        log.info("咨询订单支付成功(模拟), userId={}, orderNo={}", userId, orderNo);
        return order;
    }

    /**
     * 发送咨询消息
     */
    @Transactional
    public ConsultationOrder sendMessage(Long userId, String orderNo, String content, String role) {
        ConsultationOrder order = consultationOrderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException("订单不存在"));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该咨询");
        }

        if (!"IN_PROGRESS".equals(order.getStatus())) {
            throw new BusinessException("咨询尚未开始或已结束");
        }

        List<Map<String, Object>> messages = order.getMessages();
        if (messages == null) {
            messages = new ArrayList<>();
        }

        Map<String, Object> msg = new HashMap<>();
        msg.put("role", role != null ? role : "user");
        msg.put("content", content);
        msg.put("timestamp", LocalDateTime.now().toString());
        messages.add(msg);

        order.setMessages(messages);
        consultationOrderRepository.save(order);

        // 通过IM推送消息给营养师（异步，不影响主流程）
        pushImMessage(userId, order.getNutritionistId(), content, orderNo);

        return order;
    }

    /**
     * 模拟营养师回复（AI辅助模拟）
     */
    @Transactional
    public ConsultationOrder simulateNutritionistReply(Long userId, String orderNo) {
        ConsultationOrder order = consultationOrderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException("订单不存在"));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该咨询");
        }

        List<Map<String, Object>> messages = order.getMessages();
        if (messages == null || messages.isEmpty()) {
            throw new BusinessException("请先发送咨询消息");
        }

        // 获取最后一条用户消息用于生成回复
        String lastUserMsg = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).get("role"))) {
                lastUserMsg = (String) messages.get(i).get("content");
                break;
            }
        }

        // 模拟营养师回复
        String reply = generateSimulatedReply(lastUserMsg, order.getNutritionistName());

        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "nutritionist");
        msg.put("content", reply);
        msg.put("timestamp", LocalDateTime.now().toString());
        messages.add(msg);

        order.setMessages(messages);
        consultationOrderRepository.save(order);
        return order;
    }

    /**
     * 完成咨询
     */
    @Transactional
    public ConsultationOrder completeConsultation(Long userId, String orderNo, Integer rating, String review) {
        ConsultationOrder order = consultationOrderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException("订单不存在"));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该咨询");
        }

        if (!"IN_PROGRESS".equals(order.getStatus())) {
            throw new BusinessException("咨询未在进行中");
        }

        order.setStatus("COMPLETED");
        order.setCompletedAt(LocalDateTime.now());
        if (rating != null) {
            order.setUserRating(Math.max(1, Math.min(5, rating)));
        }
        if (review != null) {
            order.setUserReview(review);
        }

        // 生成总结
        order.setSummary("本次咨询已完成。营养师" + order.getNutritionistName() + "为您提供了专业的营养建议，请注意遵循建议调整饮食。");

        consultationOrderRepository.save(order);

        // 更新营养师评分
        if (rating != null) {
            updateNutritionistRating(order.getNutritionistId());
        }

        log.info("咨询完成, userId={}, orderNo={}", userId, orderNo);
        return order;
    }

    /**
     * 模拟退款
     */
    @Transactional
    public ConsultationOrder simulateRefund(Long userId, String orderNo) {
        ConsultationOrder order = consultationOrderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException("订单不存在"));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }

        if (!"PAID".equals(order.getPaymentStatus())) {
            throw new BusinessException("仅已支付订单可退款");
        }

        if ("COMPLETED".equals(order.getStatus())) {
            throw new BusinessException("已完成的咨询不支持退款");
        }

        order.setPaymentStatus("REFUNDED");
        order.setStatus("CANCELLED");
        consultationOrderRepository.save(order);

        log.info("咨询订单退款成功(模拟), userId={}, orderNo={}", userId, orderNo);
        return order;
    }

    /**
     * 获取用户咨询订单历史
     */
    public Page<ConsultationOrder> getConsultationHistory(Long userId, int page, int size) {
        return consultationOrderRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    /**
     * 获取用户活跃咨询（进行中的）
     */
    public List<ConsultationOrder> getActiveConsultations(Long userId) {
        return consultationOrderRepository.findActiveConsultations(userId);
    }

    /**
     * 定时清理超时未支付咨询订单
     */
    @Scheduled(fixedDelay = 120_000)
    public void cancelExpiredOrders() {
        List<ConsultationOrder> expired = consultationOrderRepository.findExpiredPendingOrders(LocalDateTime.now());
        if (!expired.isEmpty()) {
            expired.forEach(o -> {
                o.setPaymentStatus("EXPIRED");
                o.setStatus("CANCELLED");
            });
            consultationOrderRepository.saveAll(expired);
            log.info("自动取消超时咨询订单 {} 笔", expired.size());
        }
    }

    /**
     * 获取营养师的咨询订单列表
     */
    public Page<ConsultationOrder> getNutritionistConsultations(Long nutritionistId, int page, int size, String status) {
        if (status != null && !status.isEmpty()) {
            return consultationOrderRepository.findByNutritionistIdAndStatusOrderByCreatedAtDesc(
                    nutritionistId, status, PageRequest.of(page, size));
        }
        return consultationOrderRepository.findByNutritionistIdOrderByCreatedAtDesc(
                nutritionistId, PageRequest.of(page, size));
    }

    /**
     * 营养师真实回复消息
     */
    @Transactional
    public ConsultationOrder nutritionistReply(Long nutritionistId, String orderNo, String content) {
        ConsultationOrder order = consultationOrderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException("订单不存在"));

        if (!order.getNutritionistId().equals(nutritionistId)) {
            throw new BusinessException("无权回复该咨询");
        }

        if (!"IN_PROGRESS".equals(order.getStatus())) {
            throw new BusinessException("咨询未在进行中");
        }

        List<Map<String, Object>> messages = order.getMessages();
        if (messages == null) {
            messages = new ArrayList<>();
        }

        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "nutritionist");
        msg.put("content", content);
        msg.put("timestamp", LocalDateTime.now().toString());
        messages.add(msg);

        order.setMessages(messages);
        consultationOrderRepository.save(order);

        // 通过IM推送消息给用户
        Nutritionist nutritionist = nutritionistRepository.findById(nutritionistId).orElse(null);
        if (nutritionist != null && nutritionist.getUserId() != null) {
            String fromImId = tencentImService.toImUserId(nutritionist.getUserId());
            String toImId = tencentImService.toImUserId(order.getUserId());
            try {
                tencentImService.sendC2CMessage(fromImId, toImId, content, orderNo);
            } catch (Exception e) {
                log.warn("IM推送营养师回复失败，不影响主流程", e);
            }
        }

        return order;
    }

    /**
     * 获取营养师的活跃咨询
     */
    public List<ConsultationOrder> getNutritionistActiveConsultations(Long nutritionistId) {
        return consultationOrderRepository.findActiveConsultationsByNutritionistId(nutritionistId);
    }

    /**
     * 根据用户ID获取关联的营养师信息
     */
    public Nutritionist getNutritionistByUserId(Long userId) {
        return nutritionistRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("未找到关联的营养师信息"));
    }

    /**
     * 更新营养师在线状态
     */
    @Transactional
    public Nutritionist updateNutritionistStatus(Long nutritionistId, String status) {
        Nutritionist nutritionist = nutritionistRepository.findById(nutritionistId)
                .orElseThrow(() -> new BusinessException("营养师不存在"));
        if (!List.of("ONLINE", "OFFLINE", "BUSY").contains(status)) {
            throw new BusinessException("无效的状态值");
        }
        nutritionist.setStatus(status);
        nutritionistRepository.save(nutritionist);
        return nutritionist;
    }

    /**
     * 获取单个咨询订单详情（用户端）
     */
    public ConsultationOrder getConsultationDetail(Long userId, String orderNo) {
        ConsultationOrder order = consultationOrderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException("订单不存在"));
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该咨询");
        }
        return order;
    }

    // === 私有方法 ===

    /**
     * 通过IM推送用户消息给营养师（异步，失败不影响主流程）
     */
    private void pushImMessage(Long userId, Long nutritionistId, String content, String orderNo) {
        try {
            Nutritionist nutritionist = nutritionistRepository.findById(nutritionistId).orElse(null);
            if (nutritionist != null && nutritionist.getUserId() != null) {
                String fromImId = tencentImService.toImUserId(userId);
                String toImId = tencentImService.toImUserId(nutritionist.getUserId());
                tencentImService.sendC2CMessage(fromImId, toImId, content, orderNo);
            }
        } catch (Exception e) {
            log.warn("IM推送用户消息失败，不影响主流程", e);
        }
    }

    private String generateOrderNo(String prefix) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return prefix + timestamp + random;
    }

    private void updateNutritionistRating(Long nutritionistId) {
        // 简化评分更新逻辑
        Nutritionist nutritionist = nutritionistRepository.findById(nutritionistId).orElse(null);
        if (nutritionist == null) return;
        // 保持高评分模拟
        nutritionistRepository.save(nutritionist);
    }

    private String generateSimulatedReply(String userMessage, String nutritionistName) {
        String[] replies = {
            "您好！根据您的描述，我建议您增加蛋白质的摄入量。建议每餐包含100-150g优质蛋白，如鸡胸肉、鱼肉或豆制品。同时注意搭配丰富的蔬菜水果，确保膳食纤维和维生素的充足摄入。",
            "感谢您的信任！针对您的情况，我推荐采用「地中海饮食模式」。多摄入橄榄油、坚果、鱼类和全谷物，减少红肉和加工食品的摄入。这种饮食模式有助于心血管健康和体重管理。",
            "您的营养需求我已经了解了。建议您每日饮水量保持在2000ml以上，早餐一定要吃好，推荐鸡蛋+全麦面包+牛奶的组合。午餐和晚餐注意荤素搭配，遵循「一荤两素一汤」的原则。",
            "根据您提供的信息，您目前的饮食中缺乏足够的钙质和维生素D。建议每天饮用300ml牛奶或等量酸奶，适当增加深绿色蔬菜的摄入。如果户外活动较少，可以考虑补充维生素D。",
            "非常好的问题！关于减脂期间的饮食安排，我建议您采用「碳循环」方式：训练日适当增加碳水摄入（全谷物为主），休息日降低碳水。每日蛋白质保持在体重公斤数×1.6g。同时保证充足的蔬菜摄入。"
        };

        Random random = new Random();
        String base = replies[random.nextInt(replies.length)];
        return "【" + nutritionistName + "】" + base + "\n\n如果还有其他问题，请随时告诉我。";
    }
}
