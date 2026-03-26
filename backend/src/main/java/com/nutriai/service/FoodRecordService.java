package com.nutriai.service;

import com.nutriai.dto.food.CreateFoodRecordRequest;
import com.nutriai.dto.food.FoodRecordResponse;
import com.nutriai.dto.food.NutritionStatsResponse;
import com.nutriai.entity.FoodRecord;
import com.nutriai.exception.BusinessException;
import com.nutriai.repository.FoodRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 饮食记录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FoodRecordService {
    
    private final FoodRecordRepository foodRecordRepository;
    
    /**
     * 创建饮食记录
     */
    @Transactional
    public FoodRecordResponse createFoodRecord(Long userId, CreateFoodRecordRequest request) {
        log.info("创建饮食记录: userId={}, foodName={}", userId, request.getFoodName());
        
        FoodRecord record = FoodRecord.builder()
                .userId(userId)
                .mealType(request.getMealType())
                .foodName(request.getFoodName())
                .photoUrl(request.getPhotoUrl())
                .portion(request.getPortion())
                .calories(request.getCalories())
                .protein(request.getProtein())
                .carbohydrates(request.getCarbohydrates())
                .fat(request.getFat())
                .fiber(request.getFiber())
                .recordTime(request.getRecordTime())
                .notes(request.getNotes())
                .build();
        
        record = foodRecordRepository.save(record);
        log.info("饮食记录创建成功: recordId={}", record.getId());
        
        return FoodRecordResponse.from(record);
    }
    
    /**
     * 查询饮食记录（分页）
     */
    public Page<FoodRecordResponse> getFoodRecords(Long userId, int page, int size, 
                                                    LocalDate startDate, LocalDate endDate, 
                                                    FoodRecord.MealType mealType) {
        log.info("查询饮食记录: userId={}, page={}, size={}", userId, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<FoodRecord> records;
        
        if (startDate != null && endDate != null && mealType != null) {
            // 按时间范围 + 餐次类型组合查询
            LocalDateTime startTime = startDate.atStartOfDay();
            LocalDateTime endTime = endDate.atTime(LocalTime.MAX);
            records = foodRecordRepository.findByUserIdAndMealTypeAndRecordTimeBetweenOrderByRecordTimeDesc(
                    userId, mealType, startTime, endTime, pageable);
        } else if (startDate != null && endDate != null) {
            // 仅按时间范围查询
            LocalDateTime startTime = startDate.atStartOfDay();
            LocalDateTime endTime = endDate.atTime(LocalTime.MAX);
            records = foodRecordRepository.findByUserIdAndRecordTimeBetweenOrderByRecordTimeDesc(
                    userId, startTime, endTime, pageable);
        } else if (mealType != null) {
            // 仅按餐次类型查询
            records = foodRecordRepository.findByUserIdAndMealTypeOrderByRecordTimeDesc(
                    userId, mealType, pageable);
        } else {
            // 查询所有记录
            records = foodRecordRepository.findByUserIdOrderByRecordTimeDesc(userId, pageable);
        }
        
        return records.map(FoodRecordResponse::from);
    }
    
    /**
     * 获取饮食记录详情
     */
    public FoodRecordResponse getFoodRecordById(Long userId, Long recordId) {
        log.info("获取饮食记录详情: userId={}, recordId={}", userId, recordId);
        
        FoodRecord record = foodRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException("饮食记录不存在"));
        
        // 验证记录所属用户
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该记录");
        }
        
        return FoodRecordResponse.from(record);
    }
    
    /**
     * 删除饮食记录
     */
    @Transactional
    public void deleteFoodRecord(Long userId, Long recordId) {
        log.info("删除饮食记录: userId={}, recordId={}", userId, recordId);
        
        FoodRecord record = foodRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException("饮食记录不存在"));
        
        // 验证记录所属用户
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException("无权删除该记录");
        }
        
        foodRecordRepository.delete(record);
        log.info("饮食记录删除成功: recordId={}", recordId);
    }
    
    /**
     * 获取营养摄入统计
     */
    public NutritionStatsResponse getNutritionStats(Long userId, LocalDate date) {
        log.info("获取营养摄入统计: userId={}, date={}", userId, date);
        
        // 查询当天的所有记录
        LocalDateTime startTime = date.atStartOfDay();
        LocalDateTime endTime = date.atTime(LocalTime.MAX);
        List<FoodRecord> records = foodRecordRepository.findByUserIdAndRecordTimeBetween(
                userId, startTime, endTime);
        
        // 统计总营养
        BigDecimal totalCalories = records.stream()
                .map(FoodRecord::getCalories)
                .filter(c -> c != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalProtein = records.stream()
                .map(FoodRecord::getProtein)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCarbs = records.stream()
                .map(FoodRecord::getCarbohydrates)
                .filter(c -> c != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalFat = records.stream()
                .map(FoodRecord::getFat)
                .filter(f -> f != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalFiber = records.stream()
                .map(FoodRecord::getFiber)
                .filter(f -> f != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 按餐次统计卡路里
        Map<FoodRecord.MealType, BigDecimal> caloriesByMeal = records.stream()
                .collect(Collectors.groupingBy(
                        FoodRecord::getMealType,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                r -> r.getCalories() != null ? r.getCalories() : BigDecimal.ZERO,
                                BigDecimal::add
                        )
                ));
        
        // 统计各餐次记录数
        Map<FoodRecord.MealType, Long> countsByMeal = records.stream()
                .collect(Collectors.groupingBy(
                        FoodRecord::getMealType,
                        Collectors.counting()
                ));
        
        return NutritionStatsResponse.builder()
                .date(date)
                .totalCalories(totalCalories)
                .totalProtein(totalProtein)
                .totalCarbohydrates(totalCarbs)
                .totalFat(totalFat)
                .totalFiber(totalFiber)
                .breakfastCalories(caloriesByMeal.getOrDefault(FoodRecord.MealType.BREAKFAST, BigDecimal.ZERO))
                .lunchCalories(caloriesByMeal.getOrDefault(FoodRecord.MealType.LUNCH, BigDecimal.ZERO))
                .dinnerCalories(caloriesByMeal.getOrDefault(FoodRecord.MealType.DINNER, BigDecimal.ZERO))
                .snackCalories(caloriesByMeal.getOrDefault(FoodRecord.MealType.SNACK, BigDecimal.ZERO))
                .recordCount(records.size())
                .mealStats(NutritionStatsResponse.MealStats.builder()
                        .breakfastCount(countsByMeal.getOrDefault(FoodRecord.MealType.BREAKFAST, 0L).intValue())
                        .lunchCount(countsByMeal.getOrDefault(FoodRecord.MealType.LUNCH, 0L).intValue())
                        .dinnerCount(countsByMeal.getOrDefault(FoodRecord.MealType.DINNER, 0L).intValue())
                        .snackCount(countsByMeal.getOrDefault(FoodRecord.MealType.SNACK, 0L).intValue())
                        .build())
                .build();
    }
}
