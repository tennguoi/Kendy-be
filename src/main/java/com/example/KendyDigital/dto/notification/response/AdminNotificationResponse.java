package com.example.KendyDigital.dto.notification.response;

import java.time.Instant;

import com.example.KendyDigital.model.AdminNotification;

public record AdminNotificationResponse(
        Long id,
        Long adminUserId,
        String title,
        String message,
        Instant readAt,
        Instant createdAt) {
    public static AdminNotificationResponse from(AdminNotification notification) {
        return new AdminNotificationResponse(
                notification.getId(),
                notification.getAdminUserId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReadAt(),
                notification.getCreatedAt());
    }
}
