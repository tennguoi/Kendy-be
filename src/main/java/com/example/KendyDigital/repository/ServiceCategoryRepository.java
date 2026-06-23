package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.catalog.ServiceCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {
    boolean existsBySlug(String slug);

    @EntityGraph(attributePaths = "parent")
    Optional<ServiceCategory> findBySlug(String slug);

    @EntityGraph(attributePaths = "parent")
    List<ServiceCategory> findAllByOrderBySortOrderAscNameAsc();

    @EntityGraph(attributePaths = "parent")
    List<ServiceCategory> findAllByOrderBySortOrderAscNameAsc(Pageable pageable);

    @EntityGraph(attributePaths = "parent")
    List<ServiceCategory> findAllByParentIsNullOrderBySortOrderAscNameAsc();

    boolean existsByParent_Id(Long parentId);
}
