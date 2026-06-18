package com.example.KendyDigital.service.audit;

import com.example.KendyDigital.dto.audit.response.AuditLogResponse;
import com.example.KendyDigital.model.audit.AuditLog;
import com.example.KendyDigital.repository.AuditLogRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditServiceImpl  implements AuditService{
    private final AuditLogRepository auditLogRepository;

    public AuditServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    private String getClientIp() {
        try {
            org.springframework.web.context.request.RequestAttributes attributes =
                    org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attributes instanceof org.springframework.web.context.request.ServletRequestAttributes) {
                jakarta.servlet.http.HttpServletRequest request =
                        ((org.springframework.web.context.request.ServletRequestAttributes) attributes).getRequest();
                String forwardedFor = request.getHeader("X-Forwarded-For");
                if (forwardedFor != null && !forwardedFor.isBlank()) {
                    return forwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            // Ignore when not in request context
        }
        return null;
    }

    @Transactional
    public void recordSystem(String action, String targetType, Long targetId, String metadata) {
        AuditLog log = new AuditLog(null, "SYSTEM", action, targetType, targetId, metadata);
        log.recordIpAddress(getClientIp());
        auditLogRepository.save(log);
    }

    @Transactional
    public void recordAdmin(Long adminUserId, String action, String targetType, Long targetId, String metadata) {
        AuditLog log = new AuditLog(adminUserId, "ADMIN", action, targetType, targetId, metadata);
        log.recordIpAddress(getClientIp());
        auditLogRepository.save(log);
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
