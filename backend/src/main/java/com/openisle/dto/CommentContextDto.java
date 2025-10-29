package com.openisle.dto;

import java.util.List;
import lombok.Data;

/**
 * DTO representing the context of a comment including its post and previous comments.
 */
@Data
public class CommentContextDto {

  private PostSummaryDto post;
  private CommentDto targetComment;
  private List<CommentDto> previousComments;
}
