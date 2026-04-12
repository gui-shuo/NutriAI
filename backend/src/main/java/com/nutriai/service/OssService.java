package com.nutriai.service;

import com.nutriai.config.CosConfig;
import com.nutriai.exception.BusinessException;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.CannedAccessControlList;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 文件上传服务 - 腾讯云COS对象存储（未配置时回退到本地存储）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssService {

    private static final String LOCAL_APK_DIR = "/app/releases/apk";

    private final COSClient cosClient;
    private final CosConfig cosConfig;

    @Value("${nutriai.upload.max-size:10485760}")
    private Long maxFileSize; // 默认10MB

    @Value("${nutriai.upload.allowed-types:jpg,jpeg,png,gif}")
    private String allowedTypes;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
        "image/jpeg", "image/png", "image/gif", "image/jpg"
    );

    /**
     * 上传头像
     */
    public String uploadAvatar(MultipartFile file) {
        validateFile(file);
        String extension = getFileExtension(file.getOriginalFilename());
        String key = "avatars/avatar_" + UUID.randomUUID() + "." + extension;
        return uploadToCos(file, key);
    }

    /**
     * 上传食物照片
     */
    public String uploadFoodPhoto(MultipartFile file) {
        validateFile(file);
        String extension = getFileExtension(file.getOriginalFilename());
        String key = "foods/food_" + UUID.randomUUID() + "." + extension;
        return uploadToCos(file, key);
    }

    private static final List<String> ALLOWED_VIDEO_TYPES = Arrays.asList(
        "video/mp4", "video/quicktime", "video/webm"
    );

    /**
     * 上传社区媒体（图片或视频）
     */
    public String uploadCommunityMedia(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        if (file.getSize() > 50 * 1024 * 1024) {
            throw new BusinessException("文件大小不能超过50MB");
        }
        String contentType = file.getContentType();
        boolean isImage = contentType != null && ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase());
        boolean isVideo = contentType != null && ALLOWED_VIDEO_TYPES.contains(contentType.toLowerCase());
        if (!isImage && !isVideo) {
            throw new BusinessException("仅支持 jpg/png/gif 图片和 mp4/webm 视频");
        }
        String extension = getFileExtension(file.getOriginalFilename());
        String prefix = isVideo ? "community/videos/vid_" : "community/images/img_";
        String key = prefix + UUID.randomUUID() + "." + extension;
        return uploadToCos(file, key);
    }

    /**
     * 上传APK安装包到COS（通过自定义域名提供下载，绕过默认域名APK限制）
     */
    public String uploadApk(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        if (file.getSize() > 200 * 1024 * 1024) {
            throw new BusinessException("APK文件大小不能超过200MB");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".apk")) {
            throw new BusinessException("仅支持APK文件");
        }
        String key = "releases/apk/" + UUID.randomUUID() + ".apk";
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType("application/vnd.android.package-archive");
            PutObjectRequest putRequest = new PutObjectRequest(
                cosConfig.getBucket(), key, file.getInputStream(), metadata
            );
            putRequest.setCannedAcl(CannedAccessControlList.PublicRead);
            cosClient.putObject(putRequest);

            // 使用自定义域名URL，绕过COS默认域名的APK下载限制
            String downloadUrl = cosConfig.getDownloadBaseUrl() + "/" + key;
            log.info("APK上传COS成功: key={}, downloadUrl={}, size={}", key, downloadUrl, file.getSize());
            return downloadUrl;
        } catch (Exception e) {
            log.error("APK上传COS失败: key={}", key, e);
            throw BusinessException.User.fileUploadFailed();
        }
    }

    /**
     * 上传资质证书图片
     */
    public String uploadCertificate(MultipartFile file) {
        validateFile(file);
        String extension = getFileExtension(file.getOriginalFilename());
        String key = "certificates/cert_" + UUID.randomUUID() + "." + extension;
        return uploadToCos(file, key);
    }

    /**
     * 上传文件到腾讯云COS
     */
    private String uploadToCos(MultipartFile file, String key) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            PutObjectRequest putRequest = new PutObjectRequest(
                cosConfig.getBucket(), key, file.getInputStream(), metadata
            );
            putRequest.setCannedAcl(CannedAccessControlList.PublicRead);
            cosClient.putObject(putRequest);

            String fileUrl = cosConfig.getCosBaseUrl() + "/" + key;
            log.info("文件上传COS成功: key={}, url={}, size={}", key, fileUrl, file.getSize());
            return fileUrl;
        } catch (Exception e) {
            log.error("文件上传COS失败: key={}", key, e);
            throw BusinessException.User.fileUploadFailed();
        }
    }

    /**
     * 删除文件（支持本地文件、COS默认域名URL和自定义域名URL）
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }
        try {
            if (fileUrl.startsWith("local:")) {
                String fileName = fileUrl.substring("local:".length());
                java.io.File file = new java.io.File(LOCAL_APK_DIR, fileName);
                if (file.exists() && file.delete()) {
                    log.info("本地文件删除成功: {}", file.getAbsolutePath());
                }
            } else {
                String key = extractCosKey(fileUrl);
                if (key != null) {
                    cosClient.deleteObject(cosConfig.getBucket(), key);
                    log.info("COS文件删除成功: key={}", key);
                }
            }
        } catch (Exception e) {
            log.error("文件删除失败: {}", fileUrl, e);
        }
    }

    /**
     * 从URL中提取COS对象key（支持默认域名和自定义域名）
     */
    private String extractCosKey(String fileUrl) {
        String cosBaseUrl = cosConfig.getCosBaseUrl();
        String downloadBaseUrl = cosConfig.getDownloadBaseUrl();
        if (fileUrl.startsWith(cosBaseUrl + "/")) {
            return fileUrl.substring(cosBaseUrl.length() + 1);
        }
        if (!downloadBaseUrl.equals(cosBaseUrl) && fileUrl.startsWith(downloadBaseUrl + "/")) {
            return fileUrl.substring(downloadBaseUrl.length() + 1);
        }
        return null;
    }

    /**
     * 流式下载文件（支持本地文件、COS默认域名URL和自定义域名URL）
     */
    public void streamFile(String fileUrl, java.io.OutputStream outputStream) {
        try {
            java.io.InputStream inputStream;
            if (fileUrl != null && fileUrl.startsWith("local:")) {
                String fileName = fileUrl.substring("local:".length());
                java.io.File file = new java.io.File(LOCAL_APK_DIR, fileName);
                if (!file.exists()) {
                    throw new BusinessException("文件不存在");
                }
                inputStream = new java.io.FileInputStream(file);
                log.info("本地文件流式下载: {}", file.getAbsolutePath());
            } else {
                String key = extractCosKey(fileUrl);
                if (key == null) {
                    throw new BusinessException("无效的文件地址");
                }
                com.qcloud.cos.model.COSObject cosObject = cosClient.getObject(cosConfig.getBucket(), key);
                inputStream = cosObject.getObjectContent();
                log.info("COS文件流式下载: key={}", key);
            }

            try (inputStream) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件流式下载失败: {}", fileUrl, e);
            throw new BusinessException("文件下载失败");
        }
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        if (file.getSize() > maxFileSize) {
            throw BusinessException.User.fileSizeExceeded();
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw BusinessException.User.fileTypeNotAllowed();
        }
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new BusinessException("文件名不能为空");
        }
        String extension = getFileExtension(filename).toLowerCase();
        List<String> allowedExtensions = Arrays.asList(allowedTypes.split(","));
        if (!allowedExtensions.contains(extension)) {
            throw BusinessException.User.fileTypeNotAllowed();
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
