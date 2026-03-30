package com.nutriai.repository;

import com.nutriai.entity.AppVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppVersionRepository extends JpaRepository<AppVersion, Long> {

    Page<AppVersion> findByPlatformOrderByVersionCodeDesc(String platform, Pageable pageable);

    List<AppVersion> findByPlatformOrderByVersionCodeDesc(String platform);

    Optional<AppVersion> findByPlatformAndIsLatestTrue(String platform);

    @Modifying
    @Query("UPDATE AppVersion a SET a.isLatest = false WHERE a.platform = :platform")
    void clearLatestByPlatform(String platform);

    @Modifying
    @Query("UPDATE AppVersion a SET a.downloadCount = a.downloadCount + 1 WHERE a.id = :id")
    void incrementDownloadCount(Long id);
}
