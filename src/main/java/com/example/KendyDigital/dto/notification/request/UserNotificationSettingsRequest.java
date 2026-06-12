package com.example.KendyDigital.dto.notification.request;

public record UserNotificationSettingsRequest(
        Boolean orderUpdates,
        Boolean depositUpdates,
        Boolean ticketUpdates,
        Boolean walletUpdates,
        Boolean securityUpdates,
        Boolean emailNotifications) {
}
