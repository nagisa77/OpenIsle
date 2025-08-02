package com.openisle.controller;

import com.openisle.model.Post;
import com.openisle.model.Comment;
import com.openisle.model.User;
import com.openisle.service.SearchService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {
    private final SearchService searchService;

    @GetMapping("/users")
    public List<UserDto> searchUsers(@RequestParam String keyword) {
        return searchService.searchUsers(keyword).stream()
                .map(this::toUserDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/posts")
    public List<PostDto> searchPosts(@RequestParam String keyword) {
        return searchService.searchPosts(keyword).stream()
                .map(this::toPostDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/posts/content")
    public List<PostDto> searchPostsByContent(@RequestParam String keyword) {
        return searchService.searchPostsByContent(keyword).stream()
                .map(this::toPostDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/posts/title")
    public List<PostDto> searchPostsByTitle(@RequestParam String keyword) {
        return searchService.searchPostsByTitle(keyword).stream()
                .map(this::toPostDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/global")
    public List<SearchResultDto> global(@RequestParam String keyword) {
        return searchService.globalSearch(keyword).stream()
                .map(r -> {
                    SearchResultDto dto = new SearchResultDto();
                    dto.setType(r.type());
                    dto.setId(r.id());
                    dto.setText(r.text());
                    dto.setSubText(r.subText());
                    dto.setExtra(r.extra());
                    dto.setPostId(r.postId());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private UserDto toUserDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setAvatar(user.getAvatar());
        return dto;
    }

    private PostDto toPostDto(Post post) {
        PostDto dto = new PostDto();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        return dto;
    }

    @Data
    private static class UserDto {
        private Long id;
        private String username;
        private String avatar;
    }

    @Data
    private static class PostDto {
        private Long id;
        private String title;
    }

    @Data
    private static class SearchResultDto {
        private String type;
        private Long id;
        private String text;
        private String subText;
        private String extra;
        private Long postId;
    }
}
