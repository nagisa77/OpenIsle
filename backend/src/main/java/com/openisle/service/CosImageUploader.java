package com.openisle.service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.region.Region;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ImageUploader implementation using Tencent Cloud COS.
 */
@Service
public class CosImageUploader extends ImageUploader {

    private final COSClient cosClient;
    private final String bucketName;
    private final String baseUrl;
    private static final String UPLOAD_DIR = "dynamic_assert/";
    private static final Logger logger = LoggerFactory.getLogger(CosImageUploader.class);
    private final ExecutorService executor = Executors.newFixedThreadPool(2,
            new CustomizableThreadFactory("cos-upload-"));

    @org.springframework.beans.factory.annotation.Autowired
    public CosImageUploader(
            com.openisle.repository.ImageRepository imageRepository,
            @Value("${cos.secret-id:}") String secretId,
            @Value("${cos.secret-key:}") String secretKey,
            @Value("${cos.region:ap-guangzhou}") String region,
            @Value("${cos.bucket-name:}") String bucketName,
            @Value("${cos.base-url:https://example.com}") String baseUrl) {
        super(imageRepository, baseUrl);
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig config = new ClientConfig(new Region(region));
        this.cosClient = new COSClient(cred, config);
        this.bucketName = bucketName;
        this.baseUrl = baseUrl;
        logger.debug("COS client initialized for region {} with bucket {}", region, bucketName);
    }

    // for tests
    CosImageUploader(COSClient cosClient,
                     com.openisle.repository.ImageRepository imageRepository,
                     String bucketName,
                     String baseUrl) {
        super(imageRepository, baseUrl);
        this.cosClient = cosClient;
        this.bucketName = bucketName;
        this.baseUrl = baseUrl;
        logger.debug("COS client provided directly with bucket {}", bucketName);
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

            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(data.length);
            PutObjectRequest req = new PutObjectRequest(
                    bucketName,
                    objectKey,
                    new ByteArrayInputStream(data),
                    meta);
            logger.debug("Sending PutObject request to bucket {}", bucketName);
            cosClient.putObject(req);
            String url = baseUrl + "/" + objectKey;
            logger.debug("Upload successful, accessible at {}", url);
            return url;
        }, executor);
    }

    @Override
    protected void deleteFromStore(String key) {
        try {
            cosClient.deleteObject(bucketName, key);
        } catch (Exception e) {
            logger.warn("Failed to delete image {} from COS", key, e);
        }
    }

    @Override
    public java.util.Map<String, String> presignUpload(String filename) {
        String ext = "";
        int dot = filename.lastIndexOf('.');
        if (dot != -1) {
            ext = filename.substring(dot);
        }
        String randomName = java.util.UUID.randomUUID().toString().replace("-", "") + ext;
        String objectKey = UPLOAD_DIR + randomName;
        java.util.Date expiration = new java.util.Date(System.currentTimeMillis() + 15 * 60 * 1000L);
        GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(bucketName, objectKey, HttpMethodName.PUT);
        req.setExpiration(expiration);
        java.net.URL url = cosClient.generatePresignedUrl(req);
        String fileUrl = baseUrl + "/" + objectKey;
        return java.util.Map.of(
                "uploadUrl", url.toString(),
                "fileUrl", fileUrl,
                "key", objectKey
        );
    }
}
