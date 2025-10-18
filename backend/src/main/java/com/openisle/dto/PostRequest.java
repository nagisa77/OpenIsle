package com.openisle.dto;

import com.openisle.model.PostType;
import java.time.LocalDateTime;
import java.util.List;

import com.openisle.model.PostVisibleScopeType;
import lombok.Data;

/**
 * Request body for creating or updating a post.
 */
@Data
public class PostRequest {

  private Long categoryId;
  private String title;
  private String content;
  private List<Long> tagIds;
  private String captcha;

  // optional for lottery posts
  private PostType type;
  private PostVisibleScopeType postVisibleScopeType;
  private String prizeDescription;
  private String prizeIcon;
  private Integer prizeCount;
  private Integer pointCost;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  // fields for poll posts
  private List<String> options;
  private Boolean multiple;
}
