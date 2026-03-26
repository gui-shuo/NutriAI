package com.nutriai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 健康档案实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "health_profiles")
public class HealthProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(length = 10)
    private String gender;  // MALE / FEMALE

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(precision = 5, scale = 1)
    private BigDecimal height;  // cm

    @Column(precision = 5, scale = 1)
    private BigDecimal weight;  // kg

    @Column(name = "target_weight", precision = 5, scale = 1)
    private BigDecimal targetWeight;  // kg

    @Column(precision = 4, scale = 1)
    private BigDecimal bmi;

    @Column(name = "activity_level", length = 20)
    private String activityLevel;  // SEDENTARY / LIGHT / MODERATE / ACTIVE / VERY_ACTIVE

    @Column(name = "health_goals", length = 500)
    private String healthGoals;  // JSON array

    @Column(name = "dietary_restrictions", length = 500)
    private String dietaryRestrictions;  // JSON array

    @Column(name = "allergies", length = 500)
    private String allergies;  // JSON array

    @Column(name = "medical_conditions", length = 500)
    private String medicalConditions;  // JSON array

    @Column(name = "daily_calorie_target")
    private Integer dailyCalorieTarget;  // kcal

    @Column(name = "waist_circumference", precision = 5, scale = 1)
    private BigDecimal waistCircumference;  // cm

    @Column(name = "body_fat_percentage", precision = 4, scale = 1)
    private BigDecimal bodyFatPercentage;  // %

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
