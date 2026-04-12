package com.nutriai.repository;

import com.nutriai.entity.AnnouncementRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, Long> {

    Optional<AnnouncementRead> findByUserIdAndAnnouncementId(Long userId, Long announcementId);

    List<AnnouncementRead> findByUserId(Long userId);

    @Query("SELECT ar.announcementId FROM AnnouncementRead ar WHERE ar.userId = :userId")
    Set<Long> findReadAnnouncementIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(a) FROM SystemAnnouncement a WHERE a.isActive = true " +
           "AND a.id NOT IN (SELECT ar.announcementId FROM AnnouncementRead ar WHERE ar.userId = :userId)")
    long countUnreadByUserId(@Param("userId") Long userId);

    void deleteByAnnouncementId(Long announcementId);
}
