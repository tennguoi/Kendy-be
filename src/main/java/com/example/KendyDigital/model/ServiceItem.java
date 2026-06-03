package com.example.KendyDigital.model;

import java.math.BigDecimal;

import com.example.KendyDigital.common.TimestampedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
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

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "services",
        indexes = {
                @Index(name = "idx_services_status", columnList = "status"),
                @Index(name = "idx_services_sort_order", columnList = "sort_order")
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

    @Column(name = "cost_price", precision = 18, scale = 2)
    private BigDecimal costPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ServiceType type = ServiceType.MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ServiceStatus status = ServiceStatus.ACTIVE;

    @Column(name = "input_schema", columnDefinition = "TEXT")
    private String inputSchema;

    @Column(name = "processing_time")
    private String processingTime;

    @Column(name = "warranty_policy", columnDefinition = "TEXT")
    private String warrantyPolicy;

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

    public void updateCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public void updateType(ServiceType type) {
        this.type = type;
    }

    public void changeStatus(ServiceStatus status) {
        this.status = status;
    }

    public void updateInputSchema(String inputSchema) {
        this.inputSchema = inputSchema;
    }

    public void updateProcessingTime(String processingTime) {
        this.processingTime = processingTime;
    }

    public void updateWarrantyPolicy(String warrantyPolicy) {
        this.warrantyPolicy = warrantyPolicy;
    }

    public void updateSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void deactivate() {
        this.status = ServiceStatus.INACTIVE;
    }
}
