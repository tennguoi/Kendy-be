package com.example.KendyDigital.dto;

import java.time.Instant;

import com.example.KendyDigital.model.JobRecord;

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
