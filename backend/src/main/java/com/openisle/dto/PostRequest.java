package com.openisle.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

import com.openisle.model.PostType;

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
    private String prizeDescription;
    private String prizeIcon;
    private Integer prizeCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}

