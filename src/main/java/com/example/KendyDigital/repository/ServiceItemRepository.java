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

    List<ServiceItem> findAllByOrderBySortOrderAscNameAsc();

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
}
