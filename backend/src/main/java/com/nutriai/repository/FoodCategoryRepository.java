package com.nutriai.repository;

import com.nutriai.entity.FoodCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 食材分类Repository
 * 
 * @author NutriAI Team
 * @date 2025-12-03
 */
@Repository
public interface FoodCategoryRepository extends JpaRepository<FoodCategory, Long> {
    
    /**
     * 根据分类代码查找
     */
    Optional<FoodCategory> findByCategoryCode(String categoryCode);
    
    /**
     * 根据父分类ID查找子分类
     */
    List<FoodCategory> findByParentIdOrderBySortOrderAsc(Long parentId);
    
    /**
     * 查找所有一级分类
     */
    @Query("SELECT fc FROM FoodCategory fc WHERE fc.parentId IS NULL ORDER BY fc.sortOrder ASC")
    List<FoodCategory> findAllTopLevelCategories();
    
    /**
     * 根据层级查找分类
     */
    List<FoodCategory> findByLevelOrderBySortOrderAsc(Integer level);
    
    /**
     * 检查分类代码是否存在
     */
    boolean existsByCategoryCode(String categoryCode);
}
