package com.openisle.service;

import com.openisle.model.Image;
import com.openisle.repository.ImageRepository;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract service for uploading images and tracking their references.
 */
public abstract class ImageUploader {
    private final ImageRepository imageRepository;
    private final String baseUrl;
    private final Pattern urlPattern;

    protected ImageUploader(ImageRepository imageRepository, String baseUrl) {
        this.imageRepository = imageRepository;
        if (baseUrl.endsWith("/")) {
            this.baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        } else {
            this.baseUrl = baseUrl;
        }
        this.urlPattern = Pattern.compile(Pattern.quote(this.baseUrl) + "/[^\\s)]+");
    }

    /**
     * Upload an image asynchronously and return a future of its accessible URL.
     */
    public CompletableFuture<String> upload(byte[] data, String filename) {
        return doUpload(data, filename).thenApply(url -> url);
    }

    protected abstract CompletableFuture<String> doUpload(byte[] data, String filename);

    protected abstract void deleteFromStore(String key);

    /**
     * Generate a presigned PUT URL for direct browser upload.
     * Default implementation is unsupported.
     */
    public java.util.Map<String, String> presignUpload(String filename) {
        throw new UnsupportedOperationException("presignUpload not supported");
    }

    /** Extract COS URLs from text. */
    public Set<String> extractUrls(String text) {
        Set<String> set = new HashSet<>();
        if (text == null) return set;
        Matcher m = urlPattern.matcher(text);
        while (m.find()) {
            set.add(m.group());
        }
        return set;
    }

    public void addReferences(Set<String> urls) {
        for (String u : urls) addReference(u);
    }

    public void removeReferences(Set<String> urls) {
        for (String u : urls) removeReference(u);
    }

    public void adjustReferences(String oldText, String newText) {
        Set<String> oldUrls = extractUrls(oldText);
        Set<String> newUrls = extractUrls(newText);
        for (String u : newUrls) {
            if (!oldUrls.contains(u)) addReference(u);
        }
        for (String u : oldUrls) {
            if (!newUrls.contains(u)) removeReference(u);
        }
    }

    private void addReference(String url) {
        if (!url.startsWith(baseUrl)) return;
        imageRepository.findByUrl(url).ifPresentOrElse(img -> {
            img.setRefCount(img.getRefCount() + 1);
            imageRepository.save(img);
        }, () -> {
            Image img = new Image();
            img.setUrl(url);
            img.setRefCount(1);
            imageRepository.save(img);
        });
    }

    private void removeReference(String url) {
        if (!url.startsWith(baseUrl)) return;
        imageRepository.findByUrl(url).ifPresent(img -> {
            long count = img.getRefCount() - 1;
            if (count <= 0) {
                imageRepository.delete(img);
                String key = url.substring(baseUrl.length() + 1);
                deleteFromStore(key);
            } else {
                img.setRefCount(count);
                imageRepository.save(img);
            }
        });
    }
}
