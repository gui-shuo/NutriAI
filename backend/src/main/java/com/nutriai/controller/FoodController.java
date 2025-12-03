package com.nutriai.controller;

import com.nutriai.common.ApiResponse;
import com.nutriai.entity.FoodCategory;
import com.nutriai.entity.FoodNutrition;
import com.nutriai.service.FoodService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 食材控制器
 * 
 * @author NutriAI Team
 * @date 2025-12-03
 */
@Slf4j
@RestController
@RequestMapping("/food")
@RequiredArgsConstructor
public class FoodController {
    
    private final FoodService foodService;
    
    /**
     * 搜索食材
     * GET /api/food/search?keyword=xxx&page=0&size=20
     */
    @GetMapping("/search")
    public ApiResponse<Page<FoodNutrition>> searchFoods(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<FoodNutrition> foods = foodService.searchFoods(keyword, page, size);
            return ApiResponse.success(foods);
        } catch (Exception e) {
            log.error("搜索食材失败", e);
            return ApiResponse.error("搜索失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据分类搜索食材
     * GET /api/food/category/{categoryId}?keyword=xxx&page=0&size=20
     */
    @GetMapping("/category/{categoryId}")
    public ApiResponse<Page<FoodNutrition>> searchFoodsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<FoodNutrition> foods = foodService.searchFoodsByCategory(categoryId, keyword, page, size);
            return ApiResponse.success(foods);
        } catch (Exception e) {
            log.error("按分类搜索食材失败", e);
            return ApiResponse.error("搜索失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取食材详情
     * GET /api/food/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<FoodNutrition> getFoodById(@PathVariable Long id) {
        try {
            return foodService.getFoodById(id)
                    .map(ApiResponse::success)
                    .orElse(ApiResponse.error("食材不存在"));
        } catch (Exception e) {
            log.error("获取食材详情失败", e);
            return ApiResponse.error("获取失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取所有一级分类
     * GET /api/food/categories
     */
    @GetMapping("/categories")
    public ApiResponse<List<FoodCategory>> getTopLevelCategories() {
        try {
            List<FoodCategory> categories = foodService.getTopLevelCategories();
            return ApiResponse.success(categories);
        } catch (Exception e) {
            log.error("获取分类列表失败", e);
            return ApiResponse.error("获取失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取子分类
     * GET /api/food/categories/{parentId}/children
     */
    @GetMapping("/categories/{parentId}/children")
    public ApiResponse<List<FoodCategory>> getSubCategories(@PathVariable Long parentId) {
        try {
            List<FoodCategory> categories = foodService.getSubCategories(parentId);
            return ApiResponse.success(categories);
        } catch (Exception e) {
            log.error("获取子分类失败", e);
            return ApiResponse.error("获取失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取高蛋白食物
     * GET /api/food/high-protein?minProtein=15&limit=10
     */
    @GetMapping("/high-protein")
    public ApiResponse<List<FoodNutrition>> getHighProteinFoods(
            @RequestParam(defaultValue = "15") Double minProtein,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<FoodNutrition> foods = foodService.getHighProteinFoods(minProtein, limit);
            return ApiResponse.success(foods);
        } catch (Exception e) {
            log.error("获取高蛋白食物失败", e);
            return ApiResponse.error("获取失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取低热量食物
     * GET /api/food/low-calorie?maxEnergy=100&limit=10
     */
    @GetMapping("/low-calorie")
    public ApiResponse<List<FoodNutrition>> getLowCalorieFoods(
            @RequestParam(defaultValue = "100") Double maxEnergy,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<FoodNutrition> foods = foodService.getLowCalorieFoods(maxEnergy, limit);
            return ApiResponse.success(foods);
        } catch (Exception e) {
            log.error("获取低热量食物失败", e);
            return ApiResponse.error("获取失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据季节推荐食材
     * GET /api/food/seasonal?season=春
     */
    @GetMapping("/seasonal")
    public ApiResponse<List<FoodNutrition>> getSeasonalFoods(@RequestParam String season) {
        try {
            List<FoodNutrition> foods = foodService.getSeasonalFoods(season);
            return ApiResponse.success(foods);
        } catch (Exception e) {
            log.error("获取季节性食材失败", e);
            return ApiResponse.error("获取失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取随机推荐食材
     * GET /api/food/random?count=5
     */
    @GetMapping("/random")
    public ApiResponse<List<FoodNutrition>> getRandomFoods(
            @RequestParam(defaultValue = "5") int count) {
        try {
            List<FoodNutrition> foods = foodService.getRandomFoods(count);
            return ApiResponse.success(foods);
        } catch (Exception e) {
            log.error("获取随机食材失败", e);
            return ApiResponse.error("获取失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取统计信息
     * GET /api/food/database/stats
     */
    @GetMapping("/database/stats")
    public ApiResponse<Map<String, Object>> getDatabaseStats() {
        try {
            long totalFoods = foodService.searchFoods("", 0, 1).getTotalElements();
            int totalCategories = foodService.getTopLevelCategories().size();
            
            Map<String, Object> stats = Map.of(
                    "totalFoods", totalFoods,
                    "totalCategories", totalCategories
            );
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("获取统计信息失败", e);
            return ApiResponse.error("获取失败：" + e.getMessage());
        }
    }
}
