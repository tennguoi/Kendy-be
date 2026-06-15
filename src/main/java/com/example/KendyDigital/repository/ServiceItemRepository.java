package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.catalog.ServiceItem;
import com.example.KendyDigital.model.catalog.ServiceStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {
    boolean existsBySlug(String slug);

    Optional<ServiceItem> findBySlug(String slug);

    List<ServiceItem> findByStatusOrderBySortOrderAscNameAsc(ServiceStatus status);

    List<ServiceItem> findByStatusOrderBySortOrderAscNameAsc(ServiceStatus status, Pageable pageable);

    List<ServiceItem> findByStatusAndPublicVisibleTrueOrderBySortOrderAscNameAsc(ServiceStatus status,
            Pageable pageable);

    List<ServiceItem> findByStatusAndCategory_IdOrderBySortOrderAscNameAsc(ServiceStatus status, Long categoryId,
            Pageable pageable);

    List<ServiceItem> findByStatusAndPublicVisibleTrueAndCategory_IdOrderBySortOrderAscNameAsc(ServiceStatus status,
            Long categoryId, Pageable pageable);

    List<ServiceItem> findByStatusAndPublicVisibleTrueAndFeaturedOrderBySortOrderAscNameAsc(ServiceStatus status,
            boolean featured, Pageable pageable);

    List<ServiceItem> findAllByOrderBySortOrderAscNameAsc();

    List<ServiceItem> findAllByOrderBySortOrderAscNameAsc(Pageable pageable);

    @Query("""
            select s from ServiceItem s
            where (:status is null or s.status = :status)
              and (:categoryId is null or s.category.id = :categoryId)
              and (:categorySlug is null or s.category.slug = :categorySlug)
              and (:featured is null or s.featured = :featured)
              and (
                :queryPattern is null
                or lower(s.name) like :queryPattern
                or lower(s.slug) like :queryPattern
                or lower(coalesce(s.shortDescription, '')) like :queryPattern
                or (:exactId is not null and s.id = :exactId)
              )
            order by s.sortOrder asc, s.name asc
            """)
    List<ServiceItem> searchAdmin(@Param("queryPattern") String queryPattern, @Param("exactId") Long exactId,
            @Param("status") ServiceStatus status, @Param("categoryId") Long categoryId,
            @Param("categorySlug") String categorySlug, @Param("featured") Boolean featured, Pageable pageable);

    @Query("""
            select s from ServiceItem s
            where s.status = com.example.KendyDigital.model.catalog.ServiceStatus.ACTIVE
              and s.publicVisible = true
              and (:categoryId is null or s.category.id = :categoryId)
              and (:categorySlug is null or s.category.slug = :categorySlug)
              and (:featured is null or s.featured = :featured)
              and (
                :queryPattern is null
                or lower(s.name) like :queryPattern
                or lower(s.slug) like :queryPattern
                or lower(coalesce(s.shortDescription, '')) like :queryPattern
                or lower(coalesce(s.description, '')) like :queryPattern
                or (:exactId is not null and s.id = :exactId)
              )
            order by s.sortOrder asc, s.name asc
            """)
    List<ServiceItem> searchPublic(@Param("queryPattern") String queryPattern, @Param("exactId") Long exactId,
            @Param("categoryId") Long categoryId, @Param("categorySlug") String categorySlug,
            @Param("featured") Boolean featured, Pageable pageable);
}
