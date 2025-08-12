package com.openisle.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PioneerMedalDto extends MedalDto {
    private long rank;
}
