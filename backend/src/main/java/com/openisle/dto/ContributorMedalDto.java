package com.openisle.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ContributorMedalDto extends MedalDto {
    private long currentContributionLines;
    private long targetContributionLines;
}

