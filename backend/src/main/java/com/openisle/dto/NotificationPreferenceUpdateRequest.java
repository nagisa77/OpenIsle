package com.openisle.dto;

import com.openisle.model.NotificationType;
import lombok.Data;

/** Request to update a single notification preference. */
@Data
public class NotificationPreferenceUpdateRequest {
    private NotificationType type;
    private boolean enabled;
}
