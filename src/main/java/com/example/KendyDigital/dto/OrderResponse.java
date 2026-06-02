package com.example.KendyDigital.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.KendyDigital.model.OrderRecord;
import com.example.KendyDigital.model.OrderStatus;

public record OrderResponse(
        Long id,
        String orderCode,
        Long userId,
        Long serviceId,
        String serviceName,
        BigDecimal amount,
        String inputData,
        String resultData,
        OrderStatus status,
        String idempotencyKey,
        String adminNote,
        String userNote,
        Instant processingAt,
        Instant completedAt,
        Instant cancelledAt,
        Instant createdAt) {
    public static OrderResponse from(OrderRecord order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderCode(),
                order.getUser().getId(),
                order.getService().getId(),
                order.getService().getName(),
                order.getAmount(),
                order.getInputData(),
                order.getResultData(),
                order.getStatus(),
                order.getIdempotencyKey(),
                order.getAdminNote(),
                order.getUserNote(),
                order.getProcessingAt(),
                order.getCompletedAt(),
                order.getCancelledAt(),
                order.getCreatedAt());
    }
}
