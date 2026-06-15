package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.catalog.ServiceCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {
    boolean existsBySlug(String slug);

    Optional<ServiceCategory> findBySlug(String slug);

    List<ServiceCategory> findAllByOrderBySortOrderAscNameAsc();

    List<ServiceCategory> findAllByParentIsNullOrderBySortOrderAscNameAsc();
}
