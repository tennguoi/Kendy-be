package com.example.KendyDigital.dto;

public record UpdateServiceCategoryRequest(
        String name,
        String slug,
        String description,
        Integer sortOrder,
        Long parentId) {
}
