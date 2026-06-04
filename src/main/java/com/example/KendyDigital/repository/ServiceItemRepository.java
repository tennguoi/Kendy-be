package com.example.KendyDigital.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.KendyDigital.model.ServiceItem;
import com.example.KendyDigital.model.ServiceStatus;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {
    boolean existsBySlug(String slug);

    Optional<ServiceItem> findBySlug(String slug);

    List<ServiceItem> findByStatusOrderBySortOrderAscNameAsc(ServiceStatus status);

    List<ServiceItem> findByStatusOrderBySortOrderAscNameAsc(ServiceStatus status, Pageable pageable);

    List<ServiceItem> findByStatusAndCategory_IdOrderBySortOrderAscNameAsc(ServiceStatus status, Long categoryId,
            Pageable pageable);

    List<ServiceItem> findAllByOrderBySortOrderAscNameAsc();

    List<ServiceItem> findAllByOrderBySortOrderAscNameAsc(Pageable pageable);

    @Query("""
            select s from ServiceItem s
            where (:status is null or s.status = :status)
              and (
                :query is null
                or lower(s.name) like lower(concat('%', :query, '%'))
                or lower(s.slug) like lower(concat('%', :query, '%'))
                or lower(coalesce(s.shortDescription, '')) like lower(concat('%', :query, '%'))
                or (:exactId is not null and s.id = :exactId)
              )
            order by s.sortOrder asc, s.name asc
            """)
    List<ServiceItem> searchAdmin(@Param("query") String query, @Param("exactId") Long exactId,
            @Param("status") ServiceStatus status, Pageable pageable);

    @Query("""
            select s from ServiceItem s
            where s.status = com.example.KendyDigital.model.ServiceStatus.ACTIVE
              and (:categoryId is null or s.category.id = :categoryId)
              and (
                :query is null
                or lower(s.name) like lower(concat('%', :query, '%'))
                or lower(s.slug) like lower(concat('%', :query, '%'))
                or lower(coalesce(s.shortDescription, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(s.description, '')) like lower(concat('%', :query, '%'))
                or (:exactId is not null and s.id = :exactId)
              )
            order by s.sortOrder asc, s.name asc
            """)
    List<ServiceItem> searchPublic(@Param("query") String query, @Param("exactId") Long exactId,
            @Param("categoryId") Long categoryId, Pageable pageable);
}
