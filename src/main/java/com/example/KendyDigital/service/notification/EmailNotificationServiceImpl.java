package com.example.KendyDigital.service.notification;

import com.example.KendyDigital.config.AppEmailProperties;
import com.example.KendyDigital.model.content.ContentItem;
import com.example.KendyDigital.model.content.ContentType;
import com.example.KendyDigital.model.notification.EmailLog;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.repository.ContentItemRepository;
import com.example.KendyDigital.repository.EmailLogRepository;
import com.example.KendyDigital.repository.SystemSettingRepository;
import jakarta.mail.internet.MimeMessage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationServiceImpl  implements EmailNotificationService{
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailNotificationService.class);
    private static final String HTML_WRAP = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>body{font-family:Arial,sans-serif;line-height:1.6;color:#333;padding:24px;max-width:600px;margin:0 auto}</style></head><body>%s</body></html>";

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AppEmailProperties properties;
    private final EmailLogRepository emailLogRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final ContentItemRepository contentItemRepository;

    public EmailNotificationServiceImpl(ObjectProvider<JavaMailSender> mailSenderProvider,
            AppEmailProperties properties,
            EmailLogRepository emailLogRepository,
            SystemSettingRepository systemSettingRepository,
            ContentItemRepository contentItemRepository) {
        this.mailSenderProvider = mailSenderProvider;
        this.properties = properties;
        this.emailLogRepository = emailLogRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.contentItemRepository = contentItemRepository;
    }

    @Async("mailExecutor")
    public void sendPasswordReset(UserAccount user, String token, Instant expiresAt) {
        String link = frontendUrl("/reset-password?token=" + encode(token));
        String subject = template("password_reset", "email.template.password_reset.subject",
                "Reset your KendyDigital password", user, link, token, expiresAt, null);
        String body = loadHtmlTemplate("password_reset",
                "<h2>Reset password</h2><p>Click the link below to reset your password:</p><p><a href=\"{{link}}\">{{link}}</a></p><p>This link expires at: {{expiresAt}}</p>",
                user, link, token, expiresAt, null);
        send(user.getEmail(), subject, body);
    }

    @Async("mailExecutor")
    public void sendEmailVerification(UserAccount user, String token, Instant expiresAt) {
        String link = frontendUrl("/verify-email?token=" + encode(token));
        String subject = template("email_verification", "email.template.email_verification.subject",
                "Verify your KendyDigital email", user, link, token, expiresAt, null);
        String body = loadHtmlTemplate("email_verification",
                "<h2>Verify your email</h2><p>Click the link below to verify your email address:</p><p><a href=\"{{link}}\">{{link}}</a></p><p>This link expires at: {{expiresAt}}</p>",
                user, link, token, expiresAt, null);
        send(user.getEmail(), subject, body);
    }

    @Async("mailExecutor")
    public void sendTwoFactorCode(UserAccount user, String code, Instant expiresAt) {
        String subject = template("two_factor", "email.template.two_factor.subject",
                "Your KendyDigital 2FA code", user, null, null, expiresAt, code);
        String body = loadHtmlTemplate("two_factor",
                "<h2>Two-factor authentication</h2><p>Your verification code is:</p><h1 style=\"font-size:32px;letter-spacing:4px;color:#2563eb\">{{code}}</h1><p>This code expires at: {{expiresAt}}</p><p>If you did not request this code, secure your account immediately.</p>",
                user, null, null, expiresAt, code);
        send(user.getEmail(), subject, body);
    }

    @Async("mailExecutor")
    public void sendSecurityAlert(UserAccount user, String title, String message) {
        String body = String.format("<h2>%s</h2><p>%s</p>", escapeHtml(title), escapeHtml(message));
        send(user.getEmail(), title, body);
    }

    @Async("mailExecutor")
    public void sendUserNotification(UserAccount user, String title, String message, String actionUrl) {
        StringBuilder body = new StringBuilder();
        body.append("<h2>").append(escapeHtml(title)).append("</h2>");
        body.append("<p>").append(escapeHtml(message == null ? "" : message)).append("</p>");
        if (actionUrl != null && !actionUrl.isBlank()) {
            body.append("<p><a href=\"").append(escapeHtml(frontendUrl(actionUrl))).append("\">Open</a></p>");
        }
        send(user.getEmail(), title, body.toString());
    }

    private void send(String to, String subject, String bodyHtml) {
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
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setFrom(properties.getFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(bodyHtml, true);
            mailSender.send(mimeMessage);
        } catch (MailException exception) {
            status = "FAILED";
            errorMessage = exception.getMessage();
            LOGGER.warn("Failed to send email '{}' to {}: {}", subject, to, exception.getMessage());
        } catch (Exception exception) {
            status = "FAILED";
            errorMessage = exception.getMessage();
            LOGGER.warn("Failed to prepare email '{}' to {}: {}", subject, to, exception.getMessage());
        }
        try {
            emailLogRepository.save(new EmailLog(to, subject, bodyHtml, status, errorMessage, sentAt));
        } catch (Exception exception) {
            LOGGER.error("Failed to save email log to database: {}", exception.getMessage());
        }
    }

    private String loadHtmlTemplate(String slug, String fallbackHtml, UserAccount user, String link, String token,
            Instant expiresAt, String code) {
        Optional<ContentItem> template = contentItemRepository.findByTypeAndSlug(ContentType.EMAIL_TEMPLATE, slug);
        String html = template.map(ContentItem::getContent)
                .filter(content -> content != null && !content.isBlank())
                .orElse(fallbackHtml);
        String wrapped = html.contains("<!DOCTYPE") || html.contains("<html") ? html : String.format(HTML_WRAP, html);
        return wrapped
                .replace("{{name}}", nullToBlank(user.getName()))
                .replace("{{email}}", nullToBlank(user.getEmail()))
                .replace("{{link}}", nullToBlank(link))
                .replace("{{token}}", nullToBlank(token))
                .replace("{{code}}", nullToBlank(code))
                .replace("{{expiresAt}}", expiresAt == null ? "" : expiresAt.toString());
    }

    private String template(String slug, String key, String fallback, UserAccount user, String link, String token,
            Instant expiresAt, String code) {
        String value = contentItemRepository.findByTypeAndSlug(ContentType.EMAIL_TEMPLATE, slug)
                .map(ContentItem::getTitle)
                .filter(title -> !title.isBlank())
                .orElseGet(() -> systemSettingRepository.findById(key)
                        .map(setting -> setting.getValue())
                        .orElse(fallback));
        return value
                .replace("{{name}}", nullToBlank(user.getName()))
                .replace("{{email}}", nullToBlank(user.getEmail()))
                .replace("{{link}}", nullToBlank(link))
                .replace("{{token}}", nullToBlank(token))
                .replace("{{code}}", nullToBlank(code))
                .replace("{{expiresAt}}", expiresAt == null ? "" : expiresAt.toString());
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
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
}
