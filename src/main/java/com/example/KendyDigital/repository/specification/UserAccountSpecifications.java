package com.example.KendyDigital.repository.specification;

import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class UserAccountSpecifications {

    private UserAccountSpecifications() {
    }

    public static Specification<UserAccount> searchAdmin(String query, UserStatus status) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            String pattern = SearchSpecificationUtils.likePattern(query);
            if (pattern != null) {
                Long exactId = SearchSpecificationUtils.parseLongOrNull(query);
                List<Predicate> queryPredicates = new ArrayList<>();
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("name"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("email"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("phone"), pattern));
                if (exactId != null) {
                    queryPredicates.add(cb.equal(root.get("id"), exactId));
                }
                predicates.add(cb.or(queryPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
