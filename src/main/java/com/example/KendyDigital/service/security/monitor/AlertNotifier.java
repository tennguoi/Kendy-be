package com.example.KendyDigital.service.security.monitor;

import com.example.KendyDigital.config.SecurityMonitorProperties;
import com.example.KendyDigital.model.security.SecuritySeverity;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.service.notification.EmailNotificationService;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Delivers security alerts through the configured channels (Telegram and email). Never includes
 * secrets/credentials in outbound messages.
 */
@Component
public class AlertNotifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlertNotifier.class);

    private final SecurityMonitorProperties properties;
    private final EmailNotificationService emailNotificationService;
    private final UserAccountRepository userAccountRepository;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public AlertNotifier(SecurityMonitorProperties properties, EmailNotificationService emailNotificationService,
            UserAccountRepository userAccountRepository) {
        this.properties = properties;
        this.emailNotificationService = emailNotificationService;
        this.userAccountRepository = userAccountRepository;
    }

    /** Send a plain operational digest by email regardless of severity. */
    public void digest(String subject, String body) {
        if (!properties.isEmailAlertsEnabled()) {
            return;
        }
        for (String recipient : resolveRecipients()) {
            try {
                emailNotificationService.sendRawEmail(recipient, subject, "<pre>" + escape(body) + "</pre>");
            } catch (RuntimeException exception) {
                LOGGER.warn("Digest email delivery failed for {}", recipient, exception);
            }
        }
    }

    public void notify(SecuritySeverity severity, String ruleCode, String title, String detail) {
        if (!severity.atLeast(SecuritySeverity.HIGH)) {
            return;
        }
        String message = "[" + severity + "] " + ruleCode + " — " + title + "\n" + (detail == null ? "" : detail);
        dispatchTelegram(message);
        if (severity == SecuritySeverity.CRITICAL) {
            dispatchEmail(severity, ruleCode, title, detail);
        }
    }

    private void dispatchTelegram(String message) {
        if (!properties.isTelegramEnabled() || properties.getTelegramBotToken().isBlank()
                || properties.getTelegramChatId().isBlank()) {
            return;
        }
        try {
            String url = "https://api.telegram.org/bot" + properties.getTelegramBotToken() + "/sendMessage";
            String body = "chat_id=" + URLEncoder.encode(properties.getTelegramChatId(), StandardCharsets.UTF_8)
                    + "&text=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(exception -> {
                        LOGGER.warn("Telegram alert delivery failed", exception);
                        return null;
                    });
        } catch (RuntimeException exception) {
            LOGGER.warn("Telegram alert could not be scheduled", exception);
        }
    }

    private void dispatchEmail(SecuritySeverity severity, String ruleCode, String title, String detail) {
        if (!properties.isEmailAlertsEnabled()) {
            return;
        }
        List<String> recipients = resolveRecipients();
        if (recipients.isEmpty()) {
            return;
        }
        String html = "<h2>Security alert: " + escape(title) + "</h2>"
                + "<p><strong>Severity:</strong> " + severity.name() + "<br/>"
                + "<strong>Rule:</strong> " + escape(ruleCode) + "</p>"
                + "<pre>" + escape(detail == null ? "" : detail) + "</pre>";
        for (String recipient : recipients) {
            try {
                emailNotificationService.sendRawEmail(recipient, "[" + severity + "] " + title, html);
            } catch (RuntimeException exception) {
                LOGGER.warn("Email alert delivery failed for {}", recipient, exception);
            }
        }
    }

    private List<String> resolveRecipients() {
        if (properties.getAlertRecipient() != null && !properties.getAlertRecipient().isBlank()) {
            return List.of(properties.getAlertRecipient());
        }
        try {
            return userAccountRepository.findByRoleIn(List.of(
                            com.example.KendyDigital.model.user.UserRole.SUPER_ADMIN))
                    .stream()
                    .map(user -> user.getEmail())
                    .filter(email -> email != null && !email.isBlank())
                    .toList();
        } catch (RuntimeException exception) {
            LOGGER.debug("Could not resolve security alert recipients", exception);
            return List.of();
        }
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
