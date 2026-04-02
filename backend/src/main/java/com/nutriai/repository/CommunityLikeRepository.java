package com.nutriai.repository;

import com.nutriai.entity.CommunityLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommunityLikeRepository extends JpaRepository<CommunityLike, Long> {

    Optional<CommunityLike> findByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);

    boolean existsByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);

    List<CommunityLike> findByUserIdAndTargetTypeAndTargetIdIn(Long userId, String targetType, List<Long> targetIds);

    void deleteByTargetTypeAndTargetId(String targetType, Long targetId);
    void deleteAllByUserId(Long userId);
}
