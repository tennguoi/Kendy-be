package com.example.KendyDigital.dto.notification.response;

import com.example.KendyDigital.model.user.UserNotification;
import java.time.Instant;

public record UserNotificationResponse(
        Long id,
        String title,
        String message,
        String type,
        String actionUrl,
        Instant readAt,
        Instant createdAt) {
    public static UserNotificationResponse from(UserNotification notification) {
        return new UserNotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.getActionUrl(),
                notification.getReadAt(),
                notification.getCreatedAt());
    }
}
