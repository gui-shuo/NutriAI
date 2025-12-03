package com.nutriai.service;

import com.nutriai.entity.FoodCategory;
import com.nutriai.entity.FoodNutrition;
import com.nutriai.repository.FoodCategoryRepository;
import com.nutriai.repository.FoodNutritionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 食材服务类
 * 
 * @author NutriAI Team
 * @date 2025-12-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FoodService {
    
    private final FoodNutritionRepository foodNutritionRepository;
    private final FoodCategoryRepository foodCategoryRepository;
    
    /**
     * 搜索食材
     * 
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页数量
     * @return 食材列表
     */
    public Page<FoodNutrition> searchFoods(String keyword, int page, int size) {
        log.info("搜索食材，关键词: {}", keyword);
        Pageable pageable = PageRequest.of(page, size);
        
        try {
            // 优先使用全文搜索
            List<FoodNutrition> fullTextResults = foodNutritionRepository.fullTextSearch(keyword);
            if (!fullTextResults.isEmpty()) {
                log.info("全文搜索返回 {} 条结果", fullTextResults.size());
                // 注意：这里返回的是List，可以考虑转换为Page
                // 简单处理：只返回前size条
                int start = page * size;
                int end = Math.min(start + size, fullTextResults.size());
                if (start >= fullTextResults.size()) {
                    return Page.empty(pageable);
                }
                return new org.springframework.data.domain.PageImpl<>(
                    fullTextResults.subList(start, end),
                    pageable,
                    fullTextResults.size()
                );
            }
        } catch (Exception e) {
            log.warn("全文搜索失败，回退到模糊搜索: {}", e.getMessage());
        }
        
        // 回退到模糊搜索
        return foodNutritionRepository.searchByKeyword(keyword, pageable);
    }
    
    /**
     * 根据分类搜索食材
     * 
     * @param categoryId 分类ID
     * @param keyword 关键词（可选）
     * @param page 页码
     * @param size 每页数量
     * @return 食材列表
     */
    public Page<FoodNutrition> searchFoodsByCategory(Long categoryId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            return foodNutritionRepository.searchByCategoryAndKeyword(categoryId, keyword, pageable);
        } else {
            return foodNutritionRepository.findByCategoryId(categoryId, pageable);
        }
    }
    
    /**
     * 获取食材详情
     * 
     * @param foodId 食材ID
     * @return 食材详情
     */
    public Optional<FoodNutrition> getFoodById(Long foodId) {
        return foodNutritionRepository.findById(foodId);
    }
    
    /**
     * 根据食材代码获取食材
     * 
     * @param foodCode 食材代码
     * @return 食材详情
     */
    public Optional<FoodNutrition> getFoodByCode(String foodCode) {
        return foodNutritionRepository.findByFoodCode(foodCode);
    }
    
    /**
     * 获取所有一级分类
     * 
     * @return 一级分类列表
     */
    public List<FoodCategory> getTopLevelCategories() {
        return foodCategoryRepository.findAllTopLevelCategories();
    }
    
    /**
     * 获取子分类
     * 
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    public List<FoodCategory> getSubCategories(Long parentId) {
        return foodCategoryRepository.findByParentIdOrderBySortOrderAsc(parentId);
    }
    
    /**
     * 获取分类详情
     * 
     * @param categoryId 分类ID
     * @return 分类详情
     */
    public Optional<FoodCategory> getCategoryById(Long categoryId) {
        return foodCategoryRepository.findById(categoryId);
    }
    
    /**
     * 获取高蛋白食物
     * 
     * @param minProtein 最低蛋白质含量（g/100g）
     * @param limit 数量限制
     * @return 高蛋白食物列表
     */
    public List<FoodNutrition> getHighProteinFoods(Double minProtein, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return foodNutritionRepository.findHighProteinFoods(minProtein, pageable);
    }
    
    /**
     * 获取低热量食物
     * 
     * @param maxEnergy 最高热量（kcal/100g）
     * @param limit 数量限制
     * @return 低热量食物列表
     */
    public List<FoodNutrition> getLowCalorieFoods(Double maxEnergy, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return foodNutritionRepository.findLowCalorieFoods(maxEnergy, pageable);
    }
    
    /**
     * 根据季节推荐食材
     * 
     * @param season 季节（春/夏/秋/冬）
     * @return 季节性食材列表
     */
    public List<FoodNutrition> getSeasonalFoods(String season) {
        return foodNutritionRepository.findBySeasonContaining(season);
    }
    
    /**
     * 统计分类下的食材数量
     * 
     * @param categoryId 分类ID
     * @return 食材数量
     */
    public long countFoodsByCategory(Long categoryId) {
        return foodNutritionRepository.countByCategoryId(categoryId);
    }
    
    /**
     * 获取随机推荐食材
     * 
     * @param count 数量
     * @return 随机食材列表
     */
    public List<FoodNutrition> getRandomFoods(int count) {
        Pageable pageable = PageRequest.of(0, count);
        return foodNutritionRepository.findByStatus("ACTIVE", pageable).getContent();
    }
}
