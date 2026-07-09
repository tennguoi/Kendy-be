package com.example.KendyDigital.service.webhook;

import com.example.KendyDigital.config.SePayWebhookProperties;
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
    private final Map<String, RejectionCounter> signatureRejections = new ConcurrentHashMap<>();

    public SePayWebhookSecurityMonitor(SePayWebhookProperties properties) {
        this.properties = properties;
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
        if (count == threshold || count % threshold == 0) {
            LOGGER.error("SECURITY ALERT: repeated invalid SePay webhook signatures ip={} count={} windowSeconds={}",
                    safe(ipAddress), count, rejectionWindowSeconds());
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
