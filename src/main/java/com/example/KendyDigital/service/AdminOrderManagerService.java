package com.example.KendyDigital.service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.KendyDigital.dto.OrderResponse;
import com.example.KendyDigital.model.OrderRecord;
import com.example.KendyDigital.model.OrderStatus;
import com.example.KendyDigital.repository.OrderRepository;

@Service
public class AdminOrderManagerService {
    private final OrderRepository orderRepository;

    public AdminOrderManagerService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders(OrderStatus status, Long userId, Instant fromDate, Instant toDate, int page, int size) {
        List<OrderRecord> orders = userId != null
                ? orderRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, paged(page, size))
                : status == null
                        ? orderRepository.findAllByOrderByCreatedAtDesc(paged(page, size))
                        : orderRepository.findAllByStatusOrderByCreatedAtDesc(status, paged(page, size));
        return orders.stream().map(OrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> searchOrders(String query, OrderStatus status, Long userId,
            Instant fromDate, Instant toDate, int page, int size) {
        String normalizedQuery = normalizeQuery(query);
        List<OrderRecord> orders = orderRepository.searchAdmin(
                likePattern(normalizedQuery),
                parseLongOrNull(normalizedQuery),
                status,
                userId,
                paged(page, size));
        return orders.stream().map(OrderResponse::from).toList();
    }

    private PageRequest paged(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 200)));
    }

    private String normalizeQuery(String query) {
        return query == null || query.isBlank() ? null : query.trim();
    }

    private String likePattern(String query) {
        return query == null ? null : "%" + query.toLowerCase(Locale.ROOT) + "%";
    }

    private Long parseLongOrNull(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
