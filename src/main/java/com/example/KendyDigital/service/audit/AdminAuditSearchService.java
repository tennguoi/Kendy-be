package com.example.KendyDigital.service.audit;

import com.example.KendyDigital.dto.audit.response.AuditLogResponse;
import java.util.List;

public interface AdminAuditSearchService {
    List<AuditLogResponse> sepayLogs(Integer limit);
    List<AuditLogResponse> searchAudit(String query, String action, Long actorUserId, String targetType,
            Long targetId, Integer limit);
    AuditLogResponse auditDetail(Long id);
    List<AuditLogResponse> adminActions(Long adminId, Integer limit);
}
