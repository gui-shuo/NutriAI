package com.nutriai.repository;

import com.nutriai.entity.NutritionProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Collection;

@Repository
public interface NutritionProductRepository extends JpaRepository<NutritionProduct, Long> {

    /** 低库存预警查询 */
    Page<NutritionProduct> findByStockLessThanEqualAndStatusIn(int stock, Collection<String> statuses, Pageable pageable);

    // User-facing: treat both "ON_SALE" and legacy "ACTIVE" as available
    @Query("SELECT p FROM NutritionProduct p WHERE p.status IN ('ON_SALE', 'ACTIVE') ORDER BY p.sortOrder ASC")
    Page<NutritionProduct> findOnSale(Pageable pageable);

    @Query("SELECT p FROM NutritionProduct p WHERE p.status IN ('ON_SALE', 'ACTIVE') AND p.category = :category ORDER BY p.sortOrder ASC")
    Page<NutritionProduct> findOnSaleByCategory(@Param("category") String category, Pageable pageable);

    @Query("SELECT p FROM NutritionProduct p WHERE p.status IN ('ON_SALE', 'ACTIVE') AND p.isRecommended = true ORDER BY p.sortOrder ASC")
    List<NutritionProduct> findOnSaleAndRecommended();

    Page<NutritionProduct> findByStatusOrderBySortOrderAsc(String status, Pageable pageable);

    Page<NutritionProduct> findByStatusAndCategoryOrderBySortOrderAsc(String status, String category, Pageable pageable);

    List<NutritionProduct> findByStatusAndIsRecommendedTrueOrderBySortOrderAsc(String status);

    @Query("SELECT DISTINCT p.category FROM NutritionProduct p WHERE p.status IN ('ON_SALE', 'ACTIVE') ORDER BY p.category")
    List<String> findActiveCategories();

    @Query("SELECT p FROM NutritionProduct p WHERE p.status IN ('ON_SALE', 'ACTIVE') AND (p.name LIKE %:keyword% OR p.brief LIKE %:keyword%) ORDER BY p.sortOrder ASC")
    Page<NutritionProduct> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // Admin queries - return all products regardless of status
    Page<NutritionProduct> findAllByOrderBySortOrderAsc(Pageable pageable);

    Page<NutritionProduct> findByCategoryOrderBySortOrderAsc(String category, Pageable pageable);

    @Query("SELECT p FROM NutritionProduct p WHERE p.name LIKE %:keyword% OR p.brief LIKE %:keyword% ORDER BY p.sortOrder ASC")
    Page<NutritionProduct> adminSearchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
