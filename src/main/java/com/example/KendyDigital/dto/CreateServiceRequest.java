package com.example.KendyDigital.dto;

import java.math.BigDecimal;

import com.example.KendyDigital.model.ServiceStatus;
import com.example.KendyDigital.model.ServiceType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateServiceRequest(
        @NotBlank String name,
        @NotBlank String slug,
        String shortDescription,
        String description,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @DecimalMin("0.00") BigDecimal costPrice,
        ServiceType type,
        ServiceStatus status,
        String inputSchema,
        String processingTime,
        String warrantyPolicy,
        @Min(0) Integer sortOrder,
        Long categoryId,
        String metaTitle,
        String metaDescription,
        String iconUrl) {
}
