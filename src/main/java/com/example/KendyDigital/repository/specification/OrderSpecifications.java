package com.example.KendyDigital.repository.specification;

import com.example.KendyDigital.model.catalog.ServiceItem;
import com.example.KendyDigital.model.order.OrderRecord;
import com.example.KendyDigital.model.order.OrderStatus;
import com.example.KendyDigital.model.user.UserAccount;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class OrderSpecifications {

    private OrderSpecifications() {
    }

    public static Specification<OrderRecord> searchUser(Long userId, String query, OrderStatus status) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("user").get("id"), userId));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            String pattern = SearchSpecificationUtils.likePattern(query);
            if (pattern != null) {
                Long exactId = SearchSpecificationUtils.parseLongOrNull(query);
                Join<OrderRecord, ServiceItem> serviceJoin = root.join("service", JoinType.LEFT);

                List<Predicate> queryPredicates = new ArrayList<>();
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("orderCode"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, serviceJoin.get("name"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, serviceJoin.get("slug"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("inputData"), pattern));
                if (exactId != null) {
                    queryPredicates.add(cb.equal(root.get("id"), exactId));
                }
                predicates.add(cb.or(queryPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<OrderRecord> searchAdmin(
            String query,
            OrderStatus status,
            Long userId,
            Instant fromDate,
            Instant toDate) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), toDate));
            }

            String pattern = SearchSpecificationUtils.likePattern(query);
            if (pattern != null) {
                Long exactId = SearchSpecificationUtils.parseLongOrNull(query);
                Join<OrderRecord, UserAccount> userJoin = root.join("user", JoinType.LEFT);
                Join<OrderRecord, ServiceItem> serviceJoin = root.join("service", JoinType.LEFT);

                List<Predicate> queryPredicates = new ArrayList<>();
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("orderCode"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, userJoin.get("email"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, userJoin.get("name"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, serviceJoin.get("name"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, serviceJoin.get("slug"), pattern));
                if (exactId != null) {
                    queryPredicates.add(cb.equal(root.get("id"), exactId));
                }
                predicates.add(cb.or(queryPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
