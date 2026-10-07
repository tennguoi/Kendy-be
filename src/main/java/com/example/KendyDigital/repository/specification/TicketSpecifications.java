package com.example.KendyDigital.repository.specification;

import com.example.KendyDigital.model.deposit.DepositRequest;
import com.example.KendyDigital.model.order.OrderRecord;
import com.example.KendyDigital.model.ticket.Ticket;
import com.example.KendyDigital.model.ticket.TicketCategory;
import com.example.KendyDigital.model.ticket.TicketPriority;
import com.example.KendyDigital.model.ticket.TicketStatus;
import com.example.KendyDigital.model.user.UserAccount;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class TicketSpecifications {

    private TicketSpecifications() {
    }

    public static Specification<Ticket> searchUser(
            Long userId,
            String query,
            TicketStatus status,
            TicketCategory category,
            TicketPriority priority) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("user").get("id"), userId));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }

            String pattern = SearchSpecificationUtils.likePattern(query);
            if (pattern != null) {
                Long exactId = SearchSpecificationUtils.parseLongOrNull(query);
                Join<Ticket, OrderRecord> orderJoin = root.join("order", JoinType.LEFT);
                Join<Ticket, DepositRequest> depositJoin = root.join("depositRequest", JoinType.LEFT);

                List<Predicate> queryPredicates = new ArrayList<>();
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("ticketCode"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("subject"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, orderJoin.get("orderCode"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, depositJoin.get("depositCode"), pattern));
                if (exactId != null) {
                    queryPredicates.add(cb.equal(root.get("id"), exactId));
                }
                predicates.add(cb.or(queryPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Ticket> searchAdmin(
            String query,
            TicketStatus status,
            TicketCategory category,
            TicketPriority priority,
            Long userId) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }

            String pattern = SearchSpecificationUtils.likePattern(query);
            if (pattern != null) {
                Long exactId = SearchSpecificationUtils.parseLongOrNull(query);
                Join<Ticket, UserAccount> userJoin = root.join("user", JoinType.LEFT);
                Join<Ticket, OrderRecord> orderJoin = root.join("order", JoinType.LEFT);
                Join<Ticket, DepositRequest> depositJoin = root.join("depositRequest", JoinType.LEFT);

                List<Predicate> queryPredicates = new ArrayList<>();
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("ticketCode"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, root.get("subject"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, userJoin.get("email"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, userJoin.get("name"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, orderJoin.get("orderCode"), pattern));
                queryPredicates.add(SearchSpecificationUtils.likeLower(cb, depositJoin.get("depositCode"), pattern));
                if (exactId != null) {
                    queryPredicates.add(cb.equal(root.get("id"), exactId));
                }
                predicates.add(cb.or(queryPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
