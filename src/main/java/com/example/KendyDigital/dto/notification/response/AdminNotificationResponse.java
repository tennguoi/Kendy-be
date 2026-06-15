package com.example.KendyDigital.dto.notification.response;

import com.example.KendyDigital.model.admin.AdminNotification;
import java.time.Instant;

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
