package com.openisle.service;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Service;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.openisle.repository.ImageRepository;

import jakarta.annotation.PreDestroy;

/**
 * ImageUploader implementation using Alibaba Cloud OSS.
 */
@Service
public class OssImageUploader extends ImageUploader {

    private final OSS ossClient;
    private final String bucketName;
    private final String baseUrl; // 新增：与 COS 版本一致
    private static final String UPLOAD_DIR = "dynamic_assert/";
    private static final Logger logger = LoggerFactory.getLogger(OssImageUploader.class);
    private final ExecutorService executor = Executors.newFixedThreadPool(2,
            new CustomizableThreadFactory("oss-upload-"));

    @Autowired
    public OssImageUploader(
            ImageRepository imageRepository,
            @Value("${aliyun.oss.endpoint:}") String endpoint,
            @Value("${aliyun.oss.bucket-name:}") String bucketName,
            @Value("${aliyun.oss.access-key-id:}") String accessKeyId,
            @Value("${aliyun.oss.access-key-secret:}") String accessKeySecret,
            @Value("${aliyun.oss.base-url:https://example.com}") String baseUrl) {
        super(imageRepository, baseUrl);
        this.ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        this.bucketName = bucketName;
        this.baseUrl = baseUrl; // 记录在子类，便于拼接
        logger.debug("OSS client initialized for endpoint {} with bucket {}", endpoint, bucketName);
    }

    // for tests
    OssImageUploader(OSS ossClient,
                     ImageRepository imageRepository,
                     String bucketName,
                     String baseUrl) {
        super(imageRepository, baseUrl);
        this.ossClient = ossClient;
        this.bucketName = bucketName;
        this.baseUrl = baseUrl;
        logger.debug("OSS client provided directly with bucket {}", bucketName);
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (this.ossClient != null) {
                this.ossClient.shutdown();
            }
        } finally {
            executor.shutdown(); // 建议一并关闭线程池
        }
    }

    @Override
    protected CompletableFuture<String> doUpload(byte[] data, String filename) {
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("Uploading {} bytes as {}", data.length, filename);
            String ext = "";
            int dot = filename.lastIndexOf('.');
            if (dot != -1) {
                ext = filename.substring(dot);
            }
            String randomName = UUID.randomUUID().toString().replace("-", "") + ext;
            String objectKey = UPLOAD_DIR + randomName;
            logger.debug("Generated object key {}", objectKey);

            logger.debug("Sending PutObject request to bucket {}", bucketName);
            ossClient.putObject(bucketName, objectKey, new ByteArrayInputStream(data));

            String url = baseUrl + "/" + objectKey; // 使用子类保存的 baseUrl
            logger.debug("Upload successful, accessible at {}", url);
            return url;
        }, executor);
    }

    @Override
    protected void deleteFromStore(String key) {
        try {
            ossClient.deleteObject(bucketName, key);
        } catch (Exception e) {
            logger.warn("Failed to delete image {} from OSS", key, e);
        }
    }

    @Override
    public Map<String, String> presignUpload(String filename) {
        String ext = "";
        int dot = filename.lastIndexOf('.');
        if (dot != -1) {
            ext = filename.substring(dot);
        }
        String randomName = UUID.randomUUID().toString().replace("-", "") + ext;
        String objectKey = UPLOAD_DIR + randomName;

        Date expiration = new Date(System.currentTimeMillis() + 15 * 60 * 1000L);
        URL url = ossClient.generatePresignedUrl(bucketName, objectKey, expiration, HttpMethod.PUT);

        String fileUrl = baseUrl + "/" + objectKey;

        Map<String, String> result = new HashMap<>();
        result.put("uploadUrl", url.toString());
        result.put("fileUrl", fileUrl);
        result.put("key", objectKey);
        return result;
    }
}