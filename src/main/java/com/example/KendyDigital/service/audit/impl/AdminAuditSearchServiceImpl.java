package com.example.KendyDigital.service.audit.impl;

import com.example.KendyDigital.dto.audit.response.AuditLogResponse;
import com.example.KendyDigital.repository.AuditLogRepository;
import com.example.KendyDigital.service.audit.AdminAuditSearchService;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminAuditSearchServiceImpl implements AdminAuditSearchService {
    private final AuditLogRepository auditLogRepository;

    public AdminAuditSearchServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> sepayLogs(Integer limit) {
        return auditLogRepository.searchAdmin(likePattern("SEPAY"), null, null, null, null, page(limit))
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> searchAudit(String query, String action, Long actorUserId, String targetType,
            Long targetId, Integer limit) {
        return auditLogRepository.searchAdmin(likePattern(normalizeQuery(query)), blankToNull(action), actorUserId,
                        blankToNull(targetType), targetId, page(limit))
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogResponse auditDetail(Long id) {
        return auditLogRepository.findById(id)
                .map(AuditLogResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit log not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> adminActions(Long adminId, Integer limit) {
        return auditLogRepository.findAllByActorUserIdOrderByCreatedAtDesc(adminId, page(limit))
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

    private String likePattern(String value) {
        return value == null ? null : "%" + value.toLowerCase(Locale.ROOT) + "%";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
