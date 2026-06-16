package com.example.KendyDigital.dto.catalog.response;

import com.example.KendyDigital.model.catalog.ServiceCategory;
import java.time.Instant;

public record ServiceCategoryResponse(
        Long id,
        String name,
        String slug,
        String description,
        int sortOrder,
        Long parentId,
        Instant createdAt,
        Instant updatedAt,
        String microcopy,
        String priceFrom,
        String processingTime,
        String warranty,
        String requirements,
        String cta) {
    public static ServiceCategoryResponse from(ServiceCategory category) {
        return new ServiceCategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getSortOrder(),
                category.getParent() == null ? null : category.getParent().getId(),
                category.getCreatedAt(),
                category.getUpdatedAt(),
                category.getMicrocopy(),
                category.getPriceFrom(),
                category.getProcessingTime(),
                category.getWarranty(),
                category.getRequirements(),
                category.getCta());
    }
}
