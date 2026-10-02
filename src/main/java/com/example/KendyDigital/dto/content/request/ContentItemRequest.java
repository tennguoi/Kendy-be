package com.example.KendyDigital.dto.content.request;

import com.example.KendyDigital.model.content.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ContentItemRequest(
        @NotNull ContentType type,
        @NotBlank @Size(max = 150) @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug chỉ chứa chữ thường, số và dấu gạch ngang") String slug,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 1000) String summary,
        @Size(max = 20000) String content,
        @Size(max = 500) String imageUrl,
        @Size(max = 500) String ctaUrl,
        @Size(max = 200) String seoTitle,
        @Size(max = 1000) String seoDescription,
        Boolean published,
        Integer sortOrder,
        Long version) {

    public ContentItemRequest(ContentType type, String slug, String title, String summary,
            String content, String imageUrl, String ctaUrl, String seoTitle,
            String seoDescription, Boolean published, Integer sortOrder) {
        this(type, slug, title, summary, content, imageUrl, ctaUrl, seoTitle, seoDescription, published, sortOrder, null);
    }
}

