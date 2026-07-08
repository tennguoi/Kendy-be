package com.example.KendyDigital.dto.order.request;

import com.example.KendyDigital.model.order.ManualWorkflowStatus;
import java.time.Instant;
import java.util.List;

public record ManualOrderWorkflowRequest(
        Long assignedAdminId,
        Instant processingDeadlineAt,
        String manualChecklist,
        String adminNote,
        ManualWorkflowStatus manualWorkflowStatus,
        List<ManualOrderTaskRequest> tasks) {
}
