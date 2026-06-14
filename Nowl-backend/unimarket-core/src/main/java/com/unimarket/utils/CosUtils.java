package com.unimarket.utils;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 腾讯云COS工具类（支持本地存储降级）
 */
@Slf4j
@Component
public class CosUtils {

    @Value("${cos.secret-id:}")
    private String secretId;

    @Value("${cos.secret-key:}")
    private String secretKey;

    @Value("${cos.region:ap-guangzhou}")
    private String regionName;

    @Value("${cos.bucket-name:}")
    private String bucketName;

    @Value("${cos.base-url:}")
    private String baseUrl;

    @Value("${system.upload-path:/tmp/unimarket/uploads}")
    private String uploadPath;

    private COSClient cosClient;

    /** 是否使用本地存储（COS 未配置时自动降级） */
    private boolean useLocalStorage = false;

    @PostConstruct
    public void init() {
        // 判断是否配置了有效的 COS 凭证
        if (isBlank(secretId) || isBlank(secretKey) || isBlank(bucketName)) {
            useLocalStorage = true;
            log.info("COS 未配置，使用本地文件存储，路径: {}", uploadPath);
            try {
                Files.createDirectories(Paths.get(uploadPath));
            } catch (IOException e) {
                log.error("创建本地存储目录失败", e);
            }
            return;
        }
        // 初始化 COS 客户端
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        Region region = new Region(regionName);
        ClientConfig clientConfig = new ClientConfig(region);
        cosClient = new COSClient(cred, clientConfig);
        log.info("腾讯云COS客户端初始化成功, region={}, bucket={}", regionName, bucketName);
    }

    /**
     * 上传文件
     */
    public String upload(InputStream inputStream, String originalFilename, long size, String contentType) {
        return upload(inputStream, originalFilename, size, contentType, "uploads/");
    }

    public String upload(InputStream inputStream, String originalFilename, long size, String contentType, String keyPrefix) {
        if (useLocalStorage) {
            return uploadLocal(inputStream, originalFilename, keyPrefix);
        }
        try {
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String normalizedPrefix = normalizePrefix(keyPrefix);
            String key = normalizedPrefix + UUID.randomUUID() + extension;

            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(size);
            objectMetadata.setContentType(contentType);

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, inputStream, objectMetadata);
            cosClient.putObject(putObjectRequest);

            return getNormalizedBaseUrl() + "/" + key;
        } catch (Exception e) {
            log.error("COS上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传文件（MultipartFile 版本）
     */
    public String upload(MultipartFile file) {
        return upload(file, "uploads/");
    }

    public String upload(MultipartFile file, String keyPrefix) {
        try {
            return upload(file.getInputStream(), file.getOriginalFilename(), file.getSize(), file.getContentType(), keyPrefix);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     */
    public void delete(String fileUrl) {
        if (fileUrl == null) return;
        if (useLocalStorage) {
            deleteLocal(fileUrl);
            return;
        }
        try {
            String key = extractKey(fileUrl);
            cosClient.deleteObject(bucketName, key);
        } catch (Exception e) {
            log.error("COS删除失败, fileUrl: {}", fileUrl, e);
            throw new RuntimeException("文件删除失败: " + e.getMessage());
        }
    }

    public boolean isOwnedByUser(String fileUrl, Long userId) {
        if (userId == null || userId <= 0 || fileUrl == null) return false;
        String key = extractKey(fileUrl);
        return key.startsWith("uploads/" + userId + "/");
    }

    public String extractKey(String fileUrl) {
        if (useLocalStorage) {
            // 本地存储 URL 格式: /uploads/xxx
            String key = fileUrl;
            if (key.startsWith("/")) key = key.substring(1);
            return key;
        }
        String normalizedBaseUrl = getNormalizedBaseUrl();
        if (fileUrl == null || !fileUrl.startsWith(normalizedBaseUrl + "/")) {
            throw new IllegalArgumentException("fileUrl is invalid");
        }
        String key = fileUrl.substring((normalizedBaseUrl + "/").length());
        int queryIdx = key.indexOf('?');
        if (queryIdx >= 0) key = key.substring(0, queryIdx);
        int fragmentIdx = key.indexOf('#');
        if (fragmentIdx >= 0) key = key.substring(0, fragmentIdx);
        if (key.isBlank()) throw new IllegalArgumentException("fileUrl is invalid");
        return key;
    }

    /**
     * 列出指定前缀下、早于指定时间的文件URL
     */
    public List<String> listObjectUrls(String prefix, Date before, int maxKeys) {
        if (useLocalStorage) {
            return List.of(); // 本地存储暂不支持列举
        }
        List<String> urls = new ArrayList<>();
        String marker = null;
        String normalizedBase = getNormalizedBaseUrl();
        do {
            ListObjectsRequest req = new ListObjectsRequest();
            req.setBucketName(bucketName);
            req.setPrefix(prefix);
            req.setMaxKeys(Math.min(maxKeys - urls.size(), 1000));
            if (marker != null) req.setMarker(marker);
            ObjectListing listing = cosClient.listObjects(req);
            for (COSObjectSummary summary : listing.getObjectSummaries()) {
                if (before != null && summary.getLastModified().after(before)) continue;
                urls.add(normalizedBase + "/" + summary.getKey());
                if (urls.size() >= maxKeys) return urls;
            }
            marker = listing.isTruncated() ? listing.getNextMarker() : null;
        } while (marker != null && urls.size() < maxKeys);
        return urls;
    }

    // ==================== 本地存储实现 ====================

    private String uploadLocal(InputStream inputStream, String originalFilename, String keyPrefix) {
        try {
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String normalizedPrefix = normalizePrefix(keyPrefix);
            String key = normalizedPrefix + UUID.randomUUID() + extension;

            Path filePath = Paths.get(uploadPath, key);
            Files.createDirectories(filePath.getParent());
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("本地文件上传成功: {}", key);
            // 返回相对路径，通过 /uploads/** 静态资源映射访问
            return "/" + key;
        } catch (Exception e) {
            log.error("本地文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    private void deleteLocal(String fileUrl) {
        try {
            String key = extractKey(fileUrl);
            Path filePath = Paths.get(uploadPath, key);
            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            log.error("本地文件删除失败, fileUrl: {}", fileUrl, e);
        }
    }

    // ==================== 工具方法 ====================

    private String normalizePrefix(String keyPrefix) {
        if (keyPrefix == null || keyPrefix.isBlank()) return "uploads/";
        String normalized = keyPrefix.trim();
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    private String getNormalizedBaseUrl() {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("baseUrl is not configured");
        }
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private boolean isBlank(String str) {
        return str == null || str.isBlank();
    }
}
