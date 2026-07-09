package com.example.KendyDigital.controller;

import com.example.KendyDigital.dto.email.request.EmailTestRequest;
import com.example.KendyDigital.model.content.ContentItem;
import com.example.KendyDigital.model.content.ContentType;
import com.example.KendyDigital.repository.ContentItemRepository;
import com.example.KendyDigital.repository.SystemSettingRepository;
import com.example.KendyDigital.service.notification.EmailNotificationService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class EmailTestController {
    private final EmailNotificationService emailNotificationService;
    private final ContentItemRepository contentItemRepository;
    private final SystemSettingRepository systemSettingRepository;

    public EmailTestController(EmailNotificationService emailNotificationService,
            ContentItemRepository contentItemRepository,
            SystemSettingRepository systemSettingRepository) {
        this.emailNotificationService = emailNotificationService;
        this.contentItemRepository = contentItemRepository;
        this.systemSettingRepository = systemSettingRepository;
    }

    @PostMapping("/api/admin/email/test")
    @Transactional(readOnly = true)
    public EmailTestResponse test(Authentication authentication, @Valid @RequestBody EmailTestRequest request) {
        String slug = request.slug().trim().toLowerCase(java.util.Locale.ROOT);

        String subject = contentItemRepository.findByTypeAndSlug(ContentType.EMAIL_TEMPLATE, slug)
                .map(ContentItem::getTitle)
                .filter(t -> !t.isBlank())
                .orElseGet(() -> systemSettingRepository.findById("email.template." + slug + ".subject")
                        .map(s -> s.getValue())
                        .orElse(null));
        String html = contentItemRepository.findByTypeAndSlug(ContentType.EMAIL_TEMPLATE, slug)
                .map(ContentItem::getContent)
                .filter(c -> c != null && !c.isBlank())
                .orElseGet(() -> systemSettingRepository.findById("email.template." + slug + ".body")
                        .map(s -> s.getValue())
                        .orElse(null));

        if (html == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No template found for slug: " + slug + ". Create one via /api/admin/content first.");
        }

        Map<String, String> ph = request.placeholders();
        String resolvedHtml = html;
        String resolvedSubject = subject;
        if (ph != null) {
            for (var entry : ph.entrySet()) {
                String val = entry.getValue() == null ? "" : entry.getValue();
                resolvedHtml = resolvedHtml.replace("{{" + entry.getKey() + "}}", val);
                if (resolvedSubject != null) {
                    resolvedSubject = resolvedSubject.replace("{{" + entry.getKey() + "}}", val);
                }
            }
        }

        if (request.sendTo() != null && !request.sendTo().isBlank()) {
            emailNotificationService.sendRawEmail(request.sendTo(),
                    resolvedSubject != null ? resolvedSubject : "Test — " + slug, resolvedHtml);
        }

        return new EmailTestResponse(slug, resolvedSubject, resolvedHtml, request.sendTo());
    }

    private record EmailTestResponse(String slug, String subject, String html, String sentTo) {}
}