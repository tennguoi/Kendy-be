package com.example.KendyDigital.repository.specification;

import com.example.KendyDigital.model.catalog.ServiceCategory;
import com.example.KendyDigital.model.catalog.ServiceItem;
import com.example.KendyDigital.model.catalog.ServiceStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class ServiceItemSpecifications {

    private ServiceItemSpecifications() {
    }

    public static Specification<ServiceItem> searchPublic(
            String query,
            Long categoryId,
            String categorySlug,
            Boolean featured) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("status"), ServiceStatus.ACTIVE));
            predicates.add(cb.isTrue(root.get("publicVisible")));

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (categorySlug != null && !categorySlug.isBlank()) {
                predicates.add(cb.equal(root.get("category").get("slug"), categorySlug.trim()));
            }
            if (featured != null) {
                predicates.add(cb.equal(root.get("featured"), featured));
            }

            String pattern = SearchSpecificationUtils.likePattern(query);
            if (pattern != null) {
                Long exactId = SearchSpecificationUtils.parseLongOrNull(query);
                List<Predicate> queryPredicates = new ArrayList<>();
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("name"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("slug"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("shortDescription"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("description"), pattern));
                if (exactId != null) {
                    queryPredicates.add(cb.equal(root.get("id"), exactId));
                }
                predicates.add(cb.or(queryPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<ServiceItem> searchAdmin(
            String query,
            ServiceStatus status,
            Long categoryId,
            String categorySlug,
            Boolean featured) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (categorySlug != null && !categorySlug.isBlank()) {
                predicates.add(cb.equal(root.get("category").get("slug"), categorySlug.trim()));
            }
            if (featured != null) {
                predicates.add(cb.equal(root.get("featured"), featured));
            }

            String pattern = SearchSpecificationUtils.likePattern(query);
            if (pattern != null) {
                Long exactId = SearchSpecificationUtils.parseLongOrNull(query);
                List<Predicate> queryPredicates = new ArrayList<>();
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("name"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("slug"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("shortDescription"), pattern));
                if (exactId != null) {
                    queryPredicates.add(cb.equal(root.get("id"), exactId));
                }
                predicates.add(cb.or(queryPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
