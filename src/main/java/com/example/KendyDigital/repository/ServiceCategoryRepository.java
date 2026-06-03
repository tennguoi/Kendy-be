package com.example.KendyDigital.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.KendyDigital.model.ServiceCategory;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {
    boolean existsBySlug(String slug);

    Optional<ServiceCategory> findBySlug(String slug);

    List<ServiceCategory> findAllByOrderBySortOrderAscNameAsc();

    List<ServiceCategory> findAllByParentIsNullOrderBySortOrderAscNameAsc();
}
