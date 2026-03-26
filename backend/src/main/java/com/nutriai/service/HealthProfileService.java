package com.nutriai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriai.dto.user.HealthProfileRequest;
import com.nutriai.dto.user.HealthProfileResponse;
import com.nutriai.entity.HealthProfile;
import com.nutriai.exception.BusinessException;
import com.nutriai.repository.HealthProfileRepository;
import com.nutriai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 健康档案服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthProfileService {

    private final HealthProfileRepository healthProfileRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * 获取用户健康档案
     */
    public HealthProfileResponse getHealthProfile(Long userId) {
        // 确保用户存在
        if (!userRepository.existsById(userId)) {
            throw BusinessException.User.userNotFound();
        }

        HealthProfile profile = healthProfileRepository.findByUserId(userId)
                .orElse(null);

        if (profile == null) {
            // 返回空档案，表示用户尚未创建
            return null;
        }

        return HealthProfileResponse.fromEntity(profile);
    }

    /**
     * 保存或更新健康档案
     */
    @Transactional
    public HealthProfileResponse saveHealthProfile(Long userId, HealthProfileRequest request) {
        // 确保用户存在
        if (!userRepository.existsById(userId)) {
            throw BusinessException.User.userNotFound();
        }

        HealthProfile profile = healthProfileRepository.findByUserId(userId)
                .orElseGet(() -> HealthProfile.builder().userId(userId).build());

        // 更新字段
        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        }
        if (request.getBirthDate() != null) {
            profile.setBirthDate(request.getBirthDate());
        }
        if (request.getHeight() != null) {
            profile.setHeight(request.getHeight());
        }
        if (request.getWeight() != null) {
            profile.setWeight(request.getWeight());
        }
        if (request.getTargetWeight() != null) {
            profile.setTargetWeight(request.getTargetWeight());
        }
        if (request.getActivityLevel() != null) {
            profile.setActivityLevel(request.getActivityLevel());
        }
        if (request.getHealthGoals() != null) {
            profile.setHealthGoals(toJson(request.getHealthGoals()));
        }
        if (request.getDietaryRestrictions() != null) {
            profile.setDietaryRestrictions(toJson(request.getDietaryRestrictions()));
        }
        if (request.getAllergies() != null) {
            profile.setAllergies(toJson(request.getAllergies()));
        }
        if (request.getMedicalConditions() != null) {
            profile.setMedicalConditions(toJson(request.getMedicalConditions()));
        }
        if (request.getDailyCalorieTarget() != null) {
            profile.setDailyCalorieTarget(request.getDailyCalorieTarget());
        }
        if (request.getWaistCircumference() != null) {
            profile.setWaistCircumference(request.getWaistCircumference());
        }
        if (request.getBodyFatPercentage() != null) {
            profile.setBodyFatPercentage(request.getBodyFatPercentage());
        }
        if (request.getNotes() != null) {
            profile.setNotes(request.getNotes());
        }

        // 自动计算BMI
        if (profile.getHeight() != null && profile.getWeight() != null) {
            BigDecimal heightM = profile.getHeight().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal heightMSq = heightM.multiply(heightM);
            if (heightMSq.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal bmi = profile.getWeight().divide(heightMSq, 1, RoundingMode.HALF_UP);
                profile.setBmi(bmi);
            }
        }

        // 自动计算每日卡路里推荐值（如果用户未手动设置）
        if (request.getDailyCalorieTarget() == null && profile.getDailyCalorieTarget() == null) {
            Integer recommendedCalories = calculateRecommendedCalories(profile);
            if (recommendedCalories != null) {
                profile.setDailyCalorieTarget(recommendedCalories);
            }
        }

        profile = healthProfileRepository.save(profile);
        log.info("健康档案保存成功: userId={}", userId);

        return HealthProfileResponse.fromEntity(profile);
    }

    /**
     * 基于Harris-Benedict公式计算基础代谢率和推荐卡路里
     */
    private Integer calculateRecommendedCalories(HealthProfile profile) {
        if (profile.getHeight() == null || profile.getWeight() == null) {
            return null;
        }

        double height = profile.getHeight().doubleValue();
        double weight = profile.getWeight().doubleValue();
        int age = 30; // 默认年龄

        if (profile.getBirthDate() != null) {
            age = java.time.Period.between(profile.getBirthDate(), java.time.LocalDate.now()).getYears();
        }

        double bmr;
        if ("FEMALE".equals(profile.getGender())) {
            // 女性BMR = 655.1 + (9.563 × 体重kg) + (1.850 × 身高cm) - (4.676 × 年龄)
            bmr = 655.1 + (9.563 * weight) + (1.850 * height) - (4.676 * age);
        } else {
            // 男性BMR = 66.47 + (13.75 × 体重kg) + (5.003 × 身高cm) - (6.755 × 年龄)
            bmr = 66.47 + (13.75 * weight) + (5.003 * height) - (6.755 * age);
        }

        // 活动系数
        double activityFactor = switch (profile.getActivityLevel() != null ? profile.getActivityLevel() : "LIGHT") {
            case "SEDENTARY" -> 1.2;
            case "LIGHT" -> 1.375;
            case "MODERATE" -> 1.55;
            case "ACTIVE" -> 1.725;
            case "VERY_ACTIVE" -> 1.9;
            default -> 1.375;
        };

        return (int) Math.round(bmr * activityFactor);
    }

    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败", e);
            return "[]";
        }
    }
}
