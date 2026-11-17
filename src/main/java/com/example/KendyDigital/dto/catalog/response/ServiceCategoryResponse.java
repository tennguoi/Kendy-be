package com.example.KendyDigital.dto.catalog.response;

import com.example.KendyDigital.model.catalog.ServiceCategory;
import java.time.Instant;
import java.util.UUID;

public record ServiceCategoryResponse(
        UUID id,
        String name,
        String slug,
        String description,
        int sortOrder,
        UUID parentId,
        Instant createdAt,
        Instant updatedAt,
        String microcopy,
        String priceFrom,
        String processingTime,
        String warranty,
        String requirements,
        String cta,
        Long version) {

    public ServiceCategoryResponse(
            UUID id, String name, String slug, String description, int sortOrder,
            UUID parentId, Instant createdAt, Instant updatedAt, String microcopy,
            String priceFrom, String processingTime, String warranty,
            String requirements, String cta) {
        this(id, name, slug, description, sortOrder, parentId, createdAt, updatedAt,
                microcopy, priceFrom, processingTime, warranty, requirements, cta, 0L);
    }

    public static ServiceCategoryResponse from(ServiceCategory category) {
        return new ServiceCategoryResponse(
                category.getPublicId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getSortOrder(),
                category.getParent() == null ? null : category.getParent().getPublicId(),
                category.getCreatedAt(),
                category.getUpdatedAt(),
                category.getMicrocopy(),
                category.getPriceFrom(),
                category.getProcessingTime(),
                category.getWarranty(),
                category.getRequirements(),
                category.getCta(),
                category.getVersion());
    }
}
