package com.example.KendyDigital.controller;

import com.example.KendyDigital.dto.notification.request.BulkReadNotificationsRequest;
import com.example.KendyDigital.dto.notification.request.UserNotificationSettingsRequest;
import com.example.KendyDigital.dto.notification.response.UserNotificationResponse;
import com.example.KendyDigital.dto.notification.response.UserNotificationSettingsResponse;
import com.example.KendyDigital.security.CurrentUser;
import com.example.KendyDigital.service.notification.UserNotificationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserNotificationController {
    private final UserNotificationService userNotificationService;

    public UserNotificationController(UserNotificationService userNotificationService) {
        this.userNotificationService = userNotificationService;
    }

    @GetMapping("/api/notifications")
    public List<UserNotificationResponse> notifications(Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return userNotificationService.list(CurrentUser.require(authentication).userId(), page, size);
    }

    @GetMapping("/api/notifications/unread-count")
    public Map<String, Long> unreadCount(Authentication authentication) {
        return Map.of("unread", userNotificationService.unreadCount(CurrentUser.require(authentication).userId()));
    }

    @PostMapping("/api/notifications/{id}/read")
    public UserNotificationResponse markRead(Authentication authentication, @PathVariable Long id) {
        return userNotificationService.markRead(CurrentUser.require(authentication).userId(), id);
    }

    @PostMapping("/api/notifications/bulk-read")
    public List<UserNotificationResponse> bulkRead(Authentication authentication,
            @Valid @RequestBody BulkReadNotificationsRequest request) {
        return userNotificationService.bulkRead(CurrentUser.require(authentication).userId(), request.ids());
    }

    @GetMapping("/api/notifications/settings")
    public UserNotificationSettingsResponse settings(Authentication authentication) {
        return userNotificationService.settings(CurrentUser.require(authentication).userId());
    }

    @PutMapping("/api/notifications/settings")
    public UserNotificationSettingsResponse updateSettings(Authentication authentication,
            @RequestBody UserNotificationSettingsRequest request) {
        return userNotificationService.updateSettings(CurrentUser.require(authentication).userId(), request);
    }
}
