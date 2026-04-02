package com.nutriai.repository;

import com.nutriai.entity.CommunityPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

    Page<CommunityPost> findByStatusOrderByIsPinnedDescCreatedAtDesc(String status, Pageable pageable);

    Page<CommunityPost> findByStatusAndCategoryOrderByIsPinnedDescCreatedAtDesc(String status, String category, Pageable pageable);

    Page<CommunityPost> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status, Pageable pageable);

    Page<CommunityPost> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(String status);

    @Modifying
    @Query("UPDATE CommunityPost p SET p.likesCount = p.likesCount + :delta WHERE p.id = :postId")
    void updateLikesCount(Long postId, int delta);

    @Modifying
    @Query("UPDATE CommunityPost p SET p.commentsCount = p.commentsCount + :delta WHERE p.id = :postId")
    void updateCommentsCount(Long postId, int delta);
    void deleteAllByUserId(Long userId);
}
