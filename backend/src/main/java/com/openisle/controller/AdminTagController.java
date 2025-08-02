package com.openisle.controller;

import com.openisle.model.Tag;
import com.openisle.service.TagService;
import com.openisle.service.PostService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/tags")
@RequiredArgsConstructor
public class AdminTagController {
    private final TagService tagService;
    private final PostService postService;

    @GetMapping("/pending")
    public List<TagDto> pendingTags() {
        return tagService.listPendingTags().stream()
                .map(t -> toDto(t, postService.countPostsByTag(t.getId())))
                .collect(Collectors.toList());
    }

    @PostMapping("/{id}/approve")
    public TagDto approve(@PathVariable Long id) {
        Tag tag = tagService.approveTag(id);
        long count = postService.countPostsByTag(tag.getId());
        return toDto(tag, count);
    }

    private TagDto toDto(Tag tag, long count) {
        TagDto dto = new TagDto();
        dto.setId(tag.getId());
        dto.setName(tag.getName());
        dto.setDescription(tag.getDescription());
        dto.setIcon(tag.getIcon());
        dto.setSmallIcon(tag.getSmallIcon());
        dto.setCount(count);
        return dto;
    }

    @Data
    private static class TagDto {
        private Long id;
        private String name;
        private String description;
        private String icon;
        private String smallIcon;
        private Long count;
    }
}
