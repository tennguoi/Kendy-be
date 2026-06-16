package com.example.KendyDigital.dto.content.response;

import com.example.KendyDigital.model.content.ContentItem;
import com.example.KendyDigital.model.content.ContentType;
import java.time.Instant;

public record ContentItemResponse(
        Long id,
        ContentType type,
        String slug,
        String title,
        String summary,
        String content,
        String imageUrl,
        String ctaUrl,
        String seoTitle,
        String seoDescription,
        boolean published,
        int sortOrder,
        Long updatedBy,
        Instant createdAt,
        Instant updatedAt) {
    public static ContentItemResponse from(ContentItem item) {
        return new ContentItemResponse(
                item.getId(),
                item.getType(),
                item.getSlug(),
                item.getTitle(),
                item.getSummary(),
                item.getContent(),
                item.getImageUrl(),
                item.getCtaUrl(),
                item.getSeoTitle(),
                item.getSeoDescription(),
                item.isPublished(),
                item.getSortOrder(),
                item.getUpdatedBy(),
                item.getCreatedAt(),
                item.getUpdatedAt());
    }
}
