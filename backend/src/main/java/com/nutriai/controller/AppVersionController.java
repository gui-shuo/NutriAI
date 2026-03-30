package com.nutriai.controller;

import com.nutriai.common.ApiResponse;
import com.nutriai.entity.AppVersion;
import com.nutriai.service.AppVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/app-versions")
@RequiredArgsConstructor
public class AppVersionController {

    private final AppVersionService appVersionService;

    /**
     * 获取最新版本（公开）
     */
    @GetMapping("/latest")
    public ApiResponse<AppVersion> getLatest(
            @RequestParam(defaultValue = "android") String platform) {
        AppVersion version = appVersionService.getLatestVersion(platform);
        return ApiResponse.success(version);
    }

    /**
     * 获取版本列表（公开）
     */
    @GetMapping("/list")
    public ApiResponse<Page<AppVersion>> getList(
            @RequestParam(defaultValue = "android") String platform,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(appVersionService.getVersionList(platform, page, size));
    }

    /**
     * 下载（记录下载次数，返回下载URL）
     */
    @GetMapping("/download/{id}")
    public ApiResponse<String> download(@PathVariable Long id) {
        String url = appVersionService.download(id);
        return ApiResponse.success("获取下载链接成功", url);
    }

    // ========== 管理员接口 ==========

    /**
     * 上传新版本（管理员）
     */
    @PostMapping("/admin/upload")
    public ApiResponse<AppVersion> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam String versionName,
            @RequestParam Integer versionCode,
            @RequestParam(defaultValue = "android") String platform,
            @RequestParam(required = false) String description) {
        AppVersion version = appVersionService.createVersion(
                versionName, versionCode, platform, description, file);
        return ApiResponse.success("版本上传成功", version);
    }

    /**
     * 设置为最新版本（管理员）
     */
    @PutMapping("/admin/{id}/set-latest")
    public ApiResponse<AppVersion> setLatest(@PathVariable Long id) {
        return ApiResponse.success("设置成功", appVersionService.setLatest(id));
    }

    /**
     * 更新版本信息（管理员）
     */
    @PutMapping("/admin/{id}")
    public ApiResponse<AppVersion> update(
            @PathVariable Long id,
            @RequestParam(required = false) String description) {
        return ApiResponse.success("更新成功", appVersionService.updateVersion(id, description));
    }

    /**
     * 删除版本（管理员）
     */
    @DeleteMapping("/admin/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        appVersionService.deleteVersion(id);
        return ApiResponse.success("删除成功", null);
    }
}
