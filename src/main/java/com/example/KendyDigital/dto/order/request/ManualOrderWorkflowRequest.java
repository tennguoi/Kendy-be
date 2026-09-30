package com.example.KendyDigital.dto.order.request;


import jakarta.validation.constraints.Size;
import com.example.KendyDigital.model.order.ManualWorkflowStatus;
import java.time.Instant;
import java.util.List;

public record ManualOrderWorkflowRequest(
        Long assignedAdminId,
        Instant processingDeadlineAt,
        @Size(max = 10000) String manualChecklist,
        @Size(max = 2000) String adminNote,
        ManualWorkflowStatus manualWorkflowStatus,
        List<ManualOrderTaskRequest> tasks) {
}
