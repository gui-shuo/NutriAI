package com.nutriai.repository;

import com.nutriai.entity.FoodNutrition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 食材营养数据Repository
 * 
 * @author NutriAI Team
 * @date 2025-12-03
 */
@Repository
public interface FoodNutritionRepository extends JpaRepository<FoodNutrition, Long> {
    
    /**
     * 根据食材代码查找
     */
    Optional<FoodNutrition> findByFoodCode(String foodCode);
    
    /**
     * 根据分类ID查找食材
     */
    Page<FoodNutrition> findByCategoryId(Long categoryId, Pageable pageable);
    
    /**
     * 根据分类ID列表查找食材
     */
    Page<FoodNutrition> findByCategoryIdIn(List<Long> categoryIds, Pageable pageable);
    
    /**
     * 根据状态查找食材
     */
    Page<FoodNutrition> findByStatus(String status, Pageable pageable);
    
    /**
     * 全文搜索食材（使用FULLTEXT索引）
     */
    @Query(value = "SELECT * FROM food_nutrition " +
            "WHERE MATCH(food_name, search_keywords, pinyin) AGAINST(:keyword IN NATURAL LANGUAGE MODE) " +
            "AND status = 'ACTIVE' " +
            "ORDER BY MATCH(food_name, search_keywords, pinyin) AGAINST(:keyword IN NATURAL LANGUAGE MODE) DESC",
            nativeQuery = true)
    List<FoodNutrition> fullTextSearch(@Param("keyword") String keyword);
    
    /**
     * 模糊搜索食材（不依赖FULLTEXT，兼容性更好）
     */
    @Query("SELECT f FROM FoodNutrition f WHERE " +
            "(f.foodName LIKE %:keyword% OR " +
            "f.searchKeywords LIKE %:keyword% OR " +
            "f.pinyin LIKE %:keyword%) " +
            "AND f.status = 'ACTIVE'")
    Page<FoodNutrition> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 根据分类和关键词搜索
     */
    @Query("SELECT f FROM FoodNutrition f WHERE " +
            "f.categoryId = :categoryId AND " +
            "(f.foodName LIKE %:keyword% OR " +
            "f.searchKeywords LIKE %:keyword%) " +
            "AND f.status = 'ACTIVE'")
    Page<FoodNutrition> searchByCategoryAndKeyword(
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            Pageable pageable);
    
    /**
     * 获取高蛋白食物
     */
    @Query("SELECT f FROM FoodNutrition f WHERE " +
            "f.protein >= :minProtein AND f.status = 'ACTIVE' " +
            "ORDER BY f.protein DESC")
    List<FoodNutrition> findHighProteinFoods(@Param("minProtein") Double minProtein, Pageable pageable);
    
    /**
     * 获取低热量食物
     */
    @Query("SELECT f FROM FoodNutrition f WHERE " +
            "f.energy <= :maxEnergy AND f.status = 'ACTIVE' " +
            "ORDER BY f.energy ASC")
    List<FoodNutrition> findLowCalorieFoods(@Param("maxEnergy") Double maxEnergy, Pageable pageable);
    
    /**
     * 根据季节查找食材
     */
    @Query("SELECT f FROM FoodNutrition f WHERE " +
            "f.season LIKE %:season% AND f.status = 'ACTIVE'")
    List<FoodNutrition> findBySeasonContaining(@Param("season") String season);
    
    /**
     * 检查食材代码是否存在
     */
    boolean existsByFoodCode(String foodCode);
    
    /**
     * 统计分类下的食材数量
     */
    @Query("SELECT COUNT(f) FROM FoodNutrition f WHERE f.categoryId = :categoryId AND f.status = 'ACTIVE'")
    long countByCategoryId(@Param("categoryId") Long categoryId);
}
