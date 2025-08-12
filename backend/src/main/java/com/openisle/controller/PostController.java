package com.openisle.controller;

import com.openisle.dto.PostDetailDto;
import com.openisle.dto.PostRequest;
import com.openisle.dto.PostSummaryDto;
import com.openisle.mapper.PostMapper;
import com.openisle.model.Post;
import com.openisle.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;
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
    public ResponseEntity<PostDetailDto> createPost(@RequestBody PostRequest req, Authentication auth) {
        if (captchaEnabled && postCaptchaEnabled && !captchaService.verify(req.getCaptcha())) {
            return ResponseEntity.badRequest().build();
        }
        Post post = postService.createPost(auth.getName(), req.getCategoryId(),
                req.getTitle(), req.getContent(), req.getTagIds(),
                req.getType(), req.getPrizeDescription(), req.getPrizeIcon(),
                req.getPrizeCount(), req.getStartTime(), req.getEndTime());
        draftService.deleteDraft(auth.getName());
        PostDetailDto dto = postMapper.toDetailDto(post, auth.getName());
        dto.setReward(levelService.awardForPost(auth.getName()));
        dto.setPointReward(pointService.awardForPost(auth.getName()));
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostDetailDto> updatePost(@PathVariable Long id, @RequestBody PostRequest req,
                                              Authentication auth) {
        Post post = postService.updatePost(id, auth.getName(), req.getCategoryId(),
                req.getTitle(), req.getContent(), req.getTagIds());
        return ResponseEntity.ok(postMapper.toDetailDto(post, auth.getName()));
    }

    @DeleteMapping("/{id}")
    public void deletePost(@PathVariable Long id, Authentication auth) {
        postService.deletePost(id, auth.getName());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDetailDto> getPost(@PathVariable Long id, Authentication auth) {
        String viewer = auth != null ? auth.getName() : null;
        Post post = postService.viewPost(id, viewer);
        return ResponseEntity.ok(postMapper.toDetailDto(post, viewer));
    }

    @PostMapping("/{id}/lottery/join")
    public ResponseEntity<Void> joinLottery(@PathVariable Long id, Authentication auth) {
        postService.joinLottery(id, auth.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public List<PostSummaryDto> listPosts(@RequestParam(value = "categoryId", required = false) Long categoryId,
                                   @RequestParam(value = "categoryIds", required = false) List<Long> categoryIds,
                                   @RequestParam(value = "tagId", required = false) Long tagId,
                                   @RequestParam(value = "tagIds", required = false) List<Long> tagIds,
                                   @RequestParam(value = "page", required = false) Integer page,
                                   @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                   Authentication auth) {
        List<Long> ids = categoryIds;
        if (categoryId != null) {
            ids = java.util.List.of(categoryId);
        }
        List<Long> tids = tagIds;
        if (tagId != null) {
            tids = java.util.List.of(tagId);
        }

        if (auth != null) {
            userVisitService.recordVisit(auth.getName());
        }

        boolean hasCategories = ids != null && !ids.isEmpty();
        boolean hasTags = tids != null && !tids.isEmpty();

        if (hasCategories && hasTags) {
            return postService.listPostsByCategoriesAndTags(ids, tids, page, pageSize)
                    .stream().map(postMapper::toSummaryDto).collect(Collectors.toList());
        }
        if (hasTags) {
            return postService.listPostsByTags(tids, page, pageSize)
                .stream().map(postMapper::toSummaryDto).collect(Collectors.toList());
        }

        return postService.listPostsByCategories(ids, page, pageSize)
                .stream().map(postMapper::toSummaryDto).collect(Collectors.toList());
    }

    @GetMapping("/ranking")
    public List<PostSummaryDto> rankingPosts(@RequestParam(value = "categoryId", required = false) Long categoryId,
                                      @RequestParam(value = "categoryIds", required = false) List<Long> categoryIds,
                                      @RequestParam(value = "tagId", required = false) Long tagId,
                                      @RequestParam(value = "tagIds", required = false) List<Long> tagIds,
                                      @RequestParam(value = "page", required = false) Integer page,
                                      @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                      Authentication auth) {
        List<Long> ids = categoryIds;
        if (categoryId != null) {
            ids = java.util.List.of(categoryId);
        }
        List<Long> tids = tagIds;
        if (tagId != null) {
            tids = java.util.List.of(tagId);
        }

        if (auth != null) {
            userVisitService.recordVisit(auth.getName());
        }

        return postService.listPostsByViews(ids, tids, page, pageSize)
                .stream().map(postMapper::toSummaryDto).collect(Collectors.toList());
    }

    @GetMapping("/latest-reply")
    public List<PostSummaryDto> latestReplyPosts(@RequestParam(value = "categoryId", required = false) Long categoryId,
                                          @RequestParam(value = "categoryIds", required = false) List<Long> categoryIds,
                                          @RequestParam(value = "tagId", required = false) Long tagId,
                                          @RequestParam(value = "tagIds", required = false) List<Long> tagIds,
                                          @RequestParam(value = "page", required = false) Integer page,
                                          @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                          Authentication auth) {
        List<Long> ids = categoryIds;
        if (categoryId != null) {
            ids = java.util.List.of(categoryId);
        }
        List<Long> tids = tagIds;
        if (tagId != null) {
            tids = java.util.List.of(tagId);
        }

        if (auth != null) {
            userVisitService.recordVisit(auth.getName());
        }

        return postService.listPostsByLatestReply(ids, tids, page, pageSize)
                .stream().map(postMapper::toSummaryDto).collect(Collectors.toList());
    }
}
