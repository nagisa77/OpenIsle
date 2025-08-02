package com.openisle.controller;

import com.openisle.model.Tag;
import com.openisle.service.TagService;
import com.openisle.service.PostService;
import com.openisle.repository.UserRepository;
import com.openisle.model.PublishMode;
import com.openisle.model.Role;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {
    private final TagService tagService;
    private final PostService postService;
    private final UserRepository userRepository;

    @PostMapping
    public TagDto create(@RequestBody TagRequest req, org.springframework.security.core.Authentication auth) {
        boolean approved = true;
        if (postService.getPublishMode() == PublishMode.REVIEW && auth != null) {
            com.openisle.model.User user = userRepository.findByUsername(auth.getName()).orElseThrow();
            if (user.getRole() != Role.ADMIN) {
                approved = false;
            }
        }
        Tag tag = tagService.createTag(
                req.getName(),
                req.getDescription(),
                req.getIcon(),
                req.getSmallIcon(),
                approved,
                auth != null ? auth.getName() : null);
        long count = postService.countPostsByTag(tag.getId());
        return toDto(tag, count);
    }

    @PutMapping("/{id}")
    public TagDto update(@PathVariable Long id, @RequestBody TagRequest req) {
        Tag tag = tagService.updateTag(id, req.getName(), req.getDescription(), req.getIcon(), req.getSmallIcon());
        long count = postService.countPostsByTag(tag.getId());
        return toDto(tag, count);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        tagService.deleteTag(id);
    }

    @GetMapping
    public List<TagDto> list(@RequestParam(value = "keyword", required = false) String keyword,
                             @RequestParam(value = "limit", required = false) Integer limit) {
        List<TagDto> dtos = tagService.searchTags(keyword).stream()
                .map(t -> toDto(t, postService.countPostsByTag(t.getId())))
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .collect(Collectors.toList());
        if (limit != null && limit > 0 && dtos.size() > limit) {
            return dtos.subList(0, limit);
        }
        return dtos;
    }

    @GetMapping("/{id}")
    public TagDto get(@PathVariable Long id) {
        Tag tag = tagService.getTag(id);
        long count = postService.countPostsByTag(tag.getId());
        return toDto(tag, count);
    }

    @GetMapping("/{id}/posts")
    public List<PostSummaryDto> listPostsByTag(@PathVariable Long id,
                                               @RequestParam(value = "page", required = false) Integer page,
                                               @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return postService.listPostsByTags(java.util.List.of(id), page, pageSize)
                .stream()
                .map(p -> {
                    PostSummaryDto dto = new PostSummaryDto();
                    dto.setId(p.getId());
                    dto.setTitle(p.getTitle());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private TagDto toDto(Tag tag, long count) {
        TagDto dto = new TagDto();
        dto.setId(tag.getId());
        dto.setName(tag.getName());
        dto.setIcon(tag.getIcon());
        dto.setSmallIcon(tag.getSmallIcon());
        dto.setDescription(tag.getDescription());
        dto.setCount(count);
        return dto;
    }

    @Data
    private static class TagRequest {
        private String name;
        private String description;
        private String icon;
        private String smallIcon;
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

    @Data
    private static class PostSummaryDto {
        private Long id;
        private String title;
    }
}
