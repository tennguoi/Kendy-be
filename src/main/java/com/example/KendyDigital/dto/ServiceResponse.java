package com.example.KendyDigital.dto;

import java.math.BigDecimal;

import com.example.KendyDigital.model.ServiceCategory;
import com.example.KendyDigital.model.ServiceItem;
import com.example.KendyDigital.model.ServiceStatus;
import com.example.KendyDigital.model.ServiceType;

public record ServiceResponse(
        Long id,
        String name,
        String slug,
        String shortDescription,
        String description,
        BigDecimal price,
        BigDecimal costPrice,
        ServiceType type,
        ServiceStatus status,
        String inputSchema,
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
                service.getCostPrice(),
                service.getType(),
                service.getStatus(),
                service.getInputSchema(),
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
