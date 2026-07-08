package com.example.KendyDigital.dto.order.response;

import com.example.KendyDigital.model.order.ManualOrderTask;
import java.time.Instant;

public record ManualOrderTaskResponse(
        Long id,
        String title,
        boolean completed,
        int sortOrder,
        Instant completedAt,
        Long completedByAdminId,
        String completedByAdminName) {
    public static ManualOrderTaskResponse from(ManualOrderTask task) {
        return new ManualOrderTaskResponse(
                task.getId(),
                task.getTitle(),
                task.isCompleted(),
                task.getSortOrder(),
                task.getCompletedAt(),
                task.getCompletedByAdmin() == null ? null : task.getCompletedByAdmin().getId(),
                task.getCompletedByAdmin() == null ? null : task.getCompletedByAdmin().getName());
    }
}
