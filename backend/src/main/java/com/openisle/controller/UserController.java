package com.openisle.controller;

import com.openisle.exception.NotFoundException;
import com.openisle.model.User;
import com.openisle.service.*;
import org.springframework.beans.factory.annotation.Value;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final ImageUploader imageUploader;
    private final PostService postService;
    private final CommentService commentService;
    private final ReactionService reactionService;
    private final TagService tagService;
    private final SubscriptionService subscriptionService;
    private final PostReadService postReadService;
    private final UserVisitService userVisitService;
    private final LevelService levelService;
    private final JwtService jwtService;

    @Value("${app.upload.check-type:true}")
    private boolean checkImageType;

    @Value("${app.upload.max-size:5242880}")
    private long maxUploadSize;

    @Value("${app.user.posts-limit:10}")
    private int defaultPostsLimit;

    @Value("${app.user.replies-limit:50}")
    private int defaultRepliesLimit;

    @Value("${app.user.tags-limit:50}")
    private int defaultTagsLimit;

    @Value("${app.snippet-length:50}")
    private int snippetLength;

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(Authentication auth) {
        User user = userService.findByUsername(auth.getName()).orElseThrow();
        return ResponseEntity.ok(toDto(user, auth));
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file,
                                          Authentication auth) {
        if (checkImageType && (file.getContentType() == null || !file.getContentType().startsWith("image/"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is not an image"));
        }
        if (file.getSize() > maxUploadSize) {
            return ResponseEntity.badRequest().body(Map.of("error", "File too large"));
        }
        String url = null;
        try {
            url = imageUploader.upload(file.getBytes(), file.getOriginalFilename()).join();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("url", url));
        }
        userService.updateAvatar(auth.getName(), url);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileDto dto,
                                                 Authentication auth) {
        User user = userService.updateProfile(auth.getName(), dto.getUsername(), dto.getIntroduction());
        return ResponseEntity.ok(Map.of(
                "token", jwtService.generateToken(user.getUsername()),
                "user", toDto(user, auth)
        ));
    }

    @PostMapping("/me/signin")
    public Map<String, Integer> signIn(Authentication auth) {
        int reward = levelService.awardForSignin(auth.getName());
        return Map.of("reward", reward);
    }

    @GetMapping("/{identifier}")
    public ResponseEntity<UserDto> getUser(@PathVariable("identifier") String identifier,
                                           Authentication auth) {
        User user = userService.findByIdentifier(identifier).orElseThrow(() -> new NotFoundException("User not found"));
        return ResponseEntity.ok(toDto(user, auth));
    }

    @GetMapping("/{identifier}/posts")
    public java.util.List<PostMetaDto> userPosts(@PathVariable("identifier") String identifier,
                                                 @RequestParam(value = "limit", required = false) Integer limit) {
        int l = limit != null ? limit : defaultPostsLimit;
        User user = userService.findByIdentifier(identifier).orElseThrow();
        return postService.getRecentPostsByUser(user.getUsername(), l).stream()
                .map(this::toMetaDto)
                .collect(java.util.stream.Collectors.toList());
    }

    @GetMapping("/{identifier}/replies")
    public java.util.List<CommentInfoDto> userReplies(@PathVariable("identifier") String identifier,
                                                      @RequestParam(value = "limit", required = false) Integer limit) {
        int l = limit != null ? limit : defaultRepliesLimit;
        User user = userService.findByIdentifier(identifier).orElseThrow();
        return commentService.getRecentCommentsByUser(user.getUsername(), l).stream()
                .map(this::toCommentInfoDto)
                .collect(java.util.stream.Collectors.toList());
    }

    @GetMapping("/{identifier}/hot-posts")
    public java.util.List<PostMetaDto> hotPosts(@PathVariable("identifier") String identifier,
                                                @RequestParam(value = "limit", required = false) Integer limit) {
        int l = limit != null ? limit : 10;
        User user = userService.findByIdentifier(identifier).orElseThrow();
        java.util.List<Long> ids = reactionService.topPostIds(user.getUsername(), l);
        return postService.getPostsByIds(ids).stream()
                .map(this::toMetaDto)
                .collect(java.util.stream.Collectors.toList());
    }

    @GetMapping("/{identifier}/hot-replies")
    public java.util.List<CommentInfoDto> hotReplies(@PathVariable("identifier") String identifier,
                                                     @RequestParam(value = "limit", required = false) Integer limit) {
        int l = limit != null ? limit : 10;
        User user = userService.findByIdentifier(identifier).orElseThrow();
        java.util.List<Long> ids = reactionService.topCommentIds(user.getUsername(), l);
        return commentService.getCommentsByIds(ids).stream()
                .map(this::toCommentInfoDto)
                .collect(java.util.stream.Collectors.toList());
    }

    @GetMapping("/{identifier}/hot-tags")
    public java.util.List<TagInfoDto> hotTags(@PathVariable("identifier") String identifier,
                                              @RequestParam(value = "limit", required = false) Integer limit) {
        int l = limit != null ? limit : 10;
        User user = userService.findByIdentifier(identifier).orElseThrow();
        return tagService.getTagsByUser(user.getUsername()).stream()
                .map(t -> {
                    TagInfoDto dto = new TagInfoDto();
                    dto.setId(t.getId());
                    dto.setName(t.getName());
                    dto.setDescription(t.getDescription());
                    dto.setIcon(t.getIcon());
                    dto.setSmallIcon(t.getSmallIcon());
                    dto.setCreatedAt(t.getCreatedAt());
                    dto.setCount(postService.countPostsByTag(t.getId()));
                    return dto;
                })
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .limit(l)
                .collect(java.util.stream.Collectors.toList());
    }

    @GetMapping("/{identifier}/tags")
    public java.util.List<TagInfoDto> userTags(@PathVariable("identifier") String identifier,
                                               @RequestParam(value = "limit", required = false) Integer limit) {
        int l = limit != null ? limit : defaultTagsLimit;
        User user = userService.findByIdentifier(identifier).orElseThrow();
        return tagService.getRecentTagsByUser(user.getUsername(), l).stream()
                .map(t -> {
                    TagInfoDto dto = new TagInfoDto();
                    dto.setId(t.getId());
                    dto.setName(t.getName());
                    dto.setDescription(t.getDescription());
                    dto.setIcon(t.getIcon());
                    dto.setSmallIcon(t.getSmallIcon());
                    dto.setCreatedAt(t.getCreatedAt());
                    dto.setCount(postService.countPostsByTag(t.getId()));
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @GetMapping("/{identifier}/following")
    public java.util.List<UserDto> following(@PathVariable("identifier") String identifier) {
        User user = userService.findByIdentifier(identifier).orElseThrow();
        return subscriptionService.getSubscribedUsers(user.getUsername()).stream()
                .map(this::toDto)
                .collect(java.util.stream.Collectors.toList());
    }

    @GetMapping("/{identifier}/followers")
    public java.util.List<UserDto> followers(@PathVariable("identifier") String identifier) {
        User user = userService.findByIdentifier(identifier).orElseThrow();
        return subscriptionService.getSubscribers(user.getUsername()).stream()
                .map(this::toDto)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * List all administrator users.
     */
    @GetMapping("/admins")
    public java.util.List<UserDto> admins() {
        return userService.getAdmins().stream()
                .map(this::toDto)
                .collect(java.util.stream.Collectors.toList());
    }

    @GetMapping("/{identifier}/all")
    public ResponseEntity<UserAggregateDto> userAggregate(@PathVariable("identifier") String identifier,
                                                          @RequestParam(value = "postsLimit", required = false) Integer postsLimit,
                                                          @RequestParam(value = "repliesLimit", required = false) Integer repliesLimit,
                                                          Authentication auth) {
        User user = userService.findByIdentifier(identifier).orElseThrow();
        int pLimit = postsLimit != null ? postsLimit : defaultPostsLimit;
        int rLimit = repliesLimit != null ? repliesLimit : defaultRepliesLimit;
        java.util.List<PostMetaDto> posts = postService.getRecentPostsByUser(user.getUsername(), pLimit).stream()
                .map(this::toMetaDto)
                .collect(java.util.stream.Collectors.toList());
        java.util.List<CommentInfoDto> replies = commentService.getRecentCommentsByUser(user.getUsername(), rLimit).stream()
                .map(this::toCommentInfoDto)
                .collect(java.util.stream.Collectors.toList());
        UserAggregateDto dto = new UserAggregateDto();
        dto.setUser(toDto(user, auth));
        dto.setPosts(posts);
        dto.setReplies(replies);
        return ResponseEntity.ok(dto);
    }

    private UserDto toDto(User user, Authentication viewer) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setAvatar(user.getAvatar());
        dto.setRole(user.getRole().name());
        dto.setIntroduction(user.getIntroduction());
        dto.setFollowers(subscriptionService.countSubscribers(user.getUsername()));
        dto.setFollowing(subscriptionService.countSubscribed(user.getUsername()));
        dto.setCreatedAt(user.getCreatedAt());
        dto.setLastPostTime(postService.getLastPostTime(user.getUsername()));
        dto.setTotalViews(postService.getTotalViews(user.getUsername()));
        dto.setVisitedDays(userVisitService.countVisits(user.getUsername()));
        dto.setReadPosts(postReadService.countReads(user.getUsername()));
        dto.setLikesSent(reactionService.countLikesSent(user.getUsername()));
        dto.setLikesReceived(reactionService.countLikesReceived(user.getUsername()));
        dto.setExperience(user.getExperience());
        dto.setCurrentLevel(levelService.getLevel(user.getExperience()));
        dto.setNextLevelExp(levelService.nextLevelExp(user.getExperience()));
        if (viewer != null) {
            dto.setSubscribed(subscriptionService.isSubscribed(viewer.getName(), user.getUsername()));
        } else {
            dto.setSubscribed(false);
        }
        return dto;
    }

    private UserDto toDto(User user) {
        return toDto(user, null);
    }

    private PostMetaDto toMetaDto(com.openisle.model.Post post) {
        PostMetaDto dto = new PostMetaDto();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        String content = post.getContent();
        if (content == null) {
            content = "";
        }
        if (snippetLength >= 0) {
            dto.setSnippet(content.length() > snippetLength ? content.substring(0, snippetLength) : content);
        } else {
            dto.setSnippet(content);
        }
        dto.setCreatedAt(post.getCreatedAt());
        dto.setCategory(post.getCategory().getName());
        dto.setViews(post.getViews());
        return dto;
    }

    private CommentInfoDto toCommentInfoDto(com.openisle.model.Comment comment) {
        CommentInfoDto dto = new CommentInfoDto();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setPost(toMetaDto(comment.getPost()));
        if (comment.getParent() != null) {
            ParentCommentDto pc = new ParentCommentDto();
            pc.setId(comment.getParent().getId());
            pc.setAuthor(comment.getParent().getAuthor().getUsername());
            pc.setContent(comment.getParent().getContent());
            dto.setParentComment(pc);
        }
        return dto;
    }

    @Data
    private static class UserDto {
        private Long id;
        private String username;
        private String email;
        private String avatar;
        private String role;
        private String introduction;
        private long followers;
        private long following;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime lastPostTime;
        private long totalViews;
        private long visitedDays;
        private long readPosts;
        private long likesSent;
        private long likesReceived;
        private boolean subscribed;
        private int experience;
        private int currentLevel;
        private int nextLevelExp;
    }

    @Data
    private static class PostMetaDto {
        private Long id;
        private String title;
        private String snippet;
        private java.time.LocalDateTime createdAt;
        private String category;
        private long views;
    }

    @Data
    private static class CommentInfoDto {
        private Long id;
        private String content;
        private java.time.LocalDateTime createdAt;
        private PostMetaDto post;
        private ParentCommentDto parentComment;
    }

    @Data
    private static class TagInfoDto {
        private Long id;
        private String name;
        private String description;
        private String icon;
        private String smallIcon;
        private java.time.LocalDateTime createdAt;
        private Long count;
    }

    @Data
    private static class ParentCommentDto {
        private Long id;
        private String author;
        private String content;
    }

    @Data
    private static class UpdateProfileDto {
        private String username;
        private String introduction;
    }

    @Data
    private static class UserAggregateDto {
        private UserDto user;
        private java.util.List<PostMetaDto> posts;
        private java.util.List<CommentInfoDto> replies;
    }
}
