package com.example.KendyDigital.service.notification;

import com.example.KendyDigital.config.AppEmailProperties;
import com.example.KendyDigital.model.notification.EmailLog;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.repository.EmailLogRepository;
import com.example.KendyDigital.repository.SystemSettingRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationServiceImpl  implements EmailNotificationService{
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailNotificationService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AppEmailProperties properties;
    private final EmailLogRepository emailLogRepository;
    private final SystemSettingRepository systemSettingRepository;

    public EmailNotificationServiceImpl(ObjectProvider<JavaMailSender> mailSenderProvider,
            AppEmailProperties properties,
            EmailLogRepository emailLogRepository,
            SystemSettingRepository systemSettingRepository) {
        this.mailSenderProvider = mailSenderProvider;
        this.properties = properties;
        this.emailLogRepository = emailLogRepository;
        this.systemSettingRepository = systemSettingRepository;
    }

    @Async("jobExecutor")
    public void sendPasswordReset(UserAccount user, String token, Instant expiresAt) {
        String link = frontendUrl("/reset-password?token=" + encode(token));
        send(user.getEmail(),
                template("email.template.password_reset.subject", "Reset your KendyDigital password", user, link,
                        token, expiresAt, null),
                template("email.template.password_reset.body",
                        "Use this link to reset your password:\n{{link}}\n\nThis token expires at: {{expiresAt}}",
                        user, link, token, expiresAt, null));
    }

    @Async("jobExecutor")
    public void sendEmailVerification(UserAccount user, String token, Instant expiresAt) {
        String link = frontendUrl("/verify-email?token=" + encode(token));
        send(user.getEmail(),
                template("email.template.email_verification.subject", "Verify your KendyDigital email", user, link,
                        token, expiresAt, null),
                template("email.template.email_verification.body",
                        "Use this link to verify your email:\n{{link}}\n\nThis token expires at: {{expiresAt}}",
                        user, link, token, expiresAt, null));
    }

    @Async("jobExecutor")
    public void sendTwoFactorCode(UserAccount user, String code, Instant expiresAt) {
        send(user.getEmail(),
                template("email.template.two_factor.subject", "Your KendyDigital 2FA code", user, null, null,
                        expiresAt, code),
                template("email.template.two_factor.body",
                        "Your 2FA email code is: {{code}}\n\nThis code expires at: {{expiresAt}}\nIf you did not try to sign in, change your password immediately.",
                        user, null, null, expiresAt, code));
    }

    @Async("jobExecutor")
    public void sendSecurityAlert(UserAccount user, String title, String message) {
        send(user.getEmail(), title, message);
    }

    @Async("jobExecutor")
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
        String status = "SUCCESS";
        String errorMessage = null;
        Instant sentAt = Instant.now();
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.getFrom());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (MailException exception) {
            status = "FAILED";
            errorMessage = exception.getMessage();
            LOGGER.warn("Failed to send email '{}' to {}: {}", subject, to, exception.getMessage());
        }
        try {
            emailLogRepository.save(new EmailLog(to, subject, body, status, errorMessage, sentAt));
        } catch (Exception exception) {
            LOGGER.error("Failed to save email log to database: {}", exception.getMessage());
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

    private String template(String key, String fallback, UserAccount user, String link, String token, Instant expiresAt,
            String code) {
        String value = systemSettingRepository.findById(key).map(setting -> setting.getValue()).orElse(fallback);
        return value
                .replace("{{name}}", nullToBlank(user.getName()))
                .replace("{{email}}", nullToBlank(user.getEmail()))
                .replace("{{link}}", nullToBlank(link))
                .replace("{{token}}", nullToBlank(token))
                .replace("{{code}}", nullToBlank(code))
                .replace("{{expiresAt}}", expiresAt == null ? "" : expiresAt.toString());
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
