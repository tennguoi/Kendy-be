package com.example.KendyDigital.dto.catalog.request;

import com.example.KendyDigital.model.catalog.ServiceCtaType;
import com.example.KendyDigital.model.catalog.ServiceStatus;
import com.example.KendyDigital.model.catalog.ServiceStockStatus;
import com.example.KendyDigital.model.catalog.ServiceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateServiceRequest(
        @NotBlank String name,
        @NotBlank String slug,
        String shortDescription,
        String description,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        String priceText,
        @DecimalMin("0.00") BigDecimal costPrice,
        ServiceType type,
        ServiceStatus status,
        ServiceStockStatus stockStatus,
        ServiceCtaType ctaType,
        String pricingBadge,
        Boolean featured,
        Boolean publicVisible,
        String inputSchema,
        String requirements,
        String benefits,
        String usageNotes,
        String processingTime,
        String warrantyPolicy,
        @Min(0) Integer sortOrder,
        Long categoryId,
        String metaTitle,
        String metaDescription,
        String iconUrl) {
}
