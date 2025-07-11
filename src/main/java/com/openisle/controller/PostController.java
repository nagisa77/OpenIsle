package com.openisle.controller;

import com.openisle.model.Comment;
import com.openisle.model.Post;
import com.openisle.model.Reaction;
import com.openisle.service.CommentService;
import com.openisle.service.PostService;
import com.openisle.service.ReactionService;
import com.openisle.service.CaptchaService;
import com.openisle.service.DraftService;
import com.openisle.service.SubscriptionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;
    private final CommentService commentService;
    private final ReactionService reactionService;
    private final SubscriptionService subscriptionService;
    private final CaptchaService captchaService;
    private final DraftService draftService;

    @Value("${app.captcha.enabled:false}")
    private boolean captchaEnabled;

    @Value("${app.captcha.post-enabled:false}")
    private boolean postCaptchaEnabled;

    @PostMapping
    public ResponseEntity<PostDto> createPost(@RequestBody PostRequest req, Authentication auth) {
        if (captchaEnabled && postCaptchaEnabled && !captchaService.verify(req.getCaptcha())) {
            return ResponseEntity.badRequest().build();
        }
        Post post = postService.createPost(auth.getName(), req.getCategoryId(),
                req.getTitle(), req.getContent(), req.getTagIds());
        draftService.deleteDraft(auth.getName());
        return ResponseEntity.ok(toDto(post));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDto> getPost(@PathVariable Long id, Authentication auth) {
        String viewer = auth != null ? auth.getName() : null;
        Post post = postService.viewPost(id, viewer);
        return ResponseEntity.ok(toDto(post, viewer));
    }

    @GetMapping
    public List<PostDto> listPosts(@RequestParam(value = "categoryId", required = false) Long categoryId,
                                   @RequestParam(value = "categoryIds", required = false) List<Long> categoryIds,
                                   @RequestParam(value = "tagId", required = false) Long tagId,
                                   @RequestParam(value = "tagIds", required = false) List<Long> tagIds,
                                   @RequestParam(value = "page", required = false) Integer page,
                                   @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        List<Long> ids = categoryIds;
        if (categoryId != null) {
            ids = java.util.List.of(categoryId);
        }
        List<Long> tids = tagIds;
        if (tagId != null) {
            tids = java.util.List.of(tagId);
        }

        boolean hasCategories = ids != null && !ids.isEmpty();
        boolean hasTags = tids != null && !tids.isEmpty();

        if (hasCategories && hasTags) {
            return postService.listPostsByCategoriesAndTags(ids, tids, page, pageSize)
                    .stream().map(this::toDto).collect(Collectors.toList());
        }
        if (hasTags) {
            return postService.listPostsByTags(tids, page, pageSize)
                .stream().map(this::toDto).collect(Collectors.toList());
        }

        return postService.listPostsByCategories(ids, page, pageSize)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @GetMapping("/ranking")
    public List<PostDto> rankingPosts(@RequestParam(value = "categoryId", required = false) Long categoryId,
                                      @RequestParam(value = "categoryIds", required = false) List<Long> categoryIds,
                                      @RequestParam(value = "tagId", required = false) Long tagId,
                                      @RequestParam(value = "tagIds", required = false) List<Long> tagIds,
                                      @RequestParam(value = "page", required = false) Integer page,
                                      @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        List<Long> ids = categoryIds;
        if (categoryId != null) {
            ids = java.util.List.of(categoryId);
        }
        List<Long> tids = tagIds;
        if (tagId != null) {
            tids = java.util.List.of(tagId);
        }

        return postService.listPostsByViews(ids, tids, page, pageSize)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    private PostDto toDto(Post post) {
        PostDto dto = new PostDto();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setAuthor(toAuthorDto(post.getAuthor()));
        dto.setCategory(toCategoryDto(post.getCategory()));
        dto.setTags(post.getTags().stream().map(this::toTagDto).collect(Collectors.toList()));
        dto.setStatus(post.getStatus());
        dto.setViews(post.getViews());

        List<ReactionDto> reactions = reactionService.getReactionsForPost(post.getId())
                .stream()
                .map(this::toReactionDto)
                .collect(Collectors.toList());
        dto.setReactions(reactions);

        List<CommentDto> comments = commentService.getCommentsForPost(post.getId())
                .stream()
                .map(this::toCommentDtoWithReplies)
                .collect(Collectors.toList());
        dto.setComments(comments);

        java.util.List<com.openisle.model.User> participants = commentService.getParticipants(post.getId(), 5);
        dto.setParticipants(participants.stream().map(this::toAuthorDto).collect(Collectors.toList()));

        return dto;
    }

    private PostDto toDto(Post post, String viewer) {
        PostDto dto = toDto(post);
        if (viewer != null) {
            dto.setSubscribed(subscriptionService.isPostSubscribed(viewer, post.getId()));
        } else {
            dto.setSubscribed(false);
        }
        return dto;
    }

    private CommentDto toCommentDtoWithReplies(Comment comment) {
        CommentDto dto = toCommentDto(comment);
        List<CommentDto> replies = commentService.getReplies(comment.getId()).stream()
                .map(this::toCommentDtoWithReplies)
                .collect(Collectors.toList());
        dto.setReplies(replies);

        List<ReactionDto> reactions = reactionService.getReactionsForComment(comment.getId())
                .stream()
                .map(this::toReactionDto)
                .collect(Collectors.toList());
        dto.setReactions(reactions);

        return dto;
    }

    private CommentDto toCommentDto(Comment comment) {
        CommentDto dto = new CommentDto();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setAuthor(toAuthorDto(comment.getAuthor()));
        return dto;
    }

    private ReactionDto toReactionDto(Reaction reaction) {
        ReactionDto dto = new ReactionDto();
        dto.setId(reaction.getId());
        dto.setType(reaction.getType());
        dto.setUser(reaction.getUser().getUsername());
        if (reaction.getPost() != null) {
            dto.setPostId(reaction.getPost().getId());
        }
        if (reaction.getComment() != null) {
            dto.setCommentId(reaction.getComment().getId());
        }
        return dto;
    }

    private CategoryDto toCategoryDto(com.openisle.model.Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setIcon(category.getIcon());
        dto.setSmallIcon(category.getSmallIcon());
        return dto;
    }

    private TagDto toTagDto(com.openisle.model.Tag tag) {
        TagDto dto = new TagDto();
        dto.setId(tag.getId());
        dto.setName(tag.getName());
        dto.setDescription(tag.getDescription());
        dto.setIcon(tag.getIcon());
        dto.setSmallIcon(tag.getSmallIcon());
        return dto;
    }

    private AuthorDto toAuthorDto(com.openisle.model.User user) {
        AuthorDto dto = new AuthorDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setAvatar(user.getAvatar());
        return dto;
    }

    @Data
    private static class PostRequest {
        private Long categoryId;
        private String title;
        private String content;
        private java.util.List<Long> tagIds;
        private String captcha;
    }

    @Data
    private static class PostDto {
        private Long id;
        private String title;
        private String content;
        private LocalDateTime createdAt;
        private AuthorDto author;
        private CategoryDto category;
        private java.util.List<TagDto> tags;
        private com.openisle.model.PostStatus status;
        private long views;
        private List<CommentDto> comments;
        private List<ReactionDto> reactions;
        private java.util.List<AuthorDto> participants;
        private boolean subscribed;
    }

    @Data
    private static class CategoryDto {
        private Long id;
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
    }

    @Data
    private static class AuthorDto {
        private Long id;
        private String username;
        private String avatar;
    }

    @Data
    private static class CommentDto {
        private Long id;
        private String content;
        private LocalDateTime createdAt;
        private AuthorDto author;
        private List<CommentDto> replies;
        private List<ReactionDto> reactions;
    }

    @Data
    private static class ReactionDto {
        private Long id;
        private com.openisle.model.ReactionType type;
        private String user;
        private Long postId;
        private Long commentId;
    }
}
