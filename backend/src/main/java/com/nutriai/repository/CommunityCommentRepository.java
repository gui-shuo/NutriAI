package com.nutriai.repository;

import com.nutriai.entity.CommunityComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {

    Page<CommunityComment> findByPostIdOrderByCreatedAtAsc(Long postId, Pageable pageable);

    List<CommunityComment> findByPostIdOrderByCreatedAtAsc(Long postId);

    long countByPostId(Long postId);

    void deleteByPostId(Long postId);

    @Modifying
    @Query("UPDATE CommunityComment c SET c.likesCount = c.likesCount + :delta WHERE c.id = :commentId")
    void updateLikesCount(Long commentId, int delta);
    void deleteAllByUserId(Long userId);
}
