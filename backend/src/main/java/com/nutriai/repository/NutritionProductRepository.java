package com.nutriai.repository;

import com.nutriai.entity.NutritionProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NutritionProductRepository extends JpaRepository<NutritionProduct, Long> {

    Page<NutritionProduct> findByStatusOrderBySortOrderAsc(String status, Pageable pageable);

    Page<NutritionProduct> findByStatusAndCategoryOrderBySortOrderAsc(String status, String category, Pageable pageable);

    List<NutritionProduct> findByStatusAndIsRecommendedTrueOrderBySortOrderAsc(String status);

    @Query("SELECT DISTINCT p.category FROM NutritionProduct p WHERE p.status = 'ACTIVE' ORDER BY p.category")
    List<String> findActiveCategories();

    @Query("SELECT p FROM NutritionProduct p WHERE p.status = 'ACTIVE' AND (p.name LIKE %:keyword% OR p.brief LIKE %:keyword%) ORDER BY p.sortOrder ASC")
    Page<NutritionProduct> searchByKeyword(String keyword, Pageable pageable);
}
