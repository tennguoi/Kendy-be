package com.example.KendyDigital.repository.specification;

import com.example.KendyDigital.model.catalog.ServiceItem;
import com.example.KendyDigital.model.inventory.AccountCredential;
import com.example.KendyDigital.model.inventory.AccountCredentialStatus;
import com.example.KendyDigital.model.order.OrderRecord;
import com.example.KendyDigital.model.user.UserAccount;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class AccountCredentialSpecifications {

    private AccountCredentialSpecifications() {
    }

    public static Specification<AccountCredential> searchForAdmin(
            Long serviceId,
            AccountCredentialStatus status,
            String query,
            Instant createdFrom,
            Instant createdTo,
            Instant deliveredFrom,
            Instant deliveredTo,
            Instant expiresBefore) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("service").get("id"), serviceId));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (createdFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            }
            if (createdTo != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), createdTo));
            }
            if (deliveredFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("deliveredAt"), deliveredFrom));
            }
            if (deliveredTo != null) {
                predicates.add(cb.lessThan(root.get("deliveredAt"), deliveredTo));
            }
            if (expiresBefore != null) {
                predicates.add(cb.lessThan(root.get("expiresAt"), expiresBefore));
            }

            String pattern = SearchSpecificationUtils.likePattern(query);
            if (pattern != null) {
                List<Predicate> queryPredicates = new ArrayList<>();
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("loginIdentifier"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("internalNote"), pattern));
                predicates.add(cb.or(queryPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<AccountCredential> searchAssignedForAdmin(
            AccountCredentialStatus status,
            String query,
            Instant deliveredFrom,
            Instant deliveredTo,
            Instant expiresBefore) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isNotNull(root.get("deliveredToUser")));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (deliveredFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("deliveredAt"), deliveredFrom));
            }
            if (deliveredTo != null) {
                predicates.add(cb.lessThan(root.get("deliveredAt"), deliveredTo));
            }
            if (expiresBefore != null) {
                predicates.add(cb.lessThan(root.get("expiresAt"), expiresBefore));
            }

            String pattern = SearchSpecificationUtils.likePattern(query);
            if (pattern != null) {
                Join<AccountCredential, ServiceItem> serviceJoin = root.join("service", JoinType.LEFT);
                Join<AccountCredential, UserAccount> userJoin = root.join("deliveredToUser", JoinType.LEFT);
                Join<AccountCredential, OrderRecord> orderJoin = root.join("assignedOrder", JoinType.LEFT);

                List<Predicate> queryPredicates = new ArrayList<>();
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("loginIdentifier"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, serviceJoin.get("name"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, userJoin.get("name"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, userJoin.get("email"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, userJoin.get("phone"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, orderJoin.get("orderCode"), pattern));
                predicates.add(cb.or(queryPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
