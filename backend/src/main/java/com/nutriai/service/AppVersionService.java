package com.nutriai.service;

import com.nutriai.common.ApiResponse;
import com.nutriai.entity.AppVersion;
import com.nutriai.exception.BusinessException;
import com.nutriai.repository.AppVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppVersionService {

    private final AppVersionRepository appVersionRepository;
    private final OssService ossService;

    /**
     * 上传新版本
     */
    @Transactional
    public AppVersion createVersion(String versionName, Integer versionCode, String platform,
                                     String description, MultipartFile file) {
        String downloadUrl = ossService.uploadApk(file);

        // Clear previous latest flag
        appVersionRepository.clearLatestByPlatform(platform);

        AppVersion version = AppVersion.builder()
                .versionName(versionName)
                .versionCode(versionCode)
                .platform(platform)
                .description(description)
                .downloadUrl(downloadUrl)
                .fileSize(file.getSize())
                .isLatest(true)
                .downloadCount(0)
                .build();

        return appVersionRepository.save(version);
    }

    /**
     * 获取最新版本
     */
    public AppVersion getLatestVersion(String platform) {
        return appVersionRepository.findByPlatformAndIsLatestTrue(platform).orElse(null);
    }

    /**
     * 获取版本列表（分页）
     */
    public Page<AppVersion> getVersionList(String platform, int page, int size) {
        return appVersionRepository.findByPlatformOrderByVersionCodeDesc(platform, PageRequest.of(page, size));
    }

    /**
     * 获取所有版本（不分页）
     */
    public List<AppVersion> getAllVersions(String platform) {
        return appVersionRepository.findByPlatformOrderByVersionCodeDesc(platform);
    }

    /**
     * 记录下载并返回下载URL
     */
    @Transactional
    public String download(Long id) {
        AppVersion version = appVersionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("版本不存在"));
        appVersionRepository.incrementDownloadCount(id);
        return version.getDownloadUrl();
    }

    /**
     * 设置为最新版本
     */
    @Transactional
    public AppVersion setLatest(Long id) {
        AppVersion version = appVersionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("版本不存在"));
        appVersionRepository.clearLatestByPlatform(version.getPlatform());
        version.setIsLatest(true);
        return appVersionRepository.save(version);
    }

    /**
     * 更新版本信息
     */
    @Transactional
    public AppVersion updateVersion(Long id, String description) {
        AppVersion version = appVersionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("版本不存在"));
        if (description != null) {
            version.setDescription(description);
        }
        return appVersionRepository.save(version);
    }

    /**
     * 删除版本
     */
    @Transactional
    public void deleteVersion(Long id) {
        AppVersion version = appVersionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("版本不存在"));
        // Delete file from COS
        ossService.deleteFile(version.getDownloadUrl());
        appVersionRepository.delete(version);
    }
}
