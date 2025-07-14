package com.openisle.controller;

import com.openisle.model.Notification;
import com.openisle.model.NotificationType;
import com.openisle.model.ReactionType;
import com.openisle.model.Comment;
import com.openisle.model.Post;
import com.openisle.service.NotificationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/** Endpoints for user notifications. */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationDto> list(@RequestParam(value = "read", required = false) Boolean read,
                                      Authentication auth) {
        return notificationService.listNotifications(auth.getName(), read).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/unread-count")
    public UnreadCount unreadCount(Authentication auth) {
        long count = notificationService.countUnread(auth.getName());
        UnreadCount uc = new UnreadCount();
        uc.setCount(count);
        return uc;
    }

    @PostMapping("/read")
    public void markRead(@RequestBody MarkReadRequest req, Authentication auth) {
        notificationService.markRead(auth.getName(), req.getIds());
    }

    private NotificationDto toDto(Notification n) {
        NotificationDto dto = new NotificationDto();
        dto.setId(n.getId());
        dto.setType(n.getType());
        if (n.getPost() != null) {
            dto.setPost(toPostDto(n.getPost()));
        }
        if (n.getComment() != null) {
            dto.setComment(toCommentDto(n.getComment()));
            Comment parent = n.getComment().getParent();
            if (parent != null) {
                dto.setParentComment(toCommentDto(parent));
            }
        }
        if (n.getFromUser() != null) {
            dto.setFromUser(toAuthorDto(n.getFromUser()));
        }
        if (n.getReactionType() != null) {
            dto.setReactionType(n.getReactionType());
        }
        dto.setApproved(n.getApproved());
        dto.setContent(n.getContent());
        dto.setRead(n.isRead());
        dto.setCreatedAt(n.getCreatedAt());
        return dto;
    }

    private PostDto toPostDto(Post post) {
        PostDto dto = new PostDto();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
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

    private AuthorDto toAuthorDto(com.openisle.model.User user) {
        AuthorDto dto = new AuthorDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setAvatar(user.getAvatar());
        return dto;
    }

    @Data
    private static class MarkReadRequest {
        private List<Long> ids;
    }

    @Data
    private static class NotificationDto {
        private Long id;
        private NotificationType type;
        private PostDto post;
        private CommentDto comment;
        private CommentDto parentComment;
        private AuthorDto fromUser;
        private ReactionType reactionType;
        private String content;
        private Boolean approved;
        private boolean read;
        private LocalDateTime createdAt;
    }

    @Data
    private static class PostDto {
        private Long id;
        private String title;
    }

    @Data
    private static class CommentDto {
        private Long id;
        private String content;
        private LocalDateTime createdAt;
        private AuthorDto author;
    }

    @Data
    private static class AuthorDto {
        private Long id;
        private String username;
        private String avatar;
    }

    @Data
    private static class UnreadCount {
        private long count;
    }
}
