package com.example.KendyDigital.model.catalog;

import com.example.KendyDigital.common.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "services",
        indexes = {
                @Index(name = "idx_services_status", columnList = "status"),
                @Index(name = "idx_services_sort_order", columnList = "sort_order"),
                @Index(name = "idx_services_featured", columnList = "featured"),
                @Index(name = "idx_services_public_visible", columnList = "public_visible"),
                @Index(name = "idx_services_category_id", columnList = "category_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_services_slug", columnNames = "slug")
        })
public class ServiceItem extends TimestampedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String slug;

    @Column(name = "short_description", columnDefinition = "TEXT")
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Column(name = "price_text")
    private String priceText;

    @Column(name = "cost_price", precision = 18, scale = 2)
    private BigDecimal costPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ServiceType type = ServiceType.MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_strategy", length = 32)
    private AccessStrategy accessStrategy;

    @Column(name = "access_duration_days")
    private Integer accessDurationDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ServiceStatus status = ServiceStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_status", nullable = false, length = 32)
    private ServiceStockStatus stockStatus = ServiceStockStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "cta_type", nullable = false, length = 32)
    private ServiceCtaType ctaType = ServiceCtaType.BUY_NOW;

    @Column(name = "pricing_badge")
    private String pricingBadge;

    @Column(name = "featured", nullable = false)
    private boolean featured = false;

    @Column(name = "public_visible", nullable = false)
    private boolean publicVisible = true;

    @Column(name = "input_schema", columnDefinition = "TEXT")
    private String inputSchema;

    @Column(name = "requirements", columnDefinition = "TEXT")
    private String requirements;

    @Column(name = "benefits", columnDefinition = "TEXT")
    private String benefits;

    @Column(name = "usage_notes", columnDefinition = "TEXT")
    private String usageNotes;

    @Column(name = "processing_time")
    private String processingTime;

    @Column(name = "warranty_policy", columnDefinition = "TEXT")
    private String warrantyPolicy;

    @Column(name = "refund_policy", columnDefinition = "TEXT")
    private String refundPolicy;

    @Column(name = "non_warranty_cases", columnDefinition = "TEXT")
    private String nonWarrantyCases;

    @Column(name = "usage_rules", columnDefinition = "TEXT")
    private String usageRules;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 16)
    private RiskLevel riskLevel = RiskLevel.LOW;

    @Column(name = "warranty_days")
    private Integer warrantyDays;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "meta_title")
    private String metaTitle;

    @Column(name = "meta_description", columnDefinition = "TEXT")
    private String metaDescription;

    @Column(name = "icon_url")
    private String iconUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ServiceCategory category;

    public void updateCategory(ServiceCategory category) {
        this.category = category;
    }

    public void updateSeo(String metaTitle, String metaDescription, String iconUrl) {
        if (metaTitle != null) this.metaTitle = metaTitle;
        if (metaDescription != null) this.metaDescription = metaDescription;
        if (iconUrl != null) this.iconUrl = iconUrl;
    }

    public ServiceItem(String name, String slug, String shortDescription, String description, BigDecimal price,
            ServiceType type, ServiceStatus status) {
        this.name = name;
        this.slug = slug;
        this.shortDescription = shortDescription;
        this.description = description;
        this.price = price;
        this.type = type;
        this.status = status;
    }

    public void rename(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }

    public void updateDescriptions(String shortDescription, String description) {
        this.shortDescription = shortDescription;
        this.description = description;
    }

    public void updatePrice(BigDecimal price) {
        this.price = price;
    }

    public void updatePriceText(String priceText) {
        this.priceText = priceText;
    }

    public void updateCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public void updateType(ServiceType type) {
        this.type = type;
    }

    public AccessStrategy resolvedAccessStrategy() {
        if (accessStrategy != null) {
            return accessStrategy;
        }
        return type == ServiceType.ACCOUNT_STOCK
                ? AccessStrategy.DEDICATED_ACCOUNT
                : AccessStrategy.MANUAL;
    }

    public void updateAccessPolicy(AccessStrategy accessStrategy, Integer accessDurationDays) {
        this.accessStrategy = accessStrategy;
        this.accessDurationDays = accessDurationDays;
    }

    public void changeStatus(ServiceStatus status) {
        this.status = status;
    }

    public void updatePricingMetadata(ServiceStockStatus stockStatus, ServiceCtaType ctaType, String pricingBadge,
            boolean featured, boolean publicVisible) {
        this.stockStatus = stockStatus;
        this.ctaType = ctaType;
        this.pricingBadge = pricingBadge;
        this.featured = featured;
        this.publicVisible = publicVisible;
    }

    public void updateInputSchema(String inputSchema) {
        this.inputSchema = inputSchema;
    }

    public void updatePublicContent(String requirements, String benefits, String usageNotes) {
        this.requirements = requirements;
        this.benefits = benefits;
        this.usageNotes = usageNotes;
    }

    public void updateProcessingTime(String processingTime) {
        this.processingTime = processingTime;
    }

    public void updateWarrantyPolicy(String warrantyPolicy) {
        this.warrantyPolicy = warrantyPolicy;
    }

    public void updateRefundPolicy(String refundPolicy) {
        this.refundPolicy = refundPolicy;
    }

    public void updateNonWarrantyCases(String nonWarrantyCases) {
        this.nonWarrantyCases = nonWarrantyCases;
    }

    public void updateUsageRules(String usageRules) {
        this.usageRules = usageRules;
    }

    public void updateRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public void updateWarrantyDays(Integer warrantyDays) {
        this.warrantyDays = warrantyDays;
    }

    public void updateSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void deactivate() {
        this.status = ServiceStatus.INACTIVE;
    }
}
