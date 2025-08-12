package com.openisle.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CommentMedalDto extends MedalDto {
    private long currentCommentCount;
    private long targetCommentCount;
}
