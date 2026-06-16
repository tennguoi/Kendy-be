package com.example.KendyDigital.dto.catalog.request;

import jakarta.validation.constraints.NotBlank;

public record CreateServiceCategoryRequest(
        @NotBlank String name,
        @NotBlank String slug,
        String description,
        Integer sortOrder,
        Long parentId,
        String microcopy,
        String priceFrom,
        String processingTime,
        String warranty,
        String requirements,
        String cta) {
}
