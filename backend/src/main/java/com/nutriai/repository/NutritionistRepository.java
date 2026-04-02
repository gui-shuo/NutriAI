package com.nutriai.repository;

import com.nutriai.entity.Nutritionist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NutritionistRepository extends JpaRepository<Nutritionist, Long> {

    Optional<Nutritionist> findByUserId(Long userId);

    List<Nutritionist> findByIsActiveTrueOrderBySortOrderAsc();

    List<Nutritionist> findByIsActiveTrueAndApprovalStatusOrderBySortOrderAsc(String approvalStatus);

    List<Nutritionist> findByIsActiveTrueAndStatusOrderBySortOrderAsc(String status);

    @Query(value = "SELECT * FROM nutritionists WHERE is_active = true AND JSON_SEARCH(specialties, 'one', :specialty) IS NOT NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<Nutritionist> findByIsActiveTrueAndSpecialtiesContainingOrderBySortOrderAsc(@Param("specialty") String specialty);

    // Admin queries
    Page<Nutritionist> findAllByOrderBySortOrderAsc(Pageable pageable);

    List<Nutritionist> findByApprovalStatus(String approvalStatus);

    @Query("SELECT n FROM Nutritionist n WHERE n.name LIKE %:keyword% OR n.title LIKE %:keyword% ORDER BY n.sortOrder ASC")
    Page<Nutritionist> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    void deleteByUserId(Long userId);
}
