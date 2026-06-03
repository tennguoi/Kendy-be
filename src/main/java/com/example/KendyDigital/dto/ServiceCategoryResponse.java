package com.example.KendyDigital.dto;

import java.time.Instant;

import com.example.KendyDigital.model.ServiceCategory;

public record ServiceCategoryResponse(
        Long id,
        String name,
        String slug,
        String description,
        int sortOrder,
        Long parentId,
        Instant createdAt,
        Instant updatedAt) {
    public static ServiceCategoryResponse from(ServiceCategory category) {
        return new ServiceCategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getSortOrder(),
                category.getParent() == null ? null : category.getParent().getId(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }
}
