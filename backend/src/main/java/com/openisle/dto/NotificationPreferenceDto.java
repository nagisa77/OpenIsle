package com.openisle.dto;

import com.openisle.model.NotificationType;
import lombok.Data;

/** User notification preference DTO. */
@Data
public class NotificationPreferenceDto {
    private NotificationType type;
    private boolean enabled;
}
