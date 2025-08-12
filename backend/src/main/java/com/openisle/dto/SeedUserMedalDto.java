package com.openisle.dto;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SeedUserMedalDto extends MedalDto {
    private LocalDateTime registerDate;
}
