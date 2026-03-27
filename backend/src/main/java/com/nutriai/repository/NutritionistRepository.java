package com.nutriai.repository;

import com.nutriai.entity.Nutritionist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NutritionistRepository extends JpaRepository<Nutritionist, Long> {

    List<Nutritionist> findByIsActiveTrueOrderBySortOrderAsc();

    List<Nutritionist> findByIsActiveTrueAndStatusOrderBySortOrderAsc(String status);

    List<Nutritionist> findByIsActiveTrueAndSpecialtiesContainingOrderBySortOrderAsc(String specialty);
}
