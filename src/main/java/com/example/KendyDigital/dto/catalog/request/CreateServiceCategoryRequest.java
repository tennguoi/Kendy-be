package com.example.KendyDigital.dto.catalog.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateServiceCategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 120) @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug chỉ chứa chữ thường, số và dấu gạch ngang") String slug,
        @Size(max = 2000) String description,
        Integer sortOrder,
        Long parentId,
        @Size(max = 255) String microcopy,
        @Size(max = 50) String priceFrom,
        @Size(max = 100) String processingTime,
        @Size(max = 100) String warranty,
        @Size(max = 2000) String requirements,
        @Size(max = 50) String cta) {
}
