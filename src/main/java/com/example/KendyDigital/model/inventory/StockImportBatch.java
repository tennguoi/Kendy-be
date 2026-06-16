package com.example.KendyDigital.model.inventory;

import com.example.KendyDigital.common.TimestampedEntity;
import com.example.KendyDigital.model.catalog.ServiceItem;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "stock_import_batches",
        indexes = {
                @Index(name = "idx_stock_import_batches_service_id", columnList = "service_id"),
                @Index(name = "idx_stock_import_batches_created_at", columnList = "created_at")
        })
public class StockImportBatch extends TimestampedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceItem service;

    @Column(name = "imported_by", nullable = false)
    private Long importedBy;

    @Column(nullable = false)
    private Integer quantity;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public StockImportBatch(ServiceItem service, Long importedBy, Integer quantity, String notes) {
        this.service = service;
        this.importedBy = importedBy;
        this.quantity = quantity;
        this.notes = notes;
    }
}
