package com.example.KendyDigital.dto;

import java.time.Instant;

import com.example.KendyDigital.model.AuditLog;

public record AuditLogResponse(
        Long id,
        Long actorUserId,
        String actorRole,
        String action,
        String targetType,
        Long targetId,
        String metadata,
        Instant createdAt) {
    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getActorUserId(),
                auditLog.getActorRole(),
                auditLog.getAction(),
                auditLog.getTargetType(),
                auditLog.getTargetId(),
                auditLog.getMetadata(),
                auditLog.getCreatedAt());
    }
}
