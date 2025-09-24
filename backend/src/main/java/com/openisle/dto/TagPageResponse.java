package com.openisle.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagPageResponse {

  private List<TagDto> items;
  private int page;
  private int pageSize;
  private long total;
  private boolean hasNext;
}
