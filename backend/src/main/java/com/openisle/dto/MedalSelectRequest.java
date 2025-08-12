package com.openisle.dto;

import com.openisle.model.MedalType;
import lombok.Data;

@Data
public class MedalSelectRequest {
    private MedalType type;
}
