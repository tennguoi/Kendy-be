package com.example.KendyDigital.dto.notification.response;

import com.example.KendyDigital.model.UserNotificationSettings;

public record UserNotificationSettingsResponse(
        boolean orderUpdates,
        boolean depositUpdates,
        boolean ticketUpdates,
        boolean walletUpdates,
        boolean securityUpdates,
        boolean emailNotifications) {
    public static UserNotificationSettingsResponse from(UserNotificationSettings settings) {
        return new UserNotificationSettingsResponse(
                settings.isOrderUpdates(),
                settings.isDepositUpdates(),
                settings.isTicketUpdates(),
                settings.isWalletUpdates(),
                settings.isSecurityUpdates(),
                settings.isEmailNotifications());
    }
}
