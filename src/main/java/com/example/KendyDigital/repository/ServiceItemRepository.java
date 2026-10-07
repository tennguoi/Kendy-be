package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.catalog.ServiceItem;
import com.example.KendyDigital.model.catalog.ServiceStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long>, JpaSpecificationExecutor<ServiceItem> {
        @Override
        @EntityGraph(attributePaths = "category")
        Page<ServiceItem> findAll(Specification<ServiceItem> spec, Pageable pageable);

        @Override
        @EntityGraph(attributePaths = "category")
        List<ServiceItem> findAll(Specification<ServiceItem> spec);

        boolean existsBySlug(String slug);

        @EntityGraph(attributePaths = "category")
        Optional<ServiceItem> findBySlug(String slug);

        @EntityGraph(attributePaths = "category")
        List<ServiceItem> findByStatusOrderBySortOrderAscNameAsc(ServiceStatus status);

        @EntityGraph(attributePaths = "category")
        List<ServiceItem> findByStatusOrderBySortOrderAscNameAsc(ServiceStatus status, Pageable pageable);

        @EntityGraph(attributePaths = "category")
        List<ServiceItem> findByStatusAndPublicVisibleTrueOrderBySortOrderAscNameAsc(ServiceStatus status,
                        Pageable pageable);

        @EntityGraph(attributePaths = "category")
        List<ServiceItem> findByStatusAndCategory_IdOrderBySortOrderAscNameAsc(ServiceStatus status, Long categoryId,
                        Pageable pageable);

        @EntityGraph(attributePaths = "category")
        List<ServiceItem> findByStatusAndPublicVisibleTrueAndCategory_IdOrderBySortOrderAscNameAsc(ServiceStatus status,
                        Long categoryId, Pageable pageable);

        @EntityGraph(attributePaths = "category")
        List<ServiceItem> findByStatusAndPublicVisibleTrueAndFeaturedOrderBySortOrderAscNameAsc(ServiceStatus status,
                        boolean featured, Pageable pageable);

        @EntityGraph(attributePaths = "category")
        List<ServiceItem> findAllByOrderBySortOrderAscNameAsc();

        @EntityGraph(attributePaths = "category")
        List<ServiceItem> findAllByOrderBySortOrderAscNameAsc(Pageable pageable);

        boolean existsByCategory_Id(Long categoryId);

}
