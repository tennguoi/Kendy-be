package com.example.KendyDigital.service.audit;

import com.example.KendyDigital.model.audit.AuditLog;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Deterministic hash chain for audit logs. Each row commits to the previous row's hash, making
 * silent edits/deletions detectable.
 */
public final class AuditHashChain {
    public static final String GENESIS_HASH = "0".repeat(64);

    private AuditHashChain() {
    }

    public static String compute(String prevHash, AuditLog log) {
        String payload = (prevHash == null || prevHash.isBlank() ? GENESIS_HASH : prevHash) + "|"
                + value(log.getActorUserId()) + "|" + value(log.getActorRole()) + "|" + value(log.getAction())
                + "|" + value(log.getTargetType()) + "|" + value(log.getTargetId()) + "|"
                + value(log.getMetadata()) + "|" + value(log.getIpAddress());
        return sha256(payload);
    }

    private static String value(Object value) {
        return value == null ? "" : String.valueOf(value);
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
