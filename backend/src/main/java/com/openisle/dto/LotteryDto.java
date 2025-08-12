package com.openisle.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/** Metadata for lottery posts. */
@Data
public class LotteryDto {
    private String prizeDescription;
    private String prizeIcon;
    private int prizeCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<AuthorDto> participants;
    private List<AuthorDto> winners;
}
