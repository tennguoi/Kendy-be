package com.example.KendyDigital.dto.order.response;

import com.example.KendyDigital.dto.inventory.response.AccountCredentialDeliveryResponse;
import com.example.KendyDigital.model.order.ManualWorkflowStatus;
import com.example.KendyDigital.model.order.OrderRecord;
import com.example.KendyDigital.model.order.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderCode,
        UUID userId,
        UUID serviceId,
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
        UUID assignedAdminId,
        String assignedAdminName,
        String assignedAdminEmail,
        ManualWorkflowStatus manualWorkflowStatus,
        String manualChecklist,
        List<ManualOrderTaskResponse> manualTasks,
        Instant deadlineReminderSentAt,
        Instant deadlineOverdueNotifiedAt,
        UUID supportTicketId,
        Instant completedAt,
        Instant cancelledAt,
        Instant createdAt) {
    public static OrderResponse from(OrderRecord order) {
        return from(order, List.of());
    }

    public static OrderResponse from(OrderRecord order, List<ManualOrderTaskResponse> manualTasks) {
        return new OrderResponse(
                order.getPublicId(),
                order.getOrderCode(),
                order.getUser().getPublicId(),
                order.getService().getPublicId(),
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
                order.getAssignedAdmin() == null ? null : order.getAssignedAdmin().getPublicId(),
                order.getAssignedAdmin() == null ? null : order.getAssignedAdmin().getName(),
                order.getAssignedAdmin() == null ? null : order.getAssignedAdmin().getEmail(),
                order.getManualWorkflowStatus(),
                order.getManualChecklist(),
                manualTasks == null ? List.of() : manualTasks,
                order.getDeadlineReminderSentAt(),
                order.getDeadlineOverdueNotifiedAt(),
                order.getSupportTicket() == null ? null : order.getSupportTicket().getPublicId(),
                order.getCompletedAt(),
                order.getCancelledAt(),
                order.getCreatedAt());
    }
}
