package com.openisle.controller;

import com.openisle.model.Post;
import com.openisle.service.PostService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Endpoints for administrators to manage posts.
 */
@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
public class AdminPostController {
    private final PostService postService;

    @GetMapping("/pending")
    public List<PostDto> pendingPosts() {
        return postService.listPendingPosts().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @PostMapping("/{id}/approve")
    public PostDto approve(@PathVariable Long id) {
        return toDto(postService.approvePost(id));
    }

    @PostMapping("/{id}/reject")
    public PostDto reject(@PathVariable Long id) {
        return toDto(postService.rejectPost(id));
    }

    @PostMapping("/{id}/pin")
    public PostDto pin(@PathVariable Long id) {
        return toDto(postService.pinPost(id));
    }

    @PostMapping("/{id}/unpin")
    public PostDto unpin(@PathVariable Long id) {
        return toDto(postService.unpinPost(id));
    }

    private PostDto toDto(Post post) {
        PostDto dto = new PostDto();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setAuthor(post.getAuthor().getUsername());
        dto.setCategory(toCategoryDto(post.getCategory()));
        dto.setViews(post.getViews());
        dto.setStatus(post.getStatus());
        dto.setPinnedAt(post.getPinnedAt());
        return dto;
    }

    private CategoryDto toCategoryDto(com.openisle.model.Category c) {
        CategoryDto dto = new CategoryDto();
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setDescription(c.getDescription());
        dto.setIcon(c.getIcon());
        dto.setSmallIcon(c.getSmallIcon());
        return dto;
    }

    @Data
    private static class PostDto {
        private Long id;
        private String title;
        private String content;
        private LocalDateTime createdAt;
        private String author;
        private CategoryDto category;
        private long views;
        private com.openisle.model.PostStatus status;
        private LocalDateTime pinnedAt;
    }

    @Data
    private static class CategoryDto {
        private Long id;
        private String name;
        private String description;
        private String icon;
        private String smallIcon;
    }
}
