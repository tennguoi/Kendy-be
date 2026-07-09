package com.example.KendyDigital.service.notification;

import com.example.KendyDigital.config.AppEmailProperties;
import com.example.KendyDigital.model.content.ContentItem;
import com.example.KendyDigital.model.content.ContentType;
import com.example.KendyDigital.model.notification.EmailLog;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.repository.ContentItemRepository;
import com.example.KendyDigital.repository.EmailLogRepository;
import com.example.KendyDigital.repository.SystemSettingRepository;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationServiceImpl  implements EmailNotificationService{
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailNotificationService.class);
    private static final String PASSWORD_RESET_FALLBACK = "Use code %s to reset your password. Valid until %s.";
    private static final String EMAIL_VERIFY_FALLBACK = "Verify your email by visiting: %s . This link expires at: %s.";
    private static final String TWO_FACTOR_FALLBACK = "Your verification code is: %s. This code expires at: %s.";
    private static final String DEPOSIT_FALLBACK = "Your deposit %s (%s VND) has been %s. Valid until: %s.";
    private static final String WALLET_FALLBACK = "Your wallet has been %sd %s VND. Reason: %s. Admin: %d.";
    private static final String ACCOUNT_FALLBACK = "Your account has been %s: %s. Reason: %s.";

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
        String code = token;
        String link = frontendUrl("/reset-password");
        String subject = template("password_reset", "email.template.password_reset.subject",
                "Reset your KendyDigital password", user, link, token, expiresAt, code);
        String body = loadHtmlTemplate("password_reset",
                String.format(PASSWORD_RESET_FALLBACK, code, expiresAt),
                user, link, token, expiresAt, code);
        send(user.getEmail(), subject, body);
    }

    @Async("mailExecutor")
    public void sendEmailVerification(UserAccount user, String token, Instant expiresAt) {
        String link = frontendUrl("/verify-email?token=" + encode(token));
        String subject = template("email_verification", "email.template.email_verification.subject",
                "Verify your KendyDigital email", user, link, token, expiresAt, null);
        String body = loadHtmlTemplate("email_verification",
                String.format(EMAIL_VERIFY_FALLBACK, link, expiresAt),
                user, link, token, expiresAt, null);
        send(user.getEmail(), subject, body);
    }

    @Async("mailExecutor")
    public void sendTwoFactorCode(UserAccount user, String code, Instant expiresAt) {
        String subject = template("two_factor", "email.template.two_factor.subject",
                "Your KendyDigital 2FA code", user, null, null, expiresAt, code);
        String body = loadHtmlTemplate("two_factor",
                String.format(TWO_FACTOR_FALLBACK, code, expiresAt),
                user, null, null, expiresAt, code);
        send(user.getEmail(), subject, body);
    }

    @Async("mailExecutor")
    public void sendSecurityAlert(UserAccount user, String title, String message) {
        String body = loadHtmlTemplate("security_alert",
                "<h2>" + escapeHtml(title) + "</h2><p>" + escapeHtml(message) + "</p>",
                user, null, null, null, null);
        send(user.getEmail(), title, body);
    }

    @Async("mailExecutor")
    public void sendUserNotification(UserAccount user, String title, String message, String actionUrl) {
        StringBuilder fallback = new StringBuilder();
        fallback.append("<h2>").append(escapeHtml(title)).append("</h2>");
        fallback.append("<p>").append(escapeHtml(message == null ? "" : message)).append("</p>");
        if (actionUrl != null && !actionUrl.isBlank()) {
            fallback.append("<p><a href=\"").append(escapeHtml(frontendUrl(actionUrl))).append("\">Open</a></p>");
        }
        String body = loadHtmlTemplate("user_notification", fallback.toString(), user, null, null, null, null);
        send(user.getEmail(), title, body);
    }

    @Async("mailExecutor")
    public void sendDepositNotification(UserAccount user, String depositCode, String amount, String status, Instant expiresAt) {
        String subject = template("deposit_created", "email.template.deposit_created.subject",
                "Deposit " + status + ": " + depositCode, user, null, null, expiresAt, null);
        String link = frontendUrl("/wallet");
        String body = loadHtmlTemplate("deposit_created",
                String.format(DEPOSIT_FALLBACK, depositCode, amount, status, expiresAt),
                user, link, null, expiresAt, null);
        send(user.getEmail(), subject, body);
    }

    @Async("mailExecutor")
    public void sendWalletAdjusted(UserAccount user, String direction, String amount, String reason, Long adminUserId) {
        String subject = template("wallet_adjusted", "email.template.wallet_adjusted.subject",
                "Wallet " + direction + " of " + amount, user, null, null, null, null);
        String link = frontendUrl("/wallet");
        String body = loadHtmlTemplate("wallet_adjusted",
                String.format(WALLET_FALLBACK, direction, amount, reason, adminUserId),
                user, link, null, null, null);
        send(user.getEmail(), subject, body);
    }

    @Async("mailExecutor")
    public void sendAccountStatusChanged(UserAccount user, String changeType, String newValue, String reason) {
        String subject = template("account_status_changed", "email.template.account_status_changed.subject",
                "Account " + changeType + " updated", user, null, null, null, null);
        String link = frontendUrl("/account/security");
        String body = loadHtmlTemplate("account_status_changed",
                String.format(ACCOUNT_FALLBACK, changeType, newValue, reason),
                user, link, null, null, null);
        send(user.getEmail(), subject, body);
    }

    @Async("mailExecutor")
    public void sendTestEmail(String to, String slug, Map<String, String> placeholders) {
        String fallbackHtml = "<h2>Test email: " + slug + "</h2><p>This is a test email template.</p>";
        String html = loadHtmlTemplate(slug, fallbackHtml, dummyUser(), null, null, null, null);
        for (var entry : placeholders.entrySet()) {
            html = html.replace("{{" + entry.getKey() + "}}",
                    entry.getValue() == null ? "" : entry.getValue());
        }
        String subject = "Test email — " + slug;
        send(to, subject, html);
    }

    @Async("mailExecutor")
    public void sendRawEmail(String to, String subject, String html) {
        send(to, subject, html);
    }

    private static UserAccount dummyUser() {
        var user = new UserAccount() {};
        user.setName("Test User");
        user.setEmail("test@example.com");
        return user;
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

            String fromConfig = properties.getFrom();
            InternetAddress fromAddress = null;

            if (fromConfig != null && !fromConfig.isBlank()) {
                try {
                    InternetAddress[] parsed = InternetAddress.parse(fromConfig);
                    if (parsed.length > 0) {
                        fromAddress = parsed[0];
                        if (fromAddress.getPersonal() == null || fromAddress.getPersonal().isBlank()) {
                            fromAddress.setPersonal("KendyDigital", "UTF-8");
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse app.email.from '{}', falling back: {}", fromConfig, e.getMessage());
                }
            }

            if (fromAddress == null) {
                String fallbackEmail = "no-reply@kendydigital.local";
                if (mailSender instanceof JavaMailSenderImpl impl) {
                    String smtpUser = impl.getUsername();
                    if (smtpUser != null && !smtpUser.isBlank()) {
                        fallbackEmail = smtpUser;
                    }
                }
                fromAddress = new InternetAddress(fallbackEmail, "KendyDigital", "UTF-8");
            }

            helper.setFrom(fromAddress);
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
                .orElseGet(() -> systemSettingRepository.findById("email.template." + slug + ".body")
                        .map(setting -> setting.getValue())
                        .filter(value -> value != null && !value.isBlank())
                        .orElse(null));
        if (html == null || html.isBlank()) {
            html = fallbackHtml;
        }
        return html
                .replace("{{name}}", nullToBlank(user.getName()))
                .replace("{{email}}", nullToBlank(user.getEmail()))
                .replace("{{link}}", nullToBlank(link))
                .replace("{{token}}", nullToBlank(token))
                .replace("{{code}}", nullToBlank(code))
                .replace("{{brand}}", brandName())
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
                .replace("{{brand}}", brandName())
                .replace("{{expiresAt}}", expiresAt == null ? "" : expiresAt.toString());
    }

    private String brandName() {
        return systemSettingRepository.findById("brand.name")
                .map(s -> s.getValue())
                .filter(v -> !v.isBlank())
                .orElse("KendyDigital");
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
