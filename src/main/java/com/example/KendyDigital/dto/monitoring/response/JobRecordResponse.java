package com.example.KendyDigital.dto.monitoring.response;

import com.example.KendyDigital.model.job.JobRecord;
import com.example.KendyDigital.model.job.JobStatus;
import java.time.Instant;
import java.util.UUID;

public record JobRecordResponse(
        UUID id,
        String name,
        JobStatus status,
        String logs,
        Instant createdAt,
        Instant updatedAt) {
    public static JobRecordResponse from(JobRecord job) {
        return new JobRecordResponse(
                job.getPublicId(),
                job.getName(),
                job.getStatus(),
                job.getLogs(),
                job.getCreatedAt(),
                job.getUpdatedAt());
    }
}
