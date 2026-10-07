package com.example.KendyDigital.repository.specification;

import com.example.KendyDigital.model.deposit.DepositRequest;
import com.example.KendyDigital.model.deposit.DepositStatus;
import com.example.KendyDigital.model.user.UserAccount;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class DepositRequestSpecifications {

    private DepositRequestSpecifications() {
    }

    public static Specification<DepositRequest> searchAdmin(
            String query,
            DepositStatus status,
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
                Join<DepositRequest, UserAccount> userJoin = root.join("user", JoinType.LEFT);

                List<Predicate> queryPredicates = new ArrayList<>();
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("depositCode"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("transferContent"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("bankAccount"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, userJoin.get("email"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, userJoin.get("name"), pattern));
                if (exactId != null) {
                    queryPredicates.add(cb.equal(root.get("id"), exactId));
                }
                predicates.add(cb.or(queryPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
