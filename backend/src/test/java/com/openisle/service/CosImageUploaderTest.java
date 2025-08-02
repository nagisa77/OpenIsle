package com.openisle.service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.openisle.repository.ImageRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CosImageUploaderTest {
    @Test
    void uploadReturnsUrl() {
        COSClient client = mock(COSClient.class);
        ImageRepository repo = mock(ImageRepository.class);
        CosImageUploader uploader = new CosImageUploader(client, repo, "bucket", "http://cos.example.com");

        String url = uploader.upload("data".getBytes(), "img.png").join();

        verify(client).putObject(any(PutObjectRequest.class));
        assertTrue(url.matches("http://cos.example.com/dynamic_assert/[a-f0-9]{32}\\.png"));
    }
}
