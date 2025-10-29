package com.openisle.controller;

import com.openisle.dto.CommentContextDto;
import com.openisle.dto.CommentDto;
import com.openisle.dto.CommentRequest;
import com.openisle.dto.PostChangeLogDto;
import com.openisle.dto.TimelineItemDto;
import com.openisle.mapper.CommentMapper;
import com.openisle.mapper.PostChangeLogMapper;
import com.openisle.mapper.PostMapper;
import com.openisle.model.Comment;
import com.openisle.model.CommentSort;
import com.openisle.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
  private final PostChangeLogService changeLogService;
  private final PostChangeLogMapper postChangeLogMapper;
  private final PostMapper postMapper;

  @Value("${app.captcha.enabled:false}")
  private boolean captchaEnabled;

  @Value("${app.captcha.comment-enabled:false}")
  private boolean commentCaptchaEnabled;

  @PostMapping("/posts/{postId}/comments")
  @Operation(summary = "Create comment", description = "Add a comment to a post")
  @ApiResponse(
    responseCode = "200",
    description = "Created comment",
    content = @Content(schema = @Schema(implementation = CommentDto.class))
  )
  @SecurityRequirement(name = "JWT")
  public ResponseEntity<CommentDto> createComment(
    @PathVariable Long postId,
    @RequestBody CommentRequest req,
    Authentication auth
  ) {
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
  @ApiResponse(
    responseCode = "200",
    description = "Reply created",
    content = @Content(schema = @Schema(implementation = CommentDto.class))
  )
  @SecurityRequirement(name = "JWT")
  public ResponseEntity<CommentDto> replyComment(
    @PathVariable Long commentId,
    @RequestBody CommentRequest req,
    Authentication auth
  ) {
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
  @ApiResponse(
    responseCode = "200",
    description = "Comments",
    content = @Content(
      array = @ArraySchema(schema = @Schema(implementation = TimelineItemDto.class))
    )
  )
  public List<TimelineItemDto<?>> listComments(
    @PathVariable Long postId,
    @RequestParam(value = "sort", required = false, defaultValue = "OLDEST") CommentSort sort
  ) {
    log.debug("listComments called for post {} with sort {}", postId, sort);
    List<CommentDto> commentDtoList = commentService
      .getCommentsForPost(postId, sort)
      .stream()
      .map(commentMapper::toDtoWithReplies)
      .collect(Collectors.toList());
    List<PostChangeLogDto> postChangeLogDtoList = changeLogService
      .listLogs(postId)
      .stream()
      .map(postChangeLogMapper::toDto)
      .collect(Collectors.toList());
    List<TimelineItemDto<?>> itemDtoList = new ArrayList<>();

    itemDtoList.addAll(
      commentDtoList
        .stream()
        .map(c ->
          new TimelineItemDto<>(
            c.getId(),
            "comment",
            c.getCreatedAt(),
            c.getPinnedAt(),
            c // payload 是 CommentDto
          )
        )
        .toList()
    );

    itemDtoList.addAll(
      postChangeLogDtoList
        .stream()
        .map(l ->
          new TimelineItemDto<>(
            l.getId(),
            "log",
            l.getTime(), // 注意字段名不一样
            null,
            l // payload 是 PostChangeLogDto
          )
        )
        .toList()
    );
    // 排序
    Comparator<TimelineItemDto<?>> pinnedOrderComparator = (a, b) -> {
      LocalDateTime aPinned = a.getPinnedAt();
      LocalDateTime bPinned = b.getPinnedAt();
      if (aPinned == null && bPinned == null) {
        return 0;
      }
      if (aPinned == null) {
        return 1;
      }
      if (bPinned == null) {
        return -1;
      }
      return bPinned.compareTo(aPinned);
    };

    Comparator<TimelineItemDto<?>> comparator = Comparator.<TimelineItemDto<?>, Boolean>comparing(
      item -> item.getPinnedAt() == null
    ).thenComparing(pinnedOrderComparator);

    Comparator<TimelineItemDto<?>> createdAtComparator = Comparator.comparing(
      TimelineItemDto::getCreatedAt
    );
    if (CommentSort.NEWEST.equals(sort)) {
      createdAtComparator = createdAtComparator.reversed();
    }
    itemDtoList.sort(comparator.thenComparing(createdAtComparator));
    log.debug("listComments returning {} comments", itemDtoList.size());
    return itemDtoList;
  }

  @GetMapping("/comments/{commentId}/context")
  @Operation(
    summary = "Comment context",
    description = "Get a comment along with its previous comments and related post"
  )
  @ApiResponse(
    responseCode = "200",
    description = "Comment context",
    content = @Content(schema = @Schema(implementation = CommentContextDto.class))
  )
  public ResponseEntity<CommentContextDto> getCommentContext(@PathVariable Long commentId) {
    log.debug("getCommentContext called for comment {}", commentId);
    Comment comment = commentService.getComment(commentId);
    CommentContextDto dto = new CommentContextDto();
    dto.setPost(postMapper.toSummaryDto(comment.getPost()));
    dto.setTargetComment(commentMapper.toDtoWithReplies(comment));
    dto.setPreviousComments(
      commentService
        .getCommentsBefore(comment)
        .stream()
        .map(commentMapper::toDtoWithReplies)
        .collect(Collectors.toList())
    );
    log.debug(
      "getCommentContext returning {} previous comments for comment {}",
      dto.getPreviousComments().size(),
      commentId
    );
    return ResponseEntity.ok(dto);
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
  @ApiResponse(
    responseCode = "200",
    description = "Pinned comment",
    content = @Content(schema = @Schema(implementation = CommentDto.class))
  )
  @SecurityRequirement(name = "JWT")
  public CommentDto pinComment(@PathVariable Long id, Authentication auth) {
    log.debug("pinComment called by user {} for comment {}", auth.getName(), id);
    return commentMapper.toDto(commentService.pinComment(auth.getName(), id));
  }

  @PostMapping("/comments/{id}/unpin")
  @Operation(summary = "Unpin comment", description = "Unpin a comment")
  @ApiResponse(
    responseCode = "200",
    description = "Unpinned comment",
    content = @Content(schema = @Schema(implementation = CommentDto.class))
  )
  @SecurityRequirement(name = "JWT")
  public CommentDto unpinComment(@PathVariable Long id, Authentication auth) {
    log.debug("unpinComment called by user {} for comment {}", auth.getName(), id);
    return commentMapper.toDto(commentService.unpinComment(auth.getName(), id));
  }
}
