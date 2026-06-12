package com.example.KendyDigital.dto.catalog.request;

import java.math.BigDecimal;

import com.example.KendyDigital.model.ServiceCtaType;
import com.example.KendyDigital.model.ServiceStatus;
import com.example.KendyDigital.model.ServiceStockStatus;
import com.example.KendyDigital.model.ServiceType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

public record UpdateServiceRequest(
        String name,
        String slug,
        String shortDescription,
        String description,
        @DecimalMin("0.00") BigDecimal price,
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
        Boolean clearCategory,
        String metaTitle,
        String metaDescription,
        String iconUrl) {
}
