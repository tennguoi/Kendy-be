package com.example.KendyDigital.dto;

import java.math.BigDecimal;

import com.example.KendyDigital.model.ServiceCtaType;
import com.example.KendyDigital.model.ServiceItem;
import com.example.KendyDigital.model.ServiceStatus;
import com.example.KendyDigital.model.ServiceStockStatus;
import com.example.KendyDigital.model.ServiceType;

public record ServicePricingResponse(
        Long id,
        String name,
        String slug,
        String shortDescription,
        BigDecimal price,
        String priceText,
        ServiceType type,
        ServiceStatus status,
        ServiceStockStatus stockStatus,
        ServiceCtaType ctaType,
        String pricingBadge,
        boolean featured,
        boolean publicVisible,
        String processingTime,
        String warrantyPolicy,
        String requirements,
        String usageNotes,
        int sortOrder,
        Long categoryId,
        String categoryName,
        String categorySlug) {
    public static ServicePricingResponse from(ServiceItem service) {
        return new ServicePricingResponse(
                service.getId(),
                service.getName(),
                service.getSlug(),
                service.getShortDescription(),
                service.getPrice(),
                service.getPriceText(),
                service.getType(),
                service.getStatus(),
                service.getStockStatus(),
                service.getCtaType(),
                service.getPricingBadge(),
                service.isFeatured(),
                service.isPublicVisible(),
                service.getProcessingTime(),
                service.getWarrantyPolicy(),
                service.getRequirements(),
                service.getUsageNotes(),
                service.getSortOrder(),
                service.getCategory() == null ? null : service.getCategory().getId(),
                service.getCategory() == null ? null : service.getCategory().getName(),
                service.getCategory() == null ? null : service.getCategory().getSlug());
    }
}
