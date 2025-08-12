package com.openisle.dto;

import com.openisle.model.MedalType;
import lombok.Data;

@Data
public class MedalDto {
    private String icon;
    private String title;
    private String description;
    private MedalType type;
    private boolean completed;
    private boolean selected;
}
