package com.example.KendyDigital.dto.catalog.response;

import com.example.KendyDigital.model.catalog.ServiceCtaType;
import com.example.KendyDigital.model.catalog.ServiceItem;
import com.example.KendyDigital.model.catalog.ServiceStatus;
import com.example.KendyDigital.model.catalog.ServiceStockStatus;
import com.example.KendyDigital.model.catalog.ServiceType;
import java.math.BigDecimal;
import java.util.UUID;

public record ServicePricingResponse(
        UUID id,
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
        UUID categoryId,
        String categoryName,
        String categorySlug) {
    public static ServicePricingResponse from(ServiceItem service) {
        return new ServicePricingResponse(
                service.getPublicId(),
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
                service.getCategory() == null ? null : service.getCategory().getPublicId(),
                service.getCategory() == null ? null : service.getCategory().getName(),
                service.getCategory() == null ? null : service.getCategory().getSlug());
    }
}
