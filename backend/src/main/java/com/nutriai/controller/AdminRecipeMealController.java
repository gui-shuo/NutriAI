package com.nutriai.controller;

import com.nutriai.common.ApiResponse;
import com.nutriai.entity.*;
import com.nutriai.service.RecipeService;
import com.nutriai.service.MealPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminRecipeMealController {

    private final RecipeService recipeService;
    private final MealPlanService mealPlanService;

    // ========== 食谱管理 ==========

    @GetMapping("/recipes")
    public ResponseEntity<ApiResponse<Page<Recipe>>> listRecipes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("获取成功", recipeService.adminListRecipes(page, size)));
    }

    @GetMapping("/recipes/{id}")
    public ResponseEntity<ApiResponse<Recipe>> getRecipe(@PathVariable Long id) {
        Recipe recipe = recipeService.getRecipeDetail(id);
        if (recipe == null) return ResponseEntity.status(404).body(ApiResponse.error(404, "食谱不存在"));
        return ResponseEntity.ok(ApiResponse.success("获取成功", recipe));
    }

    @PostMapping("/recipes")
    public ResponseEntity<ApiResponse<Recipe>> createRecipe(@RequestBody Recipe recipe) {
        try {
            return ResponseEntity.ok(ApiResponse.success("创建成功", recipeService.createRecipe(recipe)));
        } catch (Exception e) {
            log.error("创建食谱失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error(500, "创建失败: " + e.getMessage()));
        }
    }

    @PutMapping("/recipes/{id}")
    public ResponseEntity<ApiResponse<Recipe>> updateRecipe(@PathVariable Long id, @RequestBody Recipe recipe) {
        try {
            return ResponseEntity.ok(ApiResponse.success("更新成功", recipeService.updateRecipe(id, recipe)));
        } catch (Exception e) {
            log.error("更新食谱失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error(500, "更新失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/recipes/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRecipe(@PathVariable Long id) {
        recipeService.deleteRecipe(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @PutMapping("/recipes/{id}/toggle-active")
    public ResponseEntity<ApiResponse<Void>> toggleRecipeActive(@PathVariable Long id) {
        recipeService.toggleActive(id);
        return ResponseEntity.ok(ApiResponse.success("操作成功", null));
    }

    @PutMapping("/recipes/{id}/toggle-featured")
    public ResponseEntity<ApiResponse<Void>> toggleRecipeFeatured(@PathVariable Long id) {
        recipeService.toggleFeatured(id);
        return ResponseEntity.ok(ApiResponse.success("操作成功", null));
    }

    // ========== 健康餐管理 ==========

    @GetMapping("/meal-plans")
    public ResponseEntity<ApiResponse<Page<MealPlan>>> listMealPlans(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("获取成功", mealPlanService.adminListMealPlans(page, size)));
    }

    @GetMapping("/meal-plans/{id}")
    public ResponseEntity<ApiResponse<MealPlan>> getMealPlan(@PathVariable Long id) {
        MealPlan plan = mealPlanService.getMealPlanDetail(id);
        if (plan == null) return ResponseEntity.status(404).body(ApiResponse.error(404, "健康餐不存在"));
        return ResponseEntity.ok(ApiResponse.success("获取成功", plan));
    }

    @PostMapping("/meal-plans")
    public ResponseEntity<ApiResponse<MealPlan>> createMealPlan(@RequestBody MealPlan plan) {
        try {
            return ResponseEntity.ok(ApiResponse.success("创建成功", mealPlanService.createMealPlan(plan)));
        } catch (Exception e) {
            log.error("创建健康餐失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error(500, "创建失败: " + e.getMessage()));
        }
    }

    @PutMapping("/meal-plans/{id}")
    public ResponseEntity<ApiResponse<MealPlan>> updateMealPlan(@PathVariable Long id, @RequestBody MealPlan plan) {
        try {
            return ResponseEntity.ok(ApiResponse.success("更新成功", mealPlanService.updateMealPlan(id, plan)));
        } catch (Exception e) {
            log.error("更新健康餐失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error(500, "更新失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/meal-plans/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMealPlan(@PathVariable Long id) {
        mealPlanService.deleteMealPlan(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @PutMapping("/meal-plans/{id}/toggle-active")
    public ResponseEntity<ApiResponse<Void>> toggleMealPlanActive(@PathVariable Long id) {
        mealPlanService.toggleActive(id);
        return ResponseEntity.ok(ApiResponse.success("操作成功", null));
    }

    @PutMapping("/meal-plans/{id}/toggle-featured")
    public ResponseEntity<ApiResponse<Void>> toggleMealPlanFeatured(@PathVariable Long id) {
        mealPlanService.toggleFeatured(id);
        return ResponseEntity.ok(ApiResponse.success("操作成功", null));
    }
}
