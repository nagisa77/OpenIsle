package com.openisle.dto;

import com.openisle.model.PostStatus;
import com.openisle.model.PostType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lightweight DTO for listing posts without comments.
 */
@Data
public class PostSummaryDto {
    private Long id;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private AuthorDto author;
    private CategoryDto category;
    private List<TagDto> tags;
    private long views;
    private long commentCount;
    private PostStatus status;
    private LocalDateTime pinnedAt;
    private LocalDateTime lastReplyAt;
    private List<ReactionDto> reactions;
    private List<AuthorDto> participants;
    private boolean subscribed;
    private int reward;
    private int pointReward;
    private PostType type;
    private LotteryDto lottery;
}

