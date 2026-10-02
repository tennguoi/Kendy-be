package com.example.KendyDigital.dto.catalog.request;

import com.example.KendyDigital.model.catalog.ServiceCtaType;
import com.example.KendyDigital.model.catalog.AccessStrategy;
import com.example.KendyDigital.model.catalog.ServiceStatus;
import com.example.KendyDigital.model.catalog.ServiceStockStatus;
import com.example.KendyDigital.model.catalog.ServiceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateServiceRequest(
        @Size(max = 150) String name,
        @Size(max = 150) @Pattern(regexp = "^$|^[a-z0-9-]+$", message = "Slug chỉ chứa chữ thường, số và dấu gạch ngang") String slug,
        @Size(max = 500) String shortDescription,
        @Size(max = 10000) String description,
        @DecimalMin("0.00") BigDecimal price,
        @Size(max = 50) String priceText,
        @DecimalMin("0.00") BigDecimal costPrice,
        ServiceType type,
        AccessStrategy accessStrategy,
        @Min(1) Integer accessDurationDays,
        ServiceStatus status,
        ServiceStockStatus stockStatus,
        ServiceCtaType ctaType,
        @Size(max = 50) String pricingBadge,
        Boolean featured,
        Boolean publicVisible,
        @Size(max = 10000) String inputSchema,
        @Size(max = 5000) String requirements,
        @Size(max = 5000) String benefits,
        @Size(max = 5000) String usageNotes,
        @Size(max = 100) String processingTime,
        @Size(max = 5000) String warrantyPolicy,
        @Min(0) Integer sortOrder,
        Long categoryId,
        Boolean clearCategory,
        @Size(max = 200) String metaTitle,
        @Size(max = 1000) String metaDescription,
        @Size(max = 500) String iconUrl,
        Long version) {

    public UpdateServiceRequest(
            String name, String slug, String shortDescription, String description,
            BigDecimal price, String priceText, BigDecimal costPrice, ServiceType type,
            AccessStrategy accessStrategy, Integer accessDurationDays, ServiceStatus status,
            ServiceStockStatus stockStatus, ServiceCtaType ctaType, String pricingBadge,
            Boolean featured, Boolean publicVisible, String inputSchema, String requirements,
            String benefits, String usageNotes, String processingTime, String warrantyPolicy,
            Integer sortOrder, Long categoryId, Boolean clearCategory, String metaTitle,
            String metaDescription, String iconUrl) {
        this(name, slug, shortDescription, description, price, priceText, costPrice, type,
                accessStrategy, accessDurationDays, status, stockStatus, ctaType, pricingBadge,
                featured, publicVisible, inputSchema, requirements, benefits, usageNotes,
                processingTime, warrantyPolicy, sortOrder, categoryId, clearCategory, metaTitle,
                metaDescription, iconUrl, null);
    }
}
