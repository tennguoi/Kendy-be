package com.example.KendyDigital.model.content;

import com.example.KendyDigital.common.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
        name = "content_items",
        indexes = {
                @Index(name = "idx_content_items_type", columnList = "type"),
                @Index(name = "idx_content_items_published", columnList = "published"),
                @Index(name = "idx_content_items_sort_order", columnList = "sort_order")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_content_items_type_slug", columnNames = {"type", "slug"})
        })
public class ContentItem extends TimestampedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ContentType type;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "cta_url", columnDefinition = "TEXT")
    private String ctaUrl;

    @Column(name = "seo_title")
    private String seoTitle;

    @Column(name = "seo_description", columnDefinition = "TEXT")
    private String seoDescription;

    @Column(nullable = false)
    private boolean published = false;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "updated_by")
    private Long updatedBy;

    public ContentItem(ContentType type, String slug, String title, Long updatedBy) {
        this.type = type;
        this.slug = slug;
        this.title = title;
        this.updatedBy = updatedBy;
    }

    public void update(String slug, String title, String summary, String content, String imageUrl, String ctaUrl,
            String seoTitle, String seoDescription, boolean published, int sortOrder, Long updatedBy) {
        this.slug = slug;
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.imageUrl = imageUrl;
        this.ctaUrl = ctaUrl;
        this.seoTitle = seoTitle;
        this.seoDescription = seoDescription;
        this.published = published;
        this.sortOrder = sortOrder;
        this.updatedBy = updatedBy;
    }
}
