package com.openisle.controller;

import com.openisle.config.CachingConfig;
import com.openisle.dto.PollDto;
import com.openisle.dto.PostDetailDto;
import com.openisle.dto.PostRequest;
import com.openisle.dto.PostSummaryDto;
import com.openisle.mapper.PostMapper;
import com.openisle.model.Post;
import com.openisle.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

  private final PostService postService;
  private final CategoryService categoryService;
  private final TagService tagService;
  private final LevelService levelService;
  private final CaptchaService captchaService;
  private final DraftService draftService;
  private final UserVisitService userVisitService;
  private final PostMapper postMapper;
  private final PointService pointService;

  @Value("${app.captcha.enabled:false}")
  private boolean captchaEnabled;

  @Value("${app.captcha.post-enabled:false}")
  private boolean postCaptchaEnabled;

  @PostMapping
  @SecurityRequirement(name = "JWT")
  @Operation(summary = "Create post", description = "Create a new post")
  @ApiResponse(
    responseCode = "200",
    description = "Created post",
    content = @Content(schema = @Schema(implementation = PostDetailDto.class))
  )
  public ResponseEntity<PostDetailDto> createPost(
    @RequestBody PostRequest req,
    Authentication auth
  ) {
    if (captchaEnabled && postCaptchaEnabled && !captchaService.verify(req.getCaptcha())) {
      return ResponseEntity.badRequest().build();
    }
    Post post = postService.createPost(
      auth.getName(),
      req.getCategoryId(),
      req.getTitle(),
      req.getContent(),
      req.getTagIds(),
      req.getType(),
      req.getPostVisibleScopeType(),
      req.getPrizeDescription(),
      req.getPrizeIcon(),
      req.getPrizeCount(),
      req.getPointCost(),
      req.getStartTime(),
      req.getEndTime(),
      req.getOptions(),
      req.getMultiple(),
      req.getProposedName(),
      req.getProposalDescription()
    );
    draftService.deleteDraft(auth.getName());
    PostDetailDto dto = postMapper.toDetailDto(post, auth.getName());
    dto.setReward(levelService.awardForPost(auth.getName()));
    dto.setPointReward(pointService.awardForPost(auth.getName(), post.getId()));
    return ResponseEntity.ok(dto);
  }

  @PutMapping("/{id}")
  @SecurityRequirement(name = "JWT")
  @Operation(summary = "Update post", description = "Update an existing post")
  @ApiResponse(
    responseCode = "200",
    description = "Updated post",
    content = @Content(schema = @Schema(implementation = PostDetailDto.class))
  )
  public ResponseEntity<PostDetailDto> updatePost(
    @PathVariable Long id,
    @RequestBody PostRequest req,
    Authentication auth
  ) {
    Post post = postService.updatePost(
      id,
      auth.getName(),
      req.getCategoryId(),
      req.getTitle(),
      req.getContent(),
      req.getTagIds(),
      req.getPostVisibleScopeType()
    );
    return ResponseEntity.ok(postMapper.toDetailDto(post, auth.getName()));
  }

  @DeleteMapping("/{id}")
  @SecurityRequirement(name = "JWT")
  @Operation(summary = "Delete post", description = "Delete a post")
  @ApiResponse(responseCode = "200", description = "Post deleted")
  public void deletePost(@PathVariable Long id, Authentication auth) {
    postService.deletePost(id, auth.getName());
  }

  @PostMapping("/{id}/close")
  @SecurityRequirement(name = "JWT")
  @Operation(summary = "Close post", description = "Close a post to prevent further replies")
  @ApiResponse(
    responseCode = "200",
    description = "Closed post",
    content = @Content(schema = @Schema(implementation = PostSummaryDto.class))
  )
  public PostSummaryDto close(@PathVariable Long id, Authentication auth) {
    return postMapper.toSummaryDto(postService.closePost(id, auth.getName()));
  }

  @PostMapping("/{id}/reopen")
  @SecurityRequirement(name = "JWT")
  @Operation(summary = "Reopen post", description = "Reopen a closed post")
  @ApiResponse(
    responseCode = "200",
    description = "Reopened post",
    content = @Content(schema = @Schema(implementation = PostSummaryDto.class))
  )
  public PostSummaryDto reopen(@PathVariable Long id, Authentication auth) {
    return postMapper.toSummaryDto(postService.reopenPost(id, auth.getName()));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get post", description = "Get post details by id")
  @ApiResponse(
    responseCode = "200",
    description = "Post detail",
    content = @Content(schema = @Schema(implementation = PostDetailDto.class))
  )
  public ResponseEntity<PostDetailDto> getPost(@PathVariable Long id, Authentication auth) {
    String viewer = auth != null ? auth.getName() : null;
    Post post = postService.viewPost(id, viewer);
    return ResponseEntity.ok(postMapper.toDetailDto(post, viewer));
  }

  @PostMapping("/{id}/lottery/join")
  @SecurityRequirement(name = "JWT")
  @Operation(summary = "Join lottery", description = "Join a lottery for the post")
  @ApiResponse(responseCode = "200", description = "Joined lottery")
  public ResponseEntity<Void> joinLottery(@PathVariable Long id, Authentication auth) {
    postService.joinLottery(id, auth.getName());
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{id}/poll/progress")
  @Operation(summary = "Poll progress", description = "Get poll progress for a post")
  @ApiResponse(
    responseCode = "200",
    description = "Poll progress",
    content = @Content(schema = @Schema(implementation = PollDto.class))
  )
  public ResponseEntity<PollDto> pollProgress(@PathVariable Long id) {
    return ResponseEntity.ok(postMapper.toSummaryDto(postService.getPoll(id)).getPoll());
  }

  @PostMapping("/{id}/poll/vote")
  @SecurityRequirement(name = "JWT")
  @Operation(summary = "Vote poll", description = "Vote on a poll option")
  @ApiResponse(responseCode = "200", description = "Vote recorded")
  public ResponseEntity<Void> vote(
    @PathVariable Long id,
    @RequestParam("option") List<Integer> option,
    Authentication auth
  ) {
    postService.votePoll(id, auth.getName(), option);
    return ResponseEntity.ok().build();
  }

  @GetMapping
  @Operation(summary = "List posts", description = "List posts by various filters")
  @ApiResponse(
    responseCode = "200",
    description = "List of posts",
    content = @Content(
      array = @ArraySchema(schema = @Schema(implementation = PostSummaryDto.class))
    )
  )
  @Cacheable(
    value = CachingConfig.POST_CACHE_NAME,
    key = "new org.springframework.cache.interceptor.SimpleKey('default', #categoryId, #categoryIds, #tagId, #tagIds, #page, #pageSize)"
  )
  public List<PostSummaryDto> listPosts(
    @RequestParam(value = "categoryId", required = false) Long categoryId,
    @RequestParam(value = "categoryIds", required = false) List<Long> categoryIds,
    @RequestParam(value = "tagId", required = false) Long tagId,
    @RequestParam(value = "tagIds", required = false) List<Long> tagIds,
    @RequestParam(value = "page", required = false) Integer page,
    @RequestParam(value = "pageSize", required = false) Integer pageSize,
    Authentication auth
  ) {
    List<Long> ids = categoryService.getSearchCategoryIds(categoryIds, categoryId);
    List<Long> tids = tagService.getSearchTagIds(tagIds, tagId);

    return postService
      .defaultListPosts(ids, tids, page, pageSize)
      .stream()
      .map(postMapper::toSummaryDto)
      .collect(Collectors.toList());
  }

  @GetMapping("/ranking")
  @Operation(summary = "Ranking posts", description = "List posts by view rankings")
  @ApiResponse(
    responseCode = "200",
    description = "Ranked posts",
    content = @Content(
      array = @ArraySchema(schema = @Schema(implementation = PostSummaryDto.class))
    )
  )
  public List<PostSummaryDto> rankingPosts(
    @RequestParam(value = "categoryId", required = false) Long categoryId,
    @RequestParam(value = "categoryIds", required = false) List<Long> categoryIds,
    @RequestParam(value = "tagId", required = false) Long tagId,
    @RequestParam(value = "tagIds", required = false) List<Long> tagIds,
    @RequestParam(value = "page", required = false) Integer page,
    @RequestParam(value = "pageSize", required = false) Integer pageSize,
    Authentication auth
  ) {
    List<Long> ids = categoryService.getSearchCategoryIds(categoryIds, categoryId);
    List<Long> tids = tagService.getSearchTagIds(tagIds, tagId);

    return postService
      .listPostsByViews(ids, tids, page, pageSize)
      .stream()
      .map(postMapper::toSummaryDto)
      .collect(Collectors.toList());
  }

  @GetMapping("/latest-reply")
  @Operation(summary = "Latest reply posts", description = "List posts by latest replies")
  @ApiResponse(
    responseCode = "200",
    description = "Posts sorted by latest reply",
    content = @Content(
      array = @ArraySchema(schema = @Schema(implementation = PostSummaryDto.class))
    )
  )
  @Cacheable(
    value = CachingConfig.POST_CACHE_NAME,keyGenerator = "last_reply_generator"
  )
  public List<PostSummaryDto> latestReplyPosts(
    @RequestParam(value = "categoryId", required = false) Long categoryId,
    @RequestParam(value = "tagIds", required = false) List<Long> tagIds,
    @RequestParam(value = "page", required = false) Integer page,
    @RequestParam(value = "pageSize", required = false) Integer pageSize,
    Authentication auth
  ) {

    List<Post> posts = postService.listPostsByLatestReply(categoryId, tagIds, page, pageSize);
    return posts.stream().map(postMapper::toSimpleDto).collect(Collectors.toList());
  }

  @GetMapping("/featured")
  @Operation(summary = "Featured posts", description = "List featured posts")
  @ApiResponse(
    responseCode = "200",
    description = "Featured posts",
    content = @Content(
      array = @ArraySchema(schema = @Schema(implementation = PostSummaryDto.class))
    )
  )
  public List<PostSummaryDto> featuredPosts(
    @RequestParam(value = "categoryId", required = false) Long categoryId,
    @RequestParam(value = "categoryIds", required = false) List<Long> categoryIds,
    @RequestParam(value = "tagId", required = false) Long tagId,
    @RequestParam(value = "tagIds", required = false) List<Long> tagIds,
    @RequestParam(value = "page", required = false) Integer page,
    @RequestParam(value = "pageSize", required = false) Integer pageSize,
    Authentication auth
  ) {
    List<Long> ids = categoryService.getSearchCategoryIds(categoryIds, categoryId);
    List<Long> tids = tagService.getSearchTagIds(tagIds, tagId);

    return postService
      .listFeaturedPosts(ids, tids, page, pageSize)
      .stream()
      .map(postMapper::toSummaryDto)
      .collect(Collectors.toList());
  }
}
