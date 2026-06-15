package com.example.KendyDigital.service.notification;

import com.example.KendyDigital.config.AppEmailProperties;
import com.example.KendyDigital.model.user.UserAccount;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationServiceImpl  implements EmailNotificationService{
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailNotificationService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AppEmailProperties properties;

    public EmailNotificationServiceImpl(ObjectProvider<JavaMailSender> mailSenderProvider,
            AppEmailProperties properties) {
        this.mailSenderProvider = mailSenderProvider;
        this.properties = properties;
    }

    public void sendPasswordReset(UserAccount user, String token, Instant expiresAt) {
        String link = frontendUrl("/reset-password?token=" + encode(token));
        send(user.getEmail(), "Reset your KendyDigital password",
                "Use this link to reset your password:\n" + link
                        + "\n\nThis token expires at: " + expiresAt);
    }

    public void sendEmailVerification(UserAccount user, String token, Instant expiresAt) {
        String link = frontendUrl("/verify-email?token=" + encode(token));
        send(user.getEmail(), "Verify your KendyDigital email",
                "Use this link to verify your email:\n" + link
                        + "\n\nThis token expires at: " + expiresAt);
    }

    public void sendTwoFactorCode(UserAccount user, String code, Instant expiresAt) {
        send(user.getEmail(), "Your KendyDigital 2FA code",
                "Your 2FA email code is: " + code
                        + "\n\nThis code expires at: " + expiresAt
                        + "\nIf you did not try to sign in, change your password immediately.");
    }

    public void sendSecurityAlert(UserAccount user, String title, String message) {
        send(user.getEmail(), title, message);
    }

    public void sendUserNotification(UserAccount user, String title, String message, String actionUrl) {
        String body = message == null ? "" : message;
        if (actionUrl != null && !actionUrl.isBlank()) {
            body += "\n\nOpen: " + frontendUrl(actionUrl);
        }
        send(user.getEmail(), title, body);
    }

    private void send(String to, String subject, String body) {
        if (!properties.isEnabled()) {
            LOGGER.debug("Email disabled. Skipped sending '{}' to {}", subject, to);
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            LOGGER.warn("Email enabled but JavaMailSender is not available. Skipped sending '{}' to {}", subject, to);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.getFrom());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (MailException exception) {
            LOGGER.warn("Failed to send email '{}' to {}: {}", subject, to, exception.getMessage());
        }
    }

    private String frontendUrl(String path) {
        String base = properties.getFrontendBaseUrl();
        if (base.endsWith("/") && path.startsWith("/")) {
            return base.substring(0, base.length() - 1) + path;
        }
        if (!base.endsWith("/") && !path.startsWith("/")) {
            return base + "/" + path;
        }
        return base + path;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
