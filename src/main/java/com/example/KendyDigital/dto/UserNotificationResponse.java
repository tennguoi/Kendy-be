package com.example.KendyDigital.dto;

import java.time.Instant;

import com.example.KendyDigital.model.UserNotification;

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
