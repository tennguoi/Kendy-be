package com.example.KendyDigital.dto.notification.response;

import com.example.KendyDigital.model.admin.AdminNotification;
import java.time.Instant;
import java.util.UUID;

public record AdminNotificationResponse(
        UUID id,
        Long adminUserId,
        String title,
        String message,
        Instant readAt,
        Instant createdAt) {
    public static AdminNotificationResponse from(AdminNotification notification) {
        return new AdminNotificationResponse(
                notification.getPublicId(),
                notification.getAdminUserId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReadAt(),
                notification.getCreatedAt());
    }
}
