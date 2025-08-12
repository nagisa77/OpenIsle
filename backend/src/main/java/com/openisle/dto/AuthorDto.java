package com.openisle.dto;

import lombok.Data;
import com.openisle.model.MedalType;

/**
 * DTO representing a post or comment author.
 */
@Data
public class AuthorDto {
    private Long id;
    private String username;
    private String avatar;
    private MedalType displayMedal;
}

