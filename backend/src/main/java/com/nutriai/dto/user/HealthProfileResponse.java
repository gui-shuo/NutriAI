package com.nutriai.dto.user;

import com.nutriai.entity.HealthProfile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Collections;
import java.util.List;

/**
 * 健康档案响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthProfileResponse {

    private Long id;
    private String gender;
    private LocalDate birthDate;
    private Integer age;
    private BigDecimal height;
    private BigDecimal weight;
    private BigDecimal targetWeight;
    private BigDecimal bmi;
    private String bmiStatus;
    private String activityLevel;
    private List<String> healthGoals;
    private List<String> dietaryRestrictions;
    private List<String> allergies;
    private List<String> medicalConditions;
    private Integer dailyCalorieTarget;
    private BigDecimal waistCircumference;
    private BigDecimal bodyFatPercentage;
    private BigDecimal idealWeightMin;
    private BigDecimal idealWeightMax;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从实体构建响应
     */
    public static HealthProfileResponse fromEntity(HealthProfile profile) {
        HealthProfileResponseBuilder builder = HealthProfileResponse.builder()
                .id(profile.getId())
                .gender(profile.getGender())
                .birthDate(profile.getBirthDate())
                .height(profile.getHeight())
                .weight(profile.getWeight())
                .targetWeight(profile.getTargetWeight())
                .bmi(profile.getBmi())
                .activityLevel(profile.getActivityLevel())
                .healthGoals(parseJsonList(profile.getHealthGoals()))
                .dietaryRestrictions(parseJsonList(profile.getDietaryRestrictions()))
                .allergies(parseJsonList(profile.getAllergies()))
                .medicalConditions(parseJsonList(profile.getMedicalConditions()))
                .dailyCalorieTarget(profile.getDailyCalorieTarget())
                .waistCircumference(profile.getWaistCircumference())
                .bodyFatPercentage(profile.getBodyFatPercentage())
                .notes(profile.getNotes())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt());

        // 计算年龄
        if (profile.getBirthDate() != null) {
            builder.age(Period.between(profile.getBirthDate(), LocalDate.now()).getYears());
        }

        // 计算BMI状态
        if (profile.getBmi() != null) {
            builder.bmiStatus(getBmiStatus(profile.getBmi()));
        }

        // 计算理想体重范围
        if (profile.getHeight() != null) {
            BigDecimal heightM = profile.getHeight().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal heightMSq = heightM.multiply(heightM);
            builder.idealWeightMin(BigDecimal.valueOf(18.5).multiply(heightMSq).setScale(1, RoundingMode.HALF_UP));
            builder.idealWeightMax(BigDecimal.valueOf(23.9).multiply(heightMSq).setScale(1, RoundingMode.HALF_UP));
        }

        return builder.build();
    }

    private static String getBmiStatus(BigDecimal bmi) {
        double val = bmi.doubleValue();
        if (val < 18.5) return "偏瘦";
        if (val < 24.0) return "正常";
        if (val < 28.0) return "超重";
        return "肥胖";
    }

    private static List<String> parseJsonList(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
