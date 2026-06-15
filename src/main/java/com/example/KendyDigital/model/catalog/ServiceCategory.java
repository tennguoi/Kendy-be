package com.example.KendyDigital.model.catalog;

import com.example.KendyDigital.common.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "service_categories",
        indexes = {
                @Index(name = "idx_service_categories_sort_order", columnList = "sort_order")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_service_categories_slug", columnNames = "slug")
        })
public class ServiceCategory extends TimestampedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ServiceCategory parent;

    public ServiceCategory(String name, String slug, String description, int sortOrder) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.sortOrder = sortOrder;
    }
}
