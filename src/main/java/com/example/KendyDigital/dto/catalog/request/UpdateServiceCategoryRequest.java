package com.example.KendyDigital.dto.catalog.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateServiceCategoryRequest(
        @Size(max = 100) String name,
        @Size(max = 120) @Pattern(regexp = "^$|^[a-z0-9-]+$", message = "Slug chỉ chứa chữ thường, số và dấu gạch ngang") String slug,
        @Size(max = 2000) String description,
        Integer sortOrder,
        Long parentId,
        @Size(max = 255) String microcopy,
        @Size(max = 50) String priceFrom,
        @Size(max = 100) String processingTime,
        @Size(max = 100) String warranty,
        @Size(max = 2000) String requirements,
        @Size(max = 50) String cta,
        Long version) {

    public UpdateServiceCategoryRequest(
            String name, String slug, String description, Integer sortOrder, Long parentId,
            String microcopy, String priceFrom, String processingTime, String warranty,
            String requirements, String cta) {
        this(name, slug, description, sortOrder, parentId, microcopy, priceFrom, processingTime,
                warranty, requirements, cta, null);
    }
}
