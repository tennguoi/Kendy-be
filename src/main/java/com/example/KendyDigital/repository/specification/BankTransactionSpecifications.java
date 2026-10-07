package com.example.KendyDigital.repository.specification;

import com.example.KendyDigital.model.bank.BankTransaction;
import com.example.KendyDigital.model.bank.BankTransactionStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class BankTransactionSpecifications {

    private BankTransactionSpecifications() {
    }

    public static Specification<BankTransaction> searchAdmin(
            String query,
            BankTransactionStatus status) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            String pattern = SearchSpecificationUtils.likePattern(query);
            if (pattern != null) {
                Long exactId = SearchSpecificationUtils.parseLongOrNull(query);

                List<Predicate> queryPredicates = new ArrayList<>();
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("referenceCode"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("code"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("content"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("accountNumber"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("gateway"), pattern));
                if (exactId != null) {
                    queryPredicates.add(cb.equal(root.get("id"), exactId));
                    queryPredicates.add(cb.equal(root.get("sepayId"), exactId));
                }
                predicates.add(cb.or(queryPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
