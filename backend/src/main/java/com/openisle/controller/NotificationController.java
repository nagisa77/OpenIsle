package com.openisle.controller;

import com.openisle.dto.NotificationDto;
import com.openisle.dto.NotificationMarkReadRequest;
import com.openisle.dto.NotificationUnreadCountDto;
import com.openisle.dto.NotificationPreferenceDto;
import com.openisle.dto.NotificationPreferenceUpdateRequest;
import com.openisle.mapper.NotificationMapper;
import com.openisle.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/** Endpoints for user notifications. */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;

    @GetMapping
    public List<NotificationDto> list(@RequestParam(value = "read", required = false) Boolean read,
                                      Authentication auth) {
        return notificationService.listNotifications(auth.getName(), read).stream()
                .map(notificationMapper::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/unread-count")
    public NotificationUnreadCountDto unreadCount(Authentication auth) {
        long count = notificationService.countUnread(auth.getName());
        NotificationUnreadCountDto uc = new NotificationUnreadCountDto();
        uc.setCount(count);
        return uc;
    }

    @PostMapping("/read")
    public void markRead(@RequestBody NotificationMarkReadRequest req, Authentication auth) {
        notificationService.markRead(auth.getName(), req.getIds());
    }

    @GetMapping("/prefs")
    public List<NotificationPreferenceDto> prefs(Authentication auth) {
        return notificationService.listPreferences(auth.getName());
    }

    @PostMapping("/prefs")
    public void updatePref(@RequestBody NotificationPreferenceUpdateRequest req, Authentication auth) {
        notificationService.updatePreference(auth.getName(), req.getType(), req.isEnabled());
    }
}
