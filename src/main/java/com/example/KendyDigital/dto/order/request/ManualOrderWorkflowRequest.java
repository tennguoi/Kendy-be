package com.example.KendyDigital.dto.order.request;

import java.time.Instant;

public record ManualOrderWorkflowRequest(
        Long assignedAdminId,
        Instant processingDeadlineAt,
        String manualChecklist,
        String adminNote) {
}
