package com.example.KendyDigital.service.audit;

import com.example.KendyDigital.dto.audit.response.AuditLogResponse;
import java.util.List;
import java.util.UUID;

public interface AdminAuditSearchService {
    List<AuditLogResponse> sepayLogs(Integer limit);
    List<AuditLogResponse> searchAudit(String query, String action, UUID actorUserId, String targetType,
            Long targetId, Integer limit);
    AuditLogResponse auditDetail(UUID id);
    List<AuditLogResponse> adminActions(UUID adminId, Integer limit);
}
