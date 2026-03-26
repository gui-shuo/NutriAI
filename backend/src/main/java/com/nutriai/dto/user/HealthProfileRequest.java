package com.nutriai.dto.user;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 健康档案请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthProfileRequest {

    @Pattern(regexp = "^(MALE|FEMALE)$", message = "性别值无效")
    private String gender;

    private LocalDate birthDate;

    @DecimalMin(value = "50.0", message = "身高不能低于50cm")
    @DecimalMax(value = "300.0", message = "身高不能超过300cm")
    private BigDecimal height;

    @DecimalMin(value = "20.0", message = "体重不能低于20kg")
    @DecimalMax(value = "500.0", message = "体重不能超过500kg")
    private BigDecimal weight;

    @DecimalMin(value = "20.0", message = "目标体重不能低于20kg")
    @DecimalMax(value = "500.0", message = "目标体重不能超过500kg")
    private BigDecimal targetWeight;

    @Pattern(regexp = "^(SEDENTARY|LIGHT|MODERATE|ACTIVE|VERY_ACTIVE)$", message = "活动量等级无效")
    private String activityLevel;

    private List<String> healthGoals;

    private List<String> dietaryRestrictions;

    private List<String> allergies;

    private List<String> medicalConditions;

    @Min(value = 500, message = "每日卡路里目标不能低于500")
    @Max(value = 10000, message = "每日卡路里目标不能超过10000")
    private Integer dailyCalorieTarget;

    @DecimalMin(value = "30.0", message = "腰围不能低于30cm")
    @DecimalMax(value = "300.0", message = "腰围不能超过300cm")
    private BigDecimal waistCircumference;

    @DecimalMin(value = "1.0", message = "体脂率不能低于1%")
    @DecimalMax(value = "80.0", message = "体脂率不能超过80%")
    private BigDecimal bodyFatPercentage;

    @Size(max = 1000, message = "备注不能超过1000字")
    private String notes;
}
