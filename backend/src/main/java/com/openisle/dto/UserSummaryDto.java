package com.openisle.dto;

import lombok.Data;

@Data
public class UserSummaryDto {

  private Long id;
  private String username;
  private String avatar;
  private boolean bot;
}
