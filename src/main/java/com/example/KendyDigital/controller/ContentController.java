package com.example.KendyDigital.controller;

import com.example.KendyDigital.dto.content.request.ContentItemRequest;
import com.example.KendyDigital.dto.content.response.ContentItemResponse;
import com.example.KendyDigital.model.content.ContentItem;
import com.example.KendyDigital.model.content.ContentType;
import com.example.KendyDigital.repository.ContentItemRepository;
import com.example.KendyDigital.security.CurrentUser;
import com.example.KendyDigital.service.audit.AuditService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ContentController {
    private final ContentItemRepository contentItemRepository;
    private final AuditService auditService;

    public ContentController(ContentItemRepository contentItemRepository, AuditService auditService) {
        this.contentItemRepository = contentItemRepository;
        this.auditService = auditService;
    }

    @GetMapping("/api/content")
    @Transactional(readOnly = true)
    public List<ContentItemResponse> publicList(@RequestParam ContentType type) {
        return contentItemRepository.findAllByTypeAndPublishedOrderBySortOrderAscCreatedAtDesc(type, true)
                .stream()
                .map(ContentItemResponse::from)
                .toList();
    }

    @GetMapping("/api/content/{type}/{slug}")
    @Transactional(readOnly = true)
    public ContentItemResponse publicDetail(@PathVariable ContentType type, @PathVariable String slug) {
        ContentItem item = contentItemRepository.findByTypeAndSlug(type, normalizeSlug(slug))
                .filter(ContentItem::isPublished)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found"));
        return ContentItemResponse.from(item);
    }

    @GetMapping("/api/admin/content")
    @Transactional(readOnly = true)
    public List<ContentItemResponse> adminList(@RequestParam(required = false) ContentType type,
            @RequestParam(required = false) Boolean published,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer limit) {
        return contentItemRepository.search(type, published, likePattern(query), page(limit))
                .stream()
                .map(ContentItemResponse::from)
                .toList();
    }

    @PostMapping("/api/admin/content")
    @Transactional
    public ContentItemResponse create(Authentication authentication, @Valid @RequestBody ContentItemRequest request) {
        Long adminUserId = CurrentUser.require(authentication).userId();
        String slug = normalizeSlug(request.slug());
        contentItemRepository.findByTypeAndSlug(request.type(), slug).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug already exists for this content type");
        });
        ContentItem item = new ContentItem(request.type(), slug, request.title().trim(), adminUserId);
        apply(item, request, adminUserId);
        ContentItem saved = contentItemRepository.save(item);
        auditService.recordAdmin(adminUserId, "CONTENT_CREATED", "CONTENT_ITEM", saved.getId(),
                "type=" + saved.getType() + ",slug=" + saved.getSlug());
        return ContentItemResponse.from(saved);
    }

    @PutMapping("/api/admin/content/{id}")
    @Transactional
    public ContentItemResponse update(Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody ContentItemRequest request) {
        Long adminUserId = CurrentUser.require(authentication).userId();
        ContentItem item = contentItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found"));
        String slug = normalizeSlug(request.slug());
        contentItemRepository.findByTypeAndSlug(request.type(), slug)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug already exists for this content type");
                });
        item.setType(request.type());
        apply(item, request, adminUserId);
        auditService.recordAdmin(adminUserId, "CONTENT_UPDATED", "CONTENT_ITEM", item.getId(),
                "type=" + item.getType() + ",slug=" + item.getSlug());
        return ContentItemResponse.from(item);
    }

    @DeleteMapping("/api/admin/content/{id}")
    @Transactional
    public void delete(Authentication authentication, @PathVariable Long id) {
        Long adminUserId = CurrentUser.require(authentication).userId();
        ContentItem item = contentItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found"));
        contentItemRepository.delete(item);
        auditService.recordAdmin(adminUserId, "CONTENT_DELETED", "CONTENT_ITEM", id, null);
    }

    private void apply(ContentItem item, ContentItemRequest request, Long adminUserId) {
        item.update(
                normalizeSlug(request.slug()),
                request.title().trim(),
                blankToNull(request.summary()),
                blankToNull(request.content()),
                blankToNull(request.imageUrl()),
                blankToNull(request.ctaUrl()),
                blankToNull(request.seoTitle()),
                blankToNull(request.seoDescription()),
                Boolean.TRUE.equals(request.published()),
                request.sortOrder() == null ? 0 : request.sortOrder(),
                adminUserId);
    }

    private PageRequest page(Integer limit) {
        return PageRequest.of(0, limit == null ? 100 : Math.max(1, Math.min(limit, 500)));
    }

    private String normalizeSlug(String value) {
        return value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String likePattern(String value) {
        return value == null || value.isBlank() ? null : "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
