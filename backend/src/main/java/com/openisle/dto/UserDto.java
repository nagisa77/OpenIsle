package com.openisle.dto;

import java.time.LocalDateTime;
import lombok.Data;

/** Detailed user information. */
@Data
public class UserDto {

  private Long id;
  private String username;
  private String email;
  private String avatar;
  private String role;
  private String introduction;
  private long followers;
  private long following;
  private LocalDateTime createdAt;
  private LocalDateTime lastPostTime;
  private LocalDateTime lastCommentTime;
  private long totalViews;
  private long visitedDays;
  private long readPosts;
  private long likesSent;
  private long likesReceived;
  private boolean subscribed;
  private int experience;
  private int point;
  private int currentLevel;
  private int nextLevelExp;
  private boolean bot;
}
