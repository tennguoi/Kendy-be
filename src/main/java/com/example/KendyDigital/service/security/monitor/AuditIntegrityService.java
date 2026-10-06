package com.example.KendyDigital.service.security.monitor;

import com.example.KendyDigital.model.audit.AuditLog;
import com.example.KendyDigital.model.security.SecurityEventType;
import com.example.KendyDigital.model.security.SecuritySeverity;
import com.example.KendyDigital.repository.AuditLogRepository;
import com.example.KendyDigital.service.audit.AuditHashChain;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies the audit-log hash chain end to end. A break means rows were edited or deleted directly
 * in the database.
 */
@Service
public class AuditIntegrityService {
    private static final int PAGE_SIZE = 1000;

    private final AuditLogRepository auditLogRepository;
    private final SecuritySignalService securitySignalService;

    public AuditIntegrityService(AuditLogRepository auditLogRepository, SecuritySignalService securitySignalService) {
        this.auditLogRepository = auditLogRepository;
        this.securitySignalService = securitySignalService;
    }

    @Transactional(readOnly = true)
    public IntegrityReport verify() {
        String expectedPrev = AuditHashChain.GENESIS_HASH;
        long checked = 0;
        Long firstBrokenId = null;
        boolean chainStarted = false;
        int page = 0;
        while (true) {
            List<AuditLog> batch = auditLogRepository.findAllByOrderByIdAsc(PageRequest.of(page, PAGE_SIZE));
            if (batch.isEmpty()) {
                break;
            }
            for (AuditLog log : batch) {
                String actual = log.getHash();
                if (actual == null) {
                    // Legacy rows written before the hash chain existed: skip until the chain starts.
                    continue;
                }
                checked++;
                String expected = AuditHashChain.compute(expectedPrev, log);
                if (!chainStarted) {
                    expectedPrev = AuditHashChain.GENESIS_HASH;
                    expected = AuditHashChain.compute(expectedPrev, log);
                    chainStarted = true;
                }
                if (!actual.equals(expected) || !equalsSafe(log.getPrevHash(), expectedPrev)) {
                    if (firstBrokenId == null) {
                        firstBrokenId = log.getId();
                    }
                }
                expectedPrev = actual;
            }
            if (batch.size() < PAGE_SIZE) {
                break;
            }
            page++;
        }
        boolean intact = firstBrokenId == null;
        if (!intact) {
            securitySignalService.record(SecuritySignal
                    .of(SecurityEventType.AUDIT_INTEGRITY_BROKEN, SecuritySeverity.CRITICAL, null)
                    .metadata("firstBrokenAuditLogId=" + firstBrokenId + ";checked=" + checked)
                    .risk(0)
                    .build());
        }
        return new IntegrityReport(intact, checked, firstBrokenId);
    }

    private boolean equalsSafe(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    public record IntegrityReport(boolean intact, long checkedRows, Long firstBrokenId) {
    }
}
