package com.example.KendyDigital.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateServiceCategoryRequest(
        @NotBlank String name,
        @NotBlank String slug,
        String description,
        Integer sortOrder,
        Long parentId) {
}
