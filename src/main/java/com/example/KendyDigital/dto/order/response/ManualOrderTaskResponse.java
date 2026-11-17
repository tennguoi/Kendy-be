package com.example.KendyDigital.dto.order.response;

import com.example.KendyDigital.model.order.ManualOrderTask;
import java.time.Instant;
import java.util.UUID;

public record ManualOrderTaskResponse(
        UUID id,
        String title,
        boolean completed,
        int sortOrder,
        Instant completedAt,
        UUID completedByAdminId,
        String completedByAdminName) {
    public static ManualOrderTaskResponse from(ManualOrderTask task) {
        return new ManualOrderTaskResponse(
                task.getPublicId(),
                task.getTitle(),
                task.isCompleted(),
                task.getSortOrder(),
                task.getCompletedAt(),
                task.getCompletedByAdmin() == null ? null : task.getCompletedByAdmin().getPublicId(),
                task.getCompletedByAdmin() == null ? null : task.getCompletedByAdmin().getName());
    }
}
