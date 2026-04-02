package com.nutriai.repository;

import com.nutriai.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Page<Feedback> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Feedback> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Feedback> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<Feedback> findByTypeOrderByCreatedAtDesc(String type, Pageable pageable);

    Page<Feedback> findByStatusAndTypeOrderByCreatedAtDesc(String status, String type, Pageable pageable);

    long countByStatus(String status);
    void deleteAllByUserId(Long userId);
}
