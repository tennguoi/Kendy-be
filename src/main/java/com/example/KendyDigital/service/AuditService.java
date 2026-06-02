package com.example.KendyDigital.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.KendyDigital.dto.AuditLogResponse;
import com.example.KendyDigital.model.AuditLog;
import com.example.KendyDigital.repository.AuditLogRepository;

@Service
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void recordSystem(String action, String targetType, Long targetId, String metadata) {
        auditLogRepository.save(new AuditLog(null, "SYSTEM", action, targetType, targetId, metadata));
    }

    public void recordAdmin(Long adminUserId, String action, String targetType, Long targetId, String metadata) {
        auditLogRepository.save(new AuditLog(adminUserId, "ADMIN", action, targetType, targetId, metadata));
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> list(String action) {
        List<AuditLog> auditLogs = action == null || action.isBlank()
                ? auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 100))
                : auditLogRepository.findAllByActionOrderByCreatedAtDesc(action.trim(), PageRequest.of(0, 100));
        return auditLogs.stream()
                .map(AuditLogResponse::from)
                .toList();
    }
}
