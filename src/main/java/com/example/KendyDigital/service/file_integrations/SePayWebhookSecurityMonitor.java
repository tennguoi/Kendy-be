package com.example.KendyDigital.service.file_integrations;

import com.example.KendyDigital.config.SePayWebhookProperties;
import com.example.KendyDigital.model.security.SecurityEventType;
import com.example.KendyDigital.model.security.SecuritySeverity;
import com.example.KendyDigital.service.audit.AuditService;
import com.example.KendyDigital.service.security.monitor.SecuritySignal;
import com.example.KendyDigital.service.security.monitor.SecuritySignalService;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SePayWebhookSecurityMonitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SePayWebhookSecurityMonitor.class);

    private final SePayWebhookProperties properties;
    private final AuditService auditService;
    private final SecuritySignalService securitySignalService;
    private final Map<String, RejectionCounter> signatureRejections = new ConcurrentHashMap<>();

    public SePayWebhookSecurityMonitor(SePayWebhookProperties properties, AuditService auditService,
            SecuritySignalService securitySignalService) {
        this.properties = properties;
        this.auditService = auditService;
        this.securitySignalService = securitySignalService;
    }

    public void received(String ipAddress, int payloadBytes) {
        LOGGER.info("SePay webhook received ip={} payloadBytes={}", safe(ipAddress), payloadBytes);
    }

    public void accepted(String ipAddress, Long sepayId, String referenceCode) {
        LOGGER.info("SePay webhook accepted ip={} sepayId={} referenceCode={}",
                safe(ipAddress), sepayId, safe(referenceCode));
    }

    public void rejected(String ipAddress, String reason, boolean signatureFailure) {
        LOGGER.warn("SePay webhook rejected ip={} reason={}", safe(ipAddress), safe(reason));
        emitSignal(ipAddress, reason);
        if (!signatureFailure) {
            return;
        }

        long now = Instant.now().getEpochSecond();
        RejectionCounter counter = signatureRejections.compute(ipAddress, (ignored, existing) -> {
            if (existing == null || now - existing.windowStartedAt() >= rejectionWindowSeconds()) {
                return new RejectionCounter(now, new AtomicInteger(1));
            }
            existing.count().incrementAndGet();
            return existing;
        });

        int threshold = Math.max(1, properties.getRejectionAlertThreshold());
        int count = counter.count().get();
        // Persist only the first rejection of a window and every threshold-th one, to avoid flooding audit_logs.
        if (count == 1 || count % threshold == 0) {
            recordAudit(ipAddress, reason, count);
        }
        if (count == threshold || count % threshold == 0) {
            LOGGER.error("SECURITY ALERT: repeated invalid SePay webhook signatures ip={} count={} windowSeconds={}",
                    safe(ipAddress), count, rejectionWindowSeconds());
        }
    }

    private void emitSignal(String ipAddress, String reason) {
        try {
            securitySignalService.record(SecuritySignal
                    .of(SecurityEventType.WEBHOOK_REJECTED, SecuritySeverity.MEDIUM, ipAddress)
                    .request("POST", "/api/webhooks/sepay")
                    .metadata("reason=" + safe(reason))
                    .risk(30)
                    .build());
        } catch (RuntimeException exception) {
            LOGGER.debug("Could not emit webhook rejection signal", exception);
        }
    }

    private void recordAudit(String ipAddress, String reason, int count) {
        try {
            auditService.recordSystem("SEPAY_WEBHOOK_REJECTED", "WEBHOOK", null,
                    "ip=" + safe(ipAddress) + ",count=" + count + ",reason=" + safe(reason));
        } catch (RuntimeException exception) {
            LOGGER.debug("Could not persist webhook rejection audit event", exception);
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void cleanExpired() {
        long now = Instant.now().getEpochSecond();
        signatureRejections.entrySet()
                .removeIf(entry -> now - entry.getValue().windowStartedAt() >= rejectionWindowSeconds());
    }

    private int rejectionWindowSeconds() {
        return Math.max(60, properties.getRejectionWindowSeconds());
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[\\r\\n\\t]", "_");
    }

    private record RejectionCounter(long windowStartedAt, AtomicInteger count) {
    }
}
