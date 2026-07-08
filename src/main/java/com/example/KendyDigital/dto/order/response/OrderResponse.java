package com.example.KendyDigital.dto.order.response;

import com.example.KendyDigital.dto.inventory.response.AccountCredentialDeliveryResponse;
import com.example.KendyDigital.model.order.ManualWorkflowStatus;
import com.example.KendyDigital.model.order.OrderRecord;
import com.example.KendyDigital.model.order.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderCode,
        Long userId,
        Long serviceId,
        String serviceName,
        String serviceType,
        BigDecimal amount,
        BigDecimal originalAmount,
        BigDecimal discountAmount,
        String couponCode,
        String inputData,
        String resultData,
        OrderStatus status,
        String idempotencyKey,
        String adminNote,
        String userNote,
        AccountCredentialDeliveryResponse delivery,
        Instant processingAt,
        Instant processingDeadlineAt,
        Long assignedAdminId,
        String assignedAdminName,
        String assignedAdminEmail,
        ManualWorkflowStatus manualWorkflowStatus,
        String manualChecklist,
        List<ManualOrderTaskResponse> manualTasks,
        Instant deadlineReminderSentAt,
        Instant deadlineOverdueNotifiedAt,
        Long supportTicketId,
        Instant completedAt,
        Instant cancelledAt,
        Instant createdAt) {
    public static OrderResponse from(OrderRecord order) {
        return from(order, List.of());
    }

    public static OrderResponse from(OrderRecord order, List<ManualOrderTaskResponse> manualTasks) {
        return new OrderResponse(
                order.getId(),
                order.getOrderCode(),
                order.getUser().getId(),
                order.getService().getId(),
                order.getService().getName(),
                order.getService().getType().name(),
                order.getAmount(),
                order.getOriginalAmount(),
                order.getDiscountAmount(),
                order.getCouponCode(),
                order.getInputData(),
                order.getResultData(),
                order.getStatus(),
                order.getIdempotencyKey(),
                order.getAdminNote(),
                order.getUserNote(),
                AccountCredentialDeliveryResponse.from(order.getDeliveredCredential()),
                order.getProcessingAt(),
                order.getProcessingDeadlineAt(),
                order.getAssignedAdmin() == null ? null : order.getAssignedAdmin().getId(),
                order.getAssignedAdmin() == null ? null : order.getAssignedAdmin().getName(),
                order.getAssignedAdmin() == null ? null : order.getAssignedAdmin().getEmail(),
                order.getManualWorkflowStatus(),
                order.getManualChecklist(),
                manualTasks == null ? List.of() : manualTasks,
                order.getDeadlineReminderSentAt(),
                order.getDeadlineOverdueNotifiedAt(),
                order.getSupportTicket() == null ? null : order.getSupportTicket().getId(),
                order.getCompletedAt(),
                order.getCancelledAt(),
                order.getCreatedAt());
    }
}
