package com.openisle.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PostMedalDto extends MedalDto {
    private long currentPostCount;
    private long targetPostCount;
}
