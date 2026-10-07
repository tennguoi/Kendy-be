package com.example.KendyDigital.repository.specification;

import com.example.KendyDigital.model.audit.AuditLog;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class AuditLogSpecifications {

    private AuditLogSpecifications() {
    }

    public static Specification<AuditLog> searchAdmin(
            String query,
            String action,
            Long actorUserId,
            String targetType,
            Long targetId) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (action != null && !action.isBlank()) {
                predicates.add(cb.equal(root.get("action"), action.trim()));
            }
            if (actorUserId != null) {
                predicates.add(cb.equal(root.get("actorUserId"), actorUserId));
            }
            if (targetType != null && !targetType.isBlank()) {
                predicates.add(cb.equal(root.get("targetType"), targetType.trim()));
            }
            if (targetId != null) {
                predicates.add(cb.equal(root.get("targetId"), targetId));
            }

            String pattern = SearchSpecificationUtils.likePattern(query);
            if (pattern != null) {
                List<Predicate> queryPredicates = new ArrayList<>();
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("action"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("targetType"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("metadata"), pattern));
                predicates.add(cb.or(queryPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
