package com.example.KendyDigital.service.security.monitor;

import com.example.KendyDigital.model.security.SecurityEvent;
import com.example.KendyDigital.repository.SecurityEventRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Persists security events off the request thread. When the bounded queue saturates, events are
 * dropped (and counted) rather than blocking or amplifying load on the database.
 */
@Component
public class SecurityEventWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityEventWriter.class);

    private final SecurityEventRepository repository;
    private final Executor executor;
    private final AtomicLong dropped = new AtomicLong();
    private final AtomicLong written = new AtomicLong();

    public SecurityEventWriter(SecurityEventRepository repository,
            @Qualifier("jobExecutor") Executor executor) {
        this.repository = repository;
        this.executor = executor;
    }

    public void write(SecurityEvent event) {
        if (event.getFingerprint() == null) {
            event.setFingerprint(fingerprint(event));
        }
        try {
            executor.execute(() -> {
                try {
                    repository.save(event);
                    written.incrementAndGet();
                } catch (RuntimeException exception) {
                    LOGGER.debug("Could not persist security event type={}", event.getType(), exception);
                }
            });
        } catch (RejectedExecutionException exception) {
            dropped.incrementAndGet();
            LOGGER.warn("Security event queue saturated; dropped event type={} ip={}",
                    event.getType(), event.getIp());
        }
    }

    public long droppedCount() {
        return dropped.get();
    }

    public long writtenCount() {
        return written.get();
    }

    public static String fingerprint(SecurityEvent event) {
        String raw = event.getType() + "|" + nullToEmpty(event.getIp()) + "|"
                + nullToEmpty(event.getPath()) + "|" + nullToEmpty(event.getUserAgent());
        return sha256(raw);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
