package com.example.KendyDigital.service.notification;

import com.example.KendyDigital.model.user.UserAccount;
import java.time.Instant;

public interface EmailNotificationService {
    void sendPasswordReset(UserAccount user, String token, Instant expiresAt);
    void sendEmailVerification(UserAccount user, String token, Instant expiresAt);
    void sendTwoFactorCode(UserAccount user, String code, Instant expiresAt);
    void sendSecurityAlert(UserAccount user, String title, String message);
    void sendUserNotification(UserAccount user, String title, String message, String actionUrl);
}
