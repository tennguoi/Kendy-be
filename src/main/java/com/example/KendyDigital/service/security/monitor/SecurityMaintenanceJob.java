package com.example.KendyDigital.service.security.monitor;

import com.example.KendyDigital.dto.finance.response.BalanceIntegrityIssueResponse;
import com.example.KendyDigital.model.security.AlertStatus;
import com.example.KendyDigital.model.security.SecurityEventType;
import com.example.KendyDigital.model.security.SecuritySeverity;
import com.example.KendyDigital.repository.SecurityAlertRepository;
import com.example.KendyDigital.repository.SecurityEventRepository;
import com.example.KendyDigital.service.analytics_finance.BalanceIntegrityService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Periodic security housekeeping: counter cleanup, event retention, balance reconciliation, audit
 * chain verification and a daily operational digest.
 */
@Component
public class SecurityMaintenanceJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityMaintenanceJob.class);

    private final SecurityCounters counters;
    private final SecurityEventRepository securityEventRepository;
    private final SecurityAlertRepository securityAlertRepository;
    private final BalanceIntegrityService balanceIntegrityService;
    private final AuditIntegrityService auditIntegrityService;
    private final SecuritySignalService securitySignalService;
    private final AlertNotifier alertNotifier;

    public SecurityMaintenanceJob(SecurityCounters counters, SecurityEventRepository securityEventRepository,
            SecurityAlertRepository securityAlertRepository, BalanceIntegrityService balanceIntegrityService,
            AuditIntegrityService auditIntegrityService, SecuritySignalService securitySignalService,
            AlertNotifier alertNotifier) {
        this.counters = counters;
        this.securityEventRepository = securityEventRepository;
        this.securityAlertRepository = securityAlertRepository;
        this.balanceIntegrityService = balanceIntegrityService;
        this.auditIntegrityService = auditIntegrityService;
        this.securitySignalService = securitySignalService;
        this.alertNotifier = alertNotifier;
    }

    @Scheduled(fixedDelay = 60_000)
    public void cleanCounters() {
        try {
            counters.cleanExpired();
        } catch (RuntimeException exception) {
            LOGGER.debug("Counter cleanup failed", exception);
        }
    }

    @Scheduled(cron = "${app.security.monitor.balance-check-cron:0 20 * * * *}")
    public void reconcileBalances() {
        try {
            List<BalanceIntegrityIssueResponse> issues = balanceIntegrityService.findIssues();
            if (issues.isEmpty()) {
                return;
            }
            BalanceIntegrityIssueResponse first = issues.get(0);
            securitySignalService.record(SecuritySignal
                    .of(SecurityEventType.BALANCE_MISMATCH, SecuritySeverity.CRITICAL, null)
                    .user(first.userId())
                    .metadata("issueCount=" + issues.size() + ";firstUserId=" + first.userId()
                            + ";difference=" + first.difference())
                    .risk(0)
                    .build());
        } catch (RuntimeException exception) {
            LOGGER.debug("Balance reconciliation failed", exception);
        }
    }

    @Scheduled(cron = "${app.security.monitor.audit-integrity-cron:0 30 */6 * * *}")
    public void verifyAuditIntegrity() {
        try {
            AuditIntegrityService.IntegrityReport report = auditIntegrityService.verify();
            if (!report.intact()) {
                LOGGER.error("Audit log integrity check FAILED firstBrokenId={} checked={}",
                        report.firstBrokenId(), report.checkedRows());
            }
        } catch (RuntimeException exception) {
            LOGGER.debug("Audit integrity verification failed", exception);
        }
    }

    @Scheduled(cron = "${app.security.monitor.digest-cron:0 0 9 * * *}")
    public void dailyDigest() {
        try {
            Instant since = Instant.now().minus(Duration.ofHours(24));
            long events = securityEventRepository.countByOccurredAtGreaterThanEqual(since);
            long open = securityAlertRepository.countByStatus(AlertStatus.OPEN);
            long critical = securityAlertRepository.countByStatusAndSeverity(AlertStatus.OPEN,
                    SecuritySeverity.CRITICAL);
            long high = securityAlertRepository.countByStatusAndSeverity(AlertStatus.OPEN, SecuritySeverity.HIGH);
            String body = "Security digest (24h)\n"
                    + "events=" + events + "\n"
                    + "openAlerts=" + open + " (critical=" + critical + ", high=" + high + ")\n"
                    + "generatedAt=" + Instant.now();
            alertNotifier.digest("[KendyDigital] Security digest", body);
        } catch (RuntimeException exception) {
            LOGGER.debug("Daily digest failed", exception);
        }
    }

    @Scheduled(cron = "${app.security.monitor.retention-cron:0 0 3 * * *}")
    @Transactional
    public void purgeOldEvents() {
        try {
            Instant lowCutoff = Instant.now().minus(Duration.ofDays(30));
            Instant mediumCutoff = Instant.now().minus(Duration.ofDays(180));
            securityEventRepository.deleteByOccurredAtBeforeAndSeverityIn(lowCutoff,
                    List.of(SecuritySeverity.INFO, SecuritySeverity.LOW));
            securityEventRepository.deleteByOccurredAtBeforeAndSeverityIn(mediumCutoff,
                    List.of(SecuritySeverity.MEDIUM));
        } catch (RuntimeException exception) {
            LOGGER.debug("Event retention purge failed", exception);
        }
    }
}
