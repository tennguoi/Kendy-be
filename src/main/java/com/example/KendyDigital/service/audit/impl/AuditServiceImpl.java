package com.example.KendyDigital.service.audit.impl;

import com.example.KendyDigital.service.audit.*;

import com.example.KendyDigital.common.ClientIpResolver;
import com.example.KendyDigital.dto.audit.response.AuditLogResponse;
import com.example.KendyDigital.model.audit.AuditLog;
import com.example.KendyDigital.repository.AuditLogRepository;
import com.example.KendyDigital.service.audit.AuditHashChain;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditServiceImpl  implements AuditService{
    private static final Object CHAIN_LOCK = new Object();

    private final AuditLogRepository auditLogRepository;
    private final ClientIpResolver clientIpResolver;

    public AuditServiceImpl(AuditLogRepository auditLogRepository, ClientIpResolver clientIpResolver) {
        this.auditLogRepository = auditLogRepository;
        this.clientIpResolver = clientIpResolver;
    }

    @Transactional
    public void recordSystem(String action, String targetType, Long targetId, String metadata) {
        persist(new AuditLog(null, "SYSTEM", action, targetType, targetId, sanitizeMetadata(metadata)));
    }

    @Transactional
    public void recordAdmin(Long adminUserId, String action, String targetType, Long targetId, String metadata) {
        persist(new AuditLog(adminUserId, "ADMIN", action, targetType, targetId, sanitizeMetadata(metadata)));
    }

    private void persist(AuditLog log) {
        log.recordIpAddress(clientIpResolver.resolveCurrent());
        log.recordRequestContext(currentUserAgent(), currentRequestId());
        synchronized (CHAIN_LOCK) {
            String prev = auditLogRepository.findTopByOrderByIdDesc()
                    .map(AuditLog::getHash)
                    .orElse(AuditHashChain.GENESIS_HASH);
            if (prev == null || prev.isBlank()) {
                prev = AuditHashChain.GENESIS_HASH;
            }
            log.recordChain(prev, AuditHashChain.compute(prev, log));
            auditLogRepository.save(log);
        }
    }

    private String currentUserAgent() {
        try {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes servletAttributes) {
                String userAgent = servletAttributes.getRequest().getHeader("User-Agent");
                return userAgent == null ? null
                        : userAgent.substring(0, Math.min(userAgent.length(), 512));
            }
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    private String currentRequestId() {
        return clientIpResolver.currentRequestId();
    }

    private String sanitizeMetadata(String metadata) {
        if (metadata == null) {
            return null;
        }
        // Remove newlines and control characters to prevent log injection
        return metadata.replaceAll("[\\r\\n\\t]", "_")
                .substring(0, Math.min(metadata.length(), 1000));
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> list(String action, Integer page, Integer size) {
        int safePage = page == null ? 0 : Math.max(0, page);
        int safeSize = size == null ? 100 : Math.max(1, Math.min(size, 200));
        PageRequest pageable = PageRequest.of(safePage, safeSize);
        List<AuditLog> auditLogs = action == null || action.isBlank()
                ? auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                : auditLogRepository.findAllByActionOrderByCreatedAtDesc(action.trim(), pageable);
        return auditLogs.stream()
                .map(AuditLogResponse::from)
                .toList();
    }
}
