package com.example.KendyDigital.service.audit.impl;

import com.example.KendyDigital.dto.audit.response.AuditLogResponse;
import com.example.KendyDigital.repository.AuditLogRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.service.audit.AdminAuditSearchService;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminAuditSearchServiceImpl implements AdminAuditSearchService {
    private final AuditLogRepository auditLogRepository;
    private final UserAccountRepository userAccountRepository;

    public AdminAuditSearchServiceImpl(AuditLogRepository auditLogRepository,
            UserAccountRepository userAccountRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> sepayLogs(Integer limit) {
        return auditLogRepository.findAll(
                com.example.KendyDigital.repository.specification.AuditLogSpecifications.searchAdmin(
                        "SEPAY", null, null, null, null),
                page(limit)).getContent()
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> searchAudit(String query, String action, UUID actorUserId, String targetType,
            Long targetId, Integer limit) {
        Long internalActorId = null;
        if (actorUserId != null) {
            internalActorId = userAccountRepository.findByPublicId(actorUserId)
                    .map(user -> user.getId())
                    .orElse(null);
            if (internalActorId == null) {
                return List.of();
            }
        }
        return auditLogRepository.findAll(
                com.example.KendyDigital.repository.specification.AuditLogSpecifications.searchAdmin(
                        normalizeQuery(query), blankToNull(action), internalActorId, blankToNull(targetType), targetId),
                page(limit)).getContent()
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogResponse auditDetail(UUID id) {
        return auditLogRepository.findByPublicId(id)
                .map(AuditLogResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit log not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> adminActions(UUID adminId, Integer limit) {
        Long internalAdminId = userAccountRepository.findByPublicId(adminId)
                .map(user -> user.getId())
                .orElse(null);
        if (internalAdminId == null) {
            return List.of();
        }
        return auditLogRepository.findAllByActorUserIdOrderByCreatedAtDesc(internalAdminId, page(limit))
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    private PageRequest page(Integer limit) {
        int normalizedLimit = limit == null ? 100 : Math.max(1, Math.min(limit, 500));
        return PageRequest.of(0, normalizedLimit);
    }

    private String normalizeQuery(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
