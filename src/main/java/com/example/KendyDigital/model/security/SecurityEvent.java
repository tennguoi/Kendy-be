package com.example.KendyDigital.model.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "security_events",
        indexes = {
                @Index(name = "idx_security_events_occurred_at", columnList = "occurred_at"),
                @Index(name = "idx_security_events_type", columnList = "type"),
                @Index(name = "idx_security_events_severity", columnList = "severity"),
                @Index(name = "idx_security_events_ip", columnList = "ip"),
                @Index(name = "idx_security_events_user_id", columnList = "user_id"),
                @Index(name = "idx_security_events_fingerprint", columnList = "fingerprint")
        })
public class SecurityEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private SecurityEventType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SecuritySeverity severity;

    @Column(length = 45)
    private String ip;

    @Column(length = 64)
    private String asn;

    @Column(length = 2)
    private String country;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(length = 10)
    private String method;

    @Column(length = 512)
    private String path;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "event_count", nullable = false)
    private int eventCount = 1;

    @Column(length = 64)
    private String fingerprint;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    public SecurityEvent(SecurityEventType type, SecuritySeverity severity, String ip, Long userId,
            Long sessionId, String method, String path, String userAgent, String fingerprint, String metadata) {
        this.occurredAt = Instant.now();
        this.type = type;
        this.severity = severity;
        this.ip = ip;
        this.userId = userId;
        this.sessionId = sessionId;
        this.method = method;
        this.path = path;
        this.userAgent = userAgent;
        this.fingerprint = fingerprint;
        this.metadata = metadata;
    }
}
