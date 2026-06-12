package com.example.KendyDigital.dto.catalog.response;

import java.math.BigDecimal;

import com.example.KendyDigital.model.ServiceCategory;
import com.example.KendyDigital.model.ServiceCtaType;
import com.example.KendyDigital.model.ServiceItem;
import com.example.KendyDigital.model.ServiceStatus;
import com.example.KendyDigital.model.ServiceStockStatus;
import com.example.KendyDigital.model.ServiceType;

public record ServiceResponse(
        Long id,
        String name,
        String slug,
        String shortDescription,
        String description,
        BigDecimal price,
        String priceText,
        BigDecimal costPrice,
        ServiceType type,
        ServiceStatus status,
        ServiceStockStatus stockStatus,
        ServiceCtaType ctaType,
        String pricingBadge,
        boolean featured,
        boolean publicVisible,
        String inputSchema,
        String requirements,
        String benefits,
        String usageNotes,
        String processingTime,
        String warrantyPolicy,
        int sortOrder,
        Long categoryId,
        String categoryName,
        String metaTitle,
        String metaDescription,
        String iconUrl) {
    public static ServiceResponse from(ServiceItem service) {
        return new ServiceResponse(
                service.getId(),
                service.getName(),
                service.getSlug(),
                service.getShortDescription(),
                service.getDescription(),
                service.getPrice(),
                service.getPriceText(),
                service.getCostPrice(),
                service.getType(),
                service.getStatus(),
                service.getStockStatus(),
                service.getCtaType(),
                service.getPricingBadge(),
                service.isFeatured(),
                service.isPublicVisible(),
                service.getInputSchema(),
                service.getRequirements(),
                service.getBenefits(),
                service.getUsageNotes(),
                service.getProcessingTime(),
                service.getWarrantyPolicy(),
                service.getSortOrder(),
                service.getCategory() == null ? null : service.getCategory().getId(),
                service.getCategory() == null ? null : service.getCategory().getName(),
                service.getMetaTitle(),
                service.getMetaDescription(),
                service.getIconUrl());
    }
}
