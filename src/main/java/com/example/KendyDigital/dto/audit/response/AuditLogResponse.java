package com.example.KendyDigital.dto.audit.response;

import com.example.KendyDigital.model.audit.AuditLog;
import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        Long actorUserId,
        String actorRole,
        String action,
        String targetType,
        Long targetId,
        String metadata,
        String ipAddress,
        Instant createdAt) {
    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getPublicId(),
                auditLog.getActorUserId(),
                auditLog.getActorRole(),
                auditLog.getAction(),
                auditLog.getTargetType(),
                auditLog.getTargetId(),
                auditLog.getMetadata(),
                auditLog.getIpAddress(),
                auditLog.getCreatedAt());
    }
}
