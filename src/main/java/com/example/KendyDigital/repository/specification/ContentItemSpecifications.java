package com.example.KendyDigital.repository.specification;

import com.example.KendyDigital.model.content.ContentItem;
import com.example.KendyDigital.model.content.ContentType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class ContentItemSpecifications {

    private ContentItemSpecifications() {
    }

    public static Specification<ContentItem> withFilter(
            ContentType type,
            Boolean published,
            String query) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (published != null) {
                predicates.add(cb.equal(root.get("published"), published));
            }

            String pattern = SearchSpecificationUtils.likePattern(query);
            if (pattern != null) {
                List<Predicate> queryPredicates = new ArrayList<>();
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("slug"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("title"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("summary"), pattern));
                predicates.add(cb.or(queryPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
