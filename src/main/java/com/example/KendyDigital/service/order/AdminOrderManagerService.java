package com.example.KendyDigital.service.order;

import com.example.KendyDigital.dto.order.response.OrderResponse;
import com.example.KendyDigital.model.order.OrderStatus;
import java.time.Instant;
import java.util.List;

public interface AdminOrderManagerService {
    List<OrderResponse> listOrders(OrderStatus status, Long userId, Instant fromDate, Instant toDate, int page, int size);
    List<OrderResponse> searchOrders(String query, OrderStatus status, Long userId, Instant fromDate, Instant toDate, int page, int size);
}
