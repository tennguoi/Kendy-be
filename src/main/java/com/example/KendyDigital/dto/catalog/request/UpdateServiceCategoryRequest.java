package com.example.KendyDigital.dto.catalog.request;

public record UpdateServiceCategoryRequest(
        String name,
        String slug,
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
