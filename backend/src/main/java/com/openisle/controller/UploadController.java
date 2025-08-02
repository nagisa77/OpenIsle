package com.openisle.controller;

import com.openisle.service.ImageUploader;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {
    private final ImageUploader imageUploader;

    @Value("${app.upload.check-type:true}")
    private boolean checkImageType;

    @Value("${app.upload.max-size:5242880}")
    private long maxUploadSize;

    @PostMapping
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        if (checkImageType && (file.getContentType() == null || !file.getContentType().startsWith("image/"))) {
            return ResponseEntity.badRequest().body(Map.of("code", 1, "msg", "File is not an image"));
        }
        if (file.getSize() > maxUploadSize) {
            return ResponseEntity.badRequest().body(Map.of("code", 2, "msg", "File too large"));
        }
        String url;
        try {
            url = imageUploader.upload(file.getBytes(), file.getOriginalFilename()).join();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("code", 3, "msg", "Upload failed"));
        }
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "msg", "ok",
                "data", Map.of("url", url)
        ));
    }

    @PostMapping("/url")
    public ResponseEntity<?> uploadUrl(@RequestBody Map<String, String> body) {
        String link = body.get("url");
        if (link == null || link.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("code", 1, "msg", "Missing url"));
        }
        try {
            URL u = URI.create(link).toURL();
            byte[] data = u.openStream().readAllBytes();
            if (data.length > maxUploadSize) {
                return ResponseEntity.badRequest().body(Map.of("code", 2, "msg", "File too large"));
            }
            String filename = link.substring(link.lastIndexOf('/') + 1);
            String contentType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(data));
            if (checkImageType && (contentType == null || !contentType.startsWith("image/"))) {
                return ResponseEntity.badRequest().body(Map.of("code", 1, "msg", "File is not an image"));
            }
            String url = imageUploader.upload(data, filename).join();
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "msg", "ok",
                    "data", Map.of("url", url)
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("code", 3, "msg", "Upload failed"));
        }
    }

    @GetMapping("/presign")
    public java.util.Map<String, String> presign(@RequestParam("filename") String filename) {
        return imageUploader.presignUpload(filename);
    }
}
