package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.inventory.StockImportBatch;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockImportBatchRepository extends JpaRepository<StockImportBatch, Long> {
    @Query("select b from StockImportBatch b join fetch b.service where b.service.id = :serviceId order by b.createdAt desc")
    List<StockImportBatch> findAllByService_IdOrderByCreatedAtDesc(@Param("serviceId") Long serviceId, Pageable pageable);

    @Query("select b from StockImportBatch b join fetch b.service order by b.createdAt desc")
    List<StockImportBatch> findAllOrderByCreatedAtDesc(Pageable pageable);
}
