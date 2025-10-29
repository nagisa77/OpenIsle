package com.openisle.dto;

import com.openisle.model.MedalType;
import lombok.Data;

/**
 * DTO representing a post or comment author.
 */
@Data
public class AuthorDto {

  private Long id;
  private String username;
  private String avatar;
  private MedalType displayMedal;
  private boolean bot;
}
