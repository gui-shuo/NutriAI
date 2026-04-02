package com.nutriai.repository;

import com.nutriai.entity.DietPlanTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DietPlanTaskRepository extends JpaRepository<DietPlanTask, Long> {
    
    Optional<DietPlanTask> findByTaskId(String taskId);
    
    List<DietPlanTask> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<DietPlanTask> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    void deleteAllByUserId(Long userId);
}
