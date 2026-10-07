package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.content.ContentItem;
import com.example.KendyDigital.model.content.ContentType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ContentItemRepository extends JpaRepository<ContentItem, Long>, JpaSpecificationExecutor<ContentItem> {
    Optional<ContentItem> findByTypeAndSlug(ContentType type, String slug);

    List<ContentItem> findAllByTypeAndPublishedOrderBySortOrderAscCreatedAtDesc(ContentType type, boolean published);
}

