package com.example.KendyDigital.service.notification.helper;

import com.example.KendyDigital.model.content.ContentItem;
import com.example.KendyDigital.model.content.ContentType;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.repository.ContentItemRepository;
import com.example.KendyDigital.repository.SystemSettingRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class EmailTemplateRenderer {
    private final ContentItemRepository contentItemRepository;
    private final SystemSettingRepository systemSettingRepository;

    public EmailTemplateRenderer(ContentItemRepository contentItemRepository,
            SystemSettingRepository systemSettingRepository) {
        this.contentItemRepository = contentItemRepository;
        this.systemSettingRepository = systemSettingRepository;
    }

    public String renderHtmlTemplate(String slug, String fallbackHtml, UserAccount user, String link, String token,
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

    public String resolveSubject(String slug, String key, String fallback, UserAccount user, String link, String token,
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

    public String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String brandName() {
        return systemSettingRepository.findById("brand.name")
                .map(s -> s.getValue())
                .filter(v -> !v.isBlank())
                .orElse("KendyDigital");
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }
}
