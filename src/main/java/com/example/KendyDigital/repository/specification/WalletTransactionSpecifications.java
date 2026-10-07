package com.example.KendyDigital.repository.specification;

import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.wallet.WalletTransaction;
import com.example.KendyDigital.model.wallet.WalletTransactionDirection;
import com.example.KendyDigital.model.wallet.WalletTransactionType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class WalletTransactionSpecifications {

    private WalletTransactionSpecifications() {
    }

    public static Specification<WalletTransaction> searchUser(
            Long userId,
            String query,
            WalletTransactionType type,
            WalletTransactionDirection direction) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("user").get("id"), userId));

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (direction != null) {
                predicates.add(cb.equal(root.get("direction"), direction));
            }

            String pattern = SearchSpecificationUtils.likePattern(query);
            if (pattern != null) {
                Long exactId = SearchSpecificationUtils.parseLongOrNull(query);

                List<Predicate> queryPredicates = new ArrayList<>();
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("transactionCode"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("referenceType"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("description"), pattern));
                if (exactId != null) {
                    queryPredicates.add(cb.equal(root.get("id"), exactId));
                    queryPredicates.add(cb.equal(root.get("referenceId"), exactId));
                }
                predicates.add(cb.or(queryPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<WalletTransaction> searchAdmin(
            String query,
            Long userId,
            WalletTransactionType type,
            WalletTransactionDirection direction) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (direction != null) {
                predicates.add(cb.equal(root.get("direction"), direction));
            }

            String pattern = SearchSpecificationUtils.likePattern(query);
            if (pattern != null) {
                Long exactId = SearchSpecificationUtils.parseLongOrNull(query);
                Join<WalletTransaction, UserAccount> userJoin = root.join("user", JoinType.LEFT);

                List<Predicate> queryPredicates = new ArrayList<>();
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("transactionCode"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("referenceType"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("description"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, userJoin.get("email"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, userJoin.get("name"), pattern));
                if (exactId != null) {
                    queryPredicates.add(cb.equal(root.get("id"), exactId));
                    queryPredicates.add(cb.equal(root.get("referenceId"), exactId));
                }
                predicates.add(cb.or(queryPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
