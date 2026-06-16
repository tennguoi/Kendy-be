package com.example.KendyDigital.dto.content.request;

import com.example.KendyDigital.model.content.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ContentItemRequest(
        @NotNull ContentType type,
        @NotBlank String slug,
        @NotBlank String title,
        String summary,
        String content,
        String imageUrl,
        String ctaUrl,
        String seoTitle,
        String seoDescription,
        Boolean published,
        Integer sortOrder) {
}
