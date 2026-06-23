package com.example.KendyDigital.service.audit;

import com.example.KendyDigital.dto.audit.response.AuditLogResponse;
import java.util.List;

public interface AuditService {
    void recordSystem(String action, String targetType, Long targetId, String metadata);
    void recordAdmin(Long adminUserId, String action, String targetType, Long targetId, String metadata);
    List<AuditLogResponse> list(String action, Integer page, Integer size);
}
