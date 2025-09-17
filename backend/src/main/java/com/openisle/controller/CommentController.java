package com.openisle.controller;

import com.openisle.model.Comment;
import com.openisle.dto.CommentDto;
import com.openisle.dto.CommentRequest;
import com.openisle.dto.CommentPaginationDto;
import com.openisle.mapper.CommentMapper;
import com.openisle.service.CaptchaService;
import com.openisle.service.CommentService;
import com.openisle.service.LevelService;
import com.openisle.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class CommentController {
    private final CommentService commentService;
    private final LevelService levelService;
    private final CaptchaService captchaService;
    private final CommentMapper commentMapper;
    private final PointService pointService;

    @Value("${app.captcha.enabled:false}")
    private boolean captchaEnabled;

    @Value("${app.captcha.comment-enabled:false}")
    private boolean commentCaptchaEnabled;

    @PostMapping("/posts/{postId}/comments")
    @Operation(summary = "Create comment", description = "Add a comment to a post")
    @ApiResponse(responseCode = "200", description = "Created comment",
            content = @Content(schema = @Schema(implementation = CommentDto.class)))
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<CommentDto> createComment(@PathVariable Long postId,
                                                    @RequestBody CommentRequest req,
                                                    Authentication auth) {
        log.debug("createComment called by user {} for post {}", auth.getName(), postId);
        if (captchaEnabled && commentCaptchaEnabled && !captchaService.verify(req.getCaptcha())) {
            log.debug("Captcha verification failed for user {} on post {}", auth.getName(), postId);
            return ResponseEntity.badRequest().build();
        }
        Comment comment = commentService.addComment(auth.getName(), postId, req.getContent());
        CommentDto dto = commentMapper.toDto(comment);
        dto.setReward(levelService.awardForComment(auth.getName()));
        dto.setPointReward(pointService.awardForComment(auth.getName(), postId, comment.getId()));
        log.debug("createComment succeeded for comment {}", comment.getId());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/comments/{commentId}/replies")
    @Operation(summary = "Reply to comment", description = "Reply to an existing comment")
    @ApiResponse(responseCode = "200", description = "Reply created",
            content = @Content(schema = @Schema(implementation = CommentDto.class)))
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<CommentDto> replyComment(@PathVariable Long commentId,
                                                   @RequestBody CommentRequest req,
                                                   Authentication auth) {
        log.debug("replyComment called by user {} for comment {}", auth.getName(), commentId);
        if (captchaEnabled && commentCaptchaEnabled && !captchaService.verify(req.getCaptcha())) {
            log.debug("Captcha verification failed for user {} on comment {}", auth.getName(), commentId);
            return ResponseEntity.badRequest().build();
        }
        Comment comment = commentService.addReply(auth.getName(), commentId, req.getContent());
        CommentDto dto = commentMapper.toDto(comment);
        dto.setReward(levelService.awardForComment(auth.getName()));
        log.debug("replyComment succeeded for comment {}", comment.getId());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/posts/{postId}/comments")
    @Operation(summary = "List comments", description = "List comments for a post")
    @ApiResponse(responseCode = "200", description = "Comments",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = CommentDto.class))))
    public List<CommentDto> listComments(@PathVariable Long postId,
                                         @RequestParam(value = "sort", required = false, defaultValue = "OLDEST") com.openisle.model.CommentSort sort) {
        log.debug("listComments called for post {} with sort {}", postId, sort);
        List<CommentDto> list = commentService.getCommentsForPost(postId, sort).stream()
                .map(commentMapper::toDtoWithReplies)
                .collect(Collectors.toList());
        log.debug("listComments returning {} comments", list.size());
        return list;
    }

    @GetMapping("/posts/{postId}/comments/paginated")
    @Operation(summary = "List comments with pagination", description = "List comments for a post with pagination support")
    @ApiResponse(responseCode = "200", description = "Paginated comments",
            content = @Content(schema = @Schema(implementation = CommentPaginationDto.class)))
    public CommentPaginationDto listCommentsWithPagination(
            @PathVariable Long postId,
            @RequestParam(value = "sort", required = false, defaultValue = "OLDEST") com.openisle.model.CommentSort sort,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        
        log.debug("listCommentsWithPagination called for post {} with sort {}, page {}, pageSize {}", 
                  postId, sort, page, pageSize);
        
        // 参数验证
        if (page < 0) {
            page = 0;
        }
        if (pageSize <= 0 || pageSize > 100) {
            // 限制最大页面大小为100，默认为10
            pageSize = 10;
        }
        
        CommentPaginationDto paginationResult = commentService.getCommentsForPostPaginated(postId, sort, page, pageSize);
        
        // 转换为DTO并包含回复
        List<CommentDto> commentDtos = paginationResult.getCommentEntities().stream()
                .map(commentMapper::toDtoWithReplies)
                .collect(Collectors.toList());
        
        paginationResult.setComments(commentDtos);
        // 清除临时存储的实体，避免内存泄漏
        paginationResult.setCommentEntities(null);
        
        log.debug("listCommentsWithPagination returning {} comments, hasNext: {}", 
                  commentDtos.size(), paginationResult.isHasNext());
        return paginationResult;
    }

    @DeleteMapping("/comments/{id}")
    @Operation(summary = "Delete comment", description = "Delete a comment")
    @ApiResponse(responseCode = "200", description = "Deleted")
    @SecurityRequirement(name = "JWT")
    public void deleteComment(@PathVariable Long id, Authentication auth) {
        log.debug("deleteComment called by user {} for comment {}", auth.getName(), id);
        commentService.deleteComment(auth.getName(), id);
        log.debug("deleteComment completed for comment {}", id);
    }

    @PostMapping("/comments/{id}/pin")
    @Operation(summary = "Pin comment", description = "Pin a comment")
    @ApiResponse(responseCode = "200", description = "Pinned comment",
            content = @Content(schema = @Schema(implementation = CommentDto.class)))
    @SecurityRequirement(name = "JWT")
    public CommentDto pinComment(@PathVariable Long id, Authentication auth) {
        log.debug("pinComment called by user {} for comment {}", auth.getName(), id);
        return commentMapper.toDto(commentService.pinComment(auth.getName(), id));
    }

    @PostMapping("/comments/{id}/unpin")
    @Operation(summary = "Unpin comment", description = "Unpin a comment")
    @ApiResponse(responseCode = "200", description = "Unpinned comment",
            content = @Content(schema = @Schema(implementation = CommentDto.class)))
    @SecurityRequirement(name = "JWT")
    public CommentDto unpinComment(@PathVariable Long id, Authentication auth) {
        log.debug("unpinComment called by user {} for comment {}", auth.getName(), id);
        return commentMapper.toDto(commentService.unpinComment(auth.getName(), id));
    }
}
