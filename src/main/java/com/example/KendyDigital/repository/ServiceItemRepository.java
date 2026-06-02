package com.example.KendyDigital.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.KendyDigital.model.ServiceItem;
import com.example.KendyDigital.model.ServiceStatus;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {
    boolean existsBySlug(String slug);

    Optional<ServiceItem> findBySlug(String slug);

    List<ServiceItem> findByStatusOrderBySortOrderAscNameAsc(ServiceStatus status);

    List<ServiceItem> findAllByOrderBySortOrderAscNameAsc();
}
