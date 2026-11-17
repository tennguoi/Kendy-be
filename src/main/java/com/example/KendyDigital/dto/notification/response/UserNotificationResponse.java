package com.example.KendyDigital.dto.notification.response;

import com.example.KendyDigital.model.user.UserNotification;
import java.time.Instant;
import java.util.UUID;

public record UserNotificationResponse(
        UUID id,
        String title,
        String message,
        String type,
        String actionUrl,
        Instant readAt,
        Instant createdAt) {
    public static UserNotificationResponse from(UserNotification notification) {
        return new UserNotificationResponse(
                notification.getPublicId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.getActionUrl(),
                notification.getReadAt(),
                notification.getCreatedAt());
    }
}
