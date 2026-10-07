package com.example.KendyDigital.repository.specification;

import com.example.KendyDigital.model.security.SecurityEvent;
import com.example.KendyDigital.model.security.SecurityEventType;
import com.example.KendyDigital.model.security.SecuritySeverity;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class SecurityEventSpecifications {

    private SecurityEventSpecifications() {}

    public static Specification<SecurityEvent> searchEvents(
            SecurityEventType type,
            SecuritySeverity severity,
            String ip,
            Long userId,
            Instant from,
            Instant to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (severity != null) {
                predicates.add(cb.equal(root.get("severity"), severity));
            }
            if (ip != null && !ip.isBlank()) {
                predicates.add(cb.equal(root.get("ip"), ip.trim()));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
