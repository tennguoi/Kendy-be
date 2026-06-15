package com.example.KendyDigital.dto.monitoring.response;

import com.example.KendyDigital.model.job.JobRecord;
import java.time.Instant;

public record JobRecordResponse(
        Long id,
        String name,
        String status,
        String logs,
        Instant createdAt,
        Instant updatedAt) {
    public static JobRecordResponse from(JobRecord job) {
        return new JobRecordResponse(
                job.getId(),
                job.getName(),
                job.getStatus(),
                job.getLogs(),
                job.getCreatedAt(),
                job.getUpdatedAt());
    }
}
