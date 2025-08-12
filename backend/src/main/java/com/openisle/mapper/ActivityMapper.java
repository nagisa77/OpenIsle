package com.openisle.mapper;

import com.openisle.dto.ActivityDto;
import com.openisle.model.Activity;
import org.springframework.stereotype.Component;

/** Mapper for activity entities. */
@Component
public class ActivityMapper {

    public ActivityDto toDto(Activity a) {
        ActivityDto dto = new ActivityDto();
        dto.setId(a.getId());
        dto.setTitle(a.getTitle());
        dto.setIcon(a.getIcon());
        dto.setContent(a.getContent());
        dto.setStartTime(a.getStartTime());
        dto.setEndTime(a.getEndTime());
        dto.setType(a.getType());
        dto.setEnded(a.isEnded());
        return dto;
    }
}
