package com.openisle.controller;

import com.openisle.dto.PostSummaryDto;
import com.openisle.dto.TagDto;
import com.openisle.dto.TagPageResponse;
import com.openisle.dto.TagRequest;
import com.openisle.mapper.PostMapper;
import com.openisle.mapper.TagMapper;
import com.openisle.model.PublishMode;
import com.openisle.model.Role;
import com.openisle.model.Tag;
import com.openisle.repository.UserRepository;
import com.openisle.service.PostService;
import com.openisle.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

  private final TagService tagService;
  private final PostService postService;
  private final UserRepository userRepository;
  private final PostMapper postMapper;
  private final TagMapper tagMapper;

  @PostMapping
  @Operation(summary = "Create tag", description = "Create a new tag")
  @ApiResponse(
    responseCode = "200",
    description = "Created tag",
    content = @Content(schema = @Schema(implementation = TagDto.class))
  )
  @SecurityRequirement(name = "JWT")
  public TagDto create(
    @RequestBody TagRequest req,
    org.springframework.security.core.Authentication auth
  ) {
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
      auth != null ? auth.getName() : null
    );
    long count = postService.countPostsByTag(tag.getId());
    return tagMapper.toDto(tag, count);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update tag", description = "Update an existing tag")
  @ApiResponse(
    responseCode = "200",
    description = "Updated tag",
    content = @Content(schema = @Schema(implementation = TagDto.class))
  )
  public TagDto update(@PathVariable Long id, @RequestBody TagRequest req) {
    Tag tag = tagService.updateTag(
      id,
      req.getName(),
      req.getDescription(),
      req.getIcon(),
      req.getSmallIcon()
    );
    long count = postService.countPostsByTag(tag.getId());
    return tagMapper.toDto(tag, count);
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete tag", description = "Delete a tag by id")
  @ApiResponse(responseCode = "200", description = "Tag deleted")
  public void delete(@PathVariable Long id) {
    tagService.deleteTag(id);
  }

  @GetMapping
  @Operation(summary = "List tags", description = "List tags with optional keyword")
  @ApiResponse(
    responseCode = "200",
    description = "List of tags",
    content = @Content(schema = @Schema(implementation = TagPageResponse.class))
  )
  public TagPageResponse list(
    @RequestParam(value = "keyword", required = false) String keyword,
    @RequestParam(value = "limit", required = false) Integer limit,
    @RequestParam(value = "page", required = false) Integer page,
    @RequestParam(value = "pageSize", required = false) Integer pageSize
  ) {
    int resolvedPageSize = Optional.ofNullable(pageSize)
      .filter(s -> s > 0)
      .or(() -> Optional.ofNullable(limit).filter(l -> l > 0))
      .orElse(20);
    int resolvedPage = Optional.ofNullable(page)
      .filter(p -> p >= 0)
      .orElse(0);

    Pageable pageable = PageRequest.of(
      resolvedPage,
      resolvedPageSize,
      Sort.by(Sort.Direction.DESC, "createdAt")
    );
    Page<Tag> tagPage = tagService.searchTags(keyword, pageable);
    List<Tag> tags = tagPage.getContent();
    List<Long> tagIds = tags.stream().map(Tag::getId).toList();
    Map<Long, Long> postCntByTagIds = Optional.ofNullable(
      postService.countPostsByTagIds(tagIds)
    ).orElseGet(Map::of);
    List<TagDto> dtos = tags
      .stream()
      .map(t -> tagMapper.toDto(t, postCntByTagIds.getOrDefault(t.getId(), 0L)))
      .collect(Collectors.toList());

    return new TagPageResponse(
      dtos,
      tagPage.getNumber(),
      tagPage.getSize(),
      tagPage.getTotalElements(),
      tagPage.hasNext()
    );
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get tag", description = "Get tag by id")
  @ApiResponse(
    responseCode = "200",
    description = "Tag detail",
    content = @Content(schema = @Schema(implementation = TagDto.class))
  )
  public TagDto get(@PathVariable Long id) {
    Tag tag = tagService.getTag(id);
    long count = postService.countPostsByTag(tag.getId());
    return tagMapper.toDto(tag, count);
  }

  @GetMapping("/{id}/posts")
  @Operation(summary = "List posts by tag", description = "Get posts with specific tag")
  @ApiResponse(
    responseCode = "200",
    description = "List of posts",
    content = @Content(
      array = @ArraySchema(schema = @Schema(implementation = PostSummaryDto.class))
    )
  )
  public List<PostSummaryDto> listPostsByTag(
    @PathVariable Long id,
    @RequestParam(value = "page", required = false) Integer page,
    @RequestParam(value = "pageSize", required = false) Integer pageSize
  ) {
    return postService
      .listPostsByTags(java.util.List.of(id), page, pageSize)
      .stream()
      .map(postMapper::toSummaryDto)
      .collect(Collectors.toList());
  }
}
