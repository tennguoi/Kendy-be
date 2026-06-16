package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.content.ContentItem;
import com.example.KendyDigital.model.content.ContentType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentItemRepository extends JpaRepository<ContentItem, Long> {
    Optional<ContentItem> findByTypeAndSlug(ContentType type, String slug);

    List<ContentItem> findAllByTypeAndPublishedOrderBySortOrderAscCreatedAtDesc(ContentType type, boolean published);

    @Query("""
            select c from ContentItem c
            where (:type is null or c.type = :type)
              and (:published is null or c.published = :published)
              and (
                :queryPattern is null
                or lower(c.slug) like :queryPattern
                or lower(c.title) like :queryPattern
                or lower(coalesce(c.summary, '')) like :queryPattern
              )
            order by c.type asc, c.sortOrder asc, c.createdAt desc
            """)
    List<ContentItem> search(@Param("type") ContentType type, @Param("published") Boolean published,
            @Param("queryPattern") String queryPattern, Pageable pageable);
}
