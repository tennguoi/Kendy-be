package com.example.KendyDigital.service.security.monitor;

import com.example.KendyDigital.model.security.SecurityEventType;
import com.example.KendyDigital.model.security.SecuritySeverity;

/**
 * A single security observation. Signals are cheap to create and are fanned out by
 * {@link SecuritySignalService} into persistent events, risk scores and detection rules.
 */
public record SecuritySignal(
        SecurityEventType type,
        SecuritySeverity severity,
        String ip,
        Long userId,
        Long sessionId,
        String method,
        String path,
        String userAgent,
        String metadata,
        double riskPoints) {

    public static Builder of(SecurityEventType type, SecuritySeverity severity, String ip) {
        return new Builder(type, severity, ip);
    }

    public static final class Builder {
        private final SecurityEventType type;
        private final SecuritySeverity severity;
        private final String ip;
        private Long userId;
        private Long sessionId;
        private String method;
        private String path;
        private String userAgent;
        private String metadata;
        private double riskPoints;

        private Builder(SecurityEventType type, SecuritySeverity severity, String ip) {
            this.type = type;
            this.severity = severity;
            this.ip = ip;
        }

        public Builder user(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder session(Long sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder request(String method, String path) {
            this.method = method;
            this.path = path;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder metadata(String metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder risk(double riskPoints) {
            this.riskPoints = riskPoints;
            return this;
        }

        public SecuritySignal build() {
            return new SecuritySignal(type, severity, ip, userId, sessionId, method, path, userAgent, metadata,
                    riskPoints);
        }
    }
}
