package com.example.KendyDigital.repository.specification;

import com.example.KendyDigital.model.notification.EmailLog;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class EmailLogSpecifications {

    private EmailLogSpecifications() {
    }

    public static Specification<EmailLog> searchLogs(String query, String status) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status.trim()));
            }

            String pattern = SearchSpecificationUtils.likePattern(query);
            if (pattern != null) {
                List<Predicate> queryPredicates = new ArrayList<>();
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("toEmail"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("subject"), pattern));
                predicates.add(cb.or(queryPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
