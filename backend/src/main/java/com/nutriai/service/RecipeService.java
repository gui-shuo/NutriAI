package com.nutriai.service;

import com.nutriai.entity.*;
import com.nutriai.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository ingredientRepository;
    private final RecipeStepRepository stepRepository;
    private final RecipeTagRepository tagRepository;
    private final RecipeFavoriteRepository favoriteRepository;
    private final RecipeRatingRepository ratingRepository;

    /**
     * 分页查询食谱（支持多条件筛选）
     */
    public Page<Recipe> getRecipes(int page, int size, String category, String dietType,
                                    String cuisineType, String difficulty, String keyword, String tag, String sort) {
        Pageable pageable = buildPageable(page, size, sort);

        Page<Recipe> result;
        if (keyword != null && !keyword.isBlank()) {
            result = recipeRepository.searchByKeyword(keyword.trim(), pageable);
        } else if (tag != null && !tag.isBlank()) {
            result = recipeRepository.findByTag(tag, pageable);
        } else {
            result = recipeRepository.findWithFilters(
                    blankToNull(category), blankToNull(dietType),
                    blankToNull(cuisineType), blankToNull(difficulty), pageable);
        }
        // Clear lazy collections to avoid LazyInitializationException during serialization
        result.getContent().forEach(this::clearLazyCollections);
        return result;
    }

    /**
     * 获取精选食谱
     */
    public List<Recipe> getFeaturedRecipes() {
        List<Recipe> list = recipeRepository.findByIsActiveTrueAndIsFeaturedTrueOrderByRatingAvgDesc();
        list.forEach(this::clearLazyCollections);
        return list;
    }

    /**
     * 获取食谱详情（含食材、步骤、标签）
     */
    @Transactional
    public Recipe getRecipeDetail(Long id) {
        Recipe recipe = recipeRepository.findById(id).orElse(null);
        if (recipe == null || !recipe.getIsActive()) return null;
        recipeRepository.incrementViewCount(id);
        // Force lazy load
        if (recipe.getIngredients() != null) recipe.getIngredients().size();
        if (recipe.getSteps() != null) recipe.getSteps().size();
        if (recipe.getTags() != null) recipe.getTags().size();
        return recipe;
    }

    /**
     * 收藏/取消收藏
     */
    @Transactional
    public boolean toggleFavorite(Long userId, Long recipeId) {
        Optional<RecipeFavorite> existing = favoriteRepository.findByUserIdAndRecipeId(userId, recipeId);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            recipeRepository.updateFavoriteCount(recipeId, -1);
            return false;
        } else {
            favoriteRepository.save(RecipeFavorite.builder().userId(userId).recipeId(recipeId).build());
            recipeRepository.updateFavoriteCount(recipeId, 1);
            return true;
        }
    }

    /**
     * 检查是否已收藏
     */
    public boolean isFavorited(Long userId, Long recipeId) {
        return favoriteRepository.existsByUserIdAndRecipeId(userId, recipeId);
    }

    /**
     * 获取用户收藏列表
     */
    public Page<RecipeFavorite> getUserFavorites(Long userId, int page, int size) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(Math.max(page - 1, 0), size));
    }

    /**
     * 评分
     */
    @Transactional
    public RecipeRating rateRecipe(Long userId, Long recipeId, int score, String comment) {
        RecipeRating rating = ratingRepository.findByUserIdAndRecipeId(userId, recipeId)
                .orElse(RecipeRating.builder().userId(userId).recipeId(recipeId).build());
        rating.setScore(score);
        rating.setComment(comment);
        ratingRepository.save(rating);

        // Update recipe average
        BigDecimal avg = ratingRepository.getAverageScore(recipeId);
        int count = ratingRepository.countByRecipeId(recipeId);
        if (avg != null) {
            recipeRepository.updateRating(recipeId, avg.setScale(1, RoundingMode.HALF_UP), count);
        }
        return rating;
    }

    /**
     * 获取所有标签
     */
    public List<String> getAllTags() {
        return tagRepository.findAllDistinctTags();
    }

    // ========== Admin Methods ==========

    public Page<Recipe> adminListRecipes(int page, int size) {
        Page<Recipe> result = recipeRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(page - 1, 0), size));
        result.getContent().forEach(this::clearLazyCollections);
        return result;
    }

    @Transactional
    public Recipe createRecipe(Recipe recipe) {
        Recipe saved = recipeRepository.save(recipe);
        saveRelations(saved);
        return saved;
    }

    @Transactional
    public Recipe updateRecipe(Long id, Recipe updated) {
        Recipe existing = recipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("食谱不存在"));
        // Update fields
        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setCoverImage(updated.getCoverImage());
        existing.setCategory(updated.getCategory());
        existing.setCuisineType(updated.getCuisineType());
        existing.setDietType(updated.getDietType());
        existing.setDifficulty(updated.getDifficulty());
        existing.setCookingTimeMinutes(updated.getCookingTimeMinutes());
        existing.setPrepTimeMinutes(updated.getPrepTimeMinutes());
        existing.setServings(updated.getServings());
        existing.setCaloriesPerServing(updated.getCaloriesPerServing());
        existing.setProteinPerServing(updated.getProteinPerServing());
        existing.setFatPerServing(updated.getFatPerServing());
        existing.setCarbsPerServing(updated.getCarbsPerServing());
        existing.setFiberPerServing(updated.getFiberPerServing());
        existing.setTips(updated.getTips());
        existing.setIsFeatured(updated.getIsFeatured());
        existing.setIsActive(updated.getIsActive());

        // Replace relations
        if (updated.getIngredients() != null) {
            existing.getIngredients().clear();
            updated.getIngredients().forEach(i -> { i.setRecipeId(id); existing.getIngredients().add(i); });
        }
        if (updated.getSteps() != null) {
            existing.getSteps().clear();
            updated.getSteps().forEach(s -> { s.setRecipeId(id); existing.getSteps().add(s); });
        }
        if (updated.getTags() != null) {
            existing.getTags().clear();
            updated.getTags().forEach(t -> { t.setRecipeId(id); existing.getTags().add(t); });
        }

        return recipeRepository.save(existing);
    }

    @Transactional
    public void deleteRecipe(Long id) {
        recipeRepository.deleteById(id);
    }

    @Transactional
    public void toggleActive(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("食谱不存在"));
        recipe.setIsActive(!recipe.getIsActive());
        recipeRepository.save(recipe);
    }

    @Transactional
    public void toggleFeatured(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("食谱不存在"));
        recipe.setIsFeatured(!recipe.getIsFeatured());
        recipeRepository.save(recipe);
    }

    // ========== Helpers ==========

    private void saveRelations(Recipe recipe) {
        if (recipe.getIngredients() != null) {
            recipe.getIngredients().forEach(i -> i.setRecipeId(recipe.getId()));
        }
        if (recipe.getSteps() != null) {
            recipe.getSteps().forEach(s -> s.setRecipeId(recipe.getId()));
        }
        if (recipe.getTags() != null) {
            recipe.getTags().forEach(t -> t.setRecipeId(recipe.getId()));
        }
    }

    private Pageable buildPageable(int page, int size, String sort) {
        Sort sortObj;
        if ("popular".equals(sort)) {
            sortObj = Sort.by(Sort.Direction.DESC, "viewCount");
        } else if ("rating".equals(sort)) {
            sortObj = Sort.by(Sort.Direction.DESC, "ratingAvg");
        } else if ("calories_asc".equals(sort)) {
            sortObj = Sort.by(Sort.Direction.ASC, "caloriesPerServing");
        } else if ("time_asc".equals(sort)) {
            sortObj = Sort.by(Sort.Direction.ASC, "cookingTimeMinutes");
        } else {
            sortObj = Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return PageRequest.of(Math.max(page - 1, 0), size, sortObj);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private void clearLazyCollections(Recipe recipe) {
        recipe.setIngredients(null);
        recipe.setSteps(null);
        recipe.setTags(null);
    }
}
