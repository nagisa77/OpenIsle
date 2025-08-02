package com.openisle.controller;

import com.openisle.model.Comment;
import com.openisle.service.CommentService;
import com.openisle.service.CaptchaService;
import com.openisle.service.LevelService;
import com.openisle.service.ReactionService;
import com.openisle.model.CommentSort;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;
    private final LevelService levelService;
    private final CaptchaService captchaService;
    private final ReactionService reactionService;

    @Value("${app.captcha.enabled:false}")
    private boolean captchaEnabled;

    @Value("${app.captcha.comment-enabled:false}")
    private boolean commentCaptchaEnabled;

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentDto> createComment(@PathVariable Long postId,
                                                    @RequestBody CommentRequest req,
                                                    Authentication auth) {
        if (captchaEnabled && commentCaptchaEnabled && !captchaService.verify(req.getCaptcha())) {
            return ResponseEntity.badRequest().build();
        }
        Comment comment = commentService.addComment(auth.getName(), postId, req.getContent());
        CommentDto dto = toDto(comment);
        dto.setReward(levelService.awardForComment(auth.getName()));
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/comments/{commentId}/replies")
    public ResponseEntity<CommentDto> replyComment(@PathVariable Long commentId,
                                                   @RequestBody CommentRequest req,
                                                   Authentication auth) {
        if (captchaEnabled && commentCaptchaEnabled && !captchaService.verify(req.getCaptcha())) {
            return ResponseEntity.badRequest().build();
        }
        Comment comment = commentService.addReply(auth.getName(), commentId, req.getContent());
        CommentDto dto = toDto(comment);
        dto.setReward(levelService.awardForComment(auth.getName()));
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/posts/{postId}/comments")
    public List<CommentDto> listComments(@PathVariable Long postId,
                                         @RequestParam(value = "sort", required = false, defaultValue = "OLDEST") CommentSort sort) {
        return commentService.getCommentsForPost(postId, sort).stream()
                .map(this::toDtoWithReplies)
                .collect(Collectors.toList());
    }

    private CommentDto toDtoWithReplies(Comment comment) {
        CommentDto dto = toDto(comment);
        List<CommentDto> replies = commentService.getReplies(comment.getId()).stream()
                .map(this::toDtoWithReplies)
                .collect(Collectors.toList());
        dto.setReplies(replies);
        List<ReactionDto> reactions = reactionService.getReactionsForComment(comment.getId()).stream()
                .map(this::toReactionDto)
                .collect(Collectors.toList());
        dto.setReactions(reactions);
        return dto;
    }

    @DeleteMapping("/comments/{id}")
    public void deleteComment(@PathVariable Long id, Authentication auth) {
        commentService.deleteComment(auth.getName(), id);
    }

    private CommentDto toDto(Comment comment) {
        CommentDto dto = new CommentDto();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setAuthor(toAuthorDto(comment.getAuthor()));
        dto.setReward(0);
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
    private static class CommentRequest {
        private String content;
        private String captcha;
    }

    @Data
    private static class CommentDto {
        private Long id;
        private String content;
        private LocalDateTime createdAt;
        private AuthorDto author;
        private List<CommentDto> replies;
        private List<ReactionDto> reactions;
        private int reward;
    }

    @Data
    private static class AuthorDto {
        private Long id;
        private String username;
        private String avatar;
    }

    private ReactionDto toReactionDto(com.openisle.model.Reaction reaction) {
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
        dto.setReward(0);
        return dto;
    }

    @Data
    private static class ReactionDto {
        private Long id;
        private com.openisle.model.ReactionType type;
        private String user;
        private Long postId;
        private Long commentId;
        private int reward;
    }
}
