package com.nutriai.repository;

import com.nutriai.entity.VipPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VipPlanRepository extends JpaRepository<VipPlan, Long> {

    List<VipPlan> findByIsActiveTrueOrderBySortOrderAsc();

    Optional<VipPlan> findByPlanCode(String planCode);
}
