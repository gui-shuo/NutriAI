package com.nutriai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_versions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version_name", nullable = false, length = 50)
    private String versionName;

    @Column(name = "version_code", nullable = false)
    private Integer versionCode;

    @Column(length = 20, nullable = false)
    @Builder.Default
    private String platform = "android";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "download_url", nullable = false, length = 500)
    private String downloadUrl;

    @Column(name = "file_size")
    @Builder.Default
    private Long fileSize = 0L;

    @Column(name = "is_latest")
    @Builder.Default
    private Boolean isLatest = false;

    @Column(name = "download_count")
    @Builder.Default
    private Integer downloadCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
