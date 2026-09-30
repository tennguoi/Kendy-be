package com.example.KendyDigital.dto.catalog.request;

import com.example.KendyDigital.model.catalog.ServiceCtaType;
import com.example.KendyDigital.model.catalog.AccessStrategy;
import com.example.KendyDigital.model.catalog.ServiceStatus;
import com.example.KendyDigital.model.catalog.ServiceStockStatus;
import com.example.KendyDigital.model.catalog.ServiceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateServiceRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 150) @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug chỉ chứa chữ thường, số và dấu gạch ngang") String slug,
        @Size(max = 500) String shortDescription,
        @Size(max = 10000) String description,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @Size(max = 50) String priceText,
        @DecimalMin("0.00") BigDecimal costPrice,
        @NotNull ServiceType type,
        AccessStrategy accessStrategy,
        @Min(1) Integer accessDurationDays,
        @NotNull ServiceStatus status,
        @NotNull ServiceStockStatus stockStatus,
        @NotNull ServiceCtaType ctaType,
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
        @Size(max = 200) String metaTitle,
        @Size(max = 1000) String metaDescription,
        @Size(max = 500) String iconUrl) {
}
