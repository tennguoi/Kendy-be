package com.example.KendyDigital.model.security;

import com.example.KendyDigital.common.TimestampedEntity;
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
        name = "security_alerts",
        indexes = {
                @Index(name = "idx_security_alerts_status", columnList = "status"),
                @Index(name = "idx_security_alerts_severity", columnList = "severity"),
                @Index(name = "idx_security_alerts_last_seen", columnList = "last_seen"),
                @Index(name = "idx_security_alerts_subject", columnList = "subject_type, subject_value")
        })
public class SecurityAlert extends TimestampedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_code", nullable = false, length = 32)
    private String ruleCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SecuritySeverity severity;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 16)
    private AlertSubjectType subjectType;

    @Column(name = "subject_value", nullable = false, length = 128)
    private String subjectValue;

    @Column(name = "first_seen", nullable = false)
    private Instant firstSeen = Instant.now();

    @Column(name = "last_seen", nullable = false)
    private Instant lastSeen = Instant.now();

    @Column(name = "event_count", nullable = false)
    private int eventCount = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AlertStatus status = AlertStatus.OPEN;

    @Column(length = 255)
    private String assignee;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    public SecurityAlert(String ruleCode, SecuritySeverity severity, String title, AlertSubjectType subjectType,
            String subjectValue, String metadata) {
        this.ruleCode = ruleCode;
        this.severity = severity;
        this.title = title;
        this.subjectType = subjectType;
        this.subjectValue = subjectValue;
        this.metadata = metadata;
    }

    public void registerOccurrence() {
        this.eventCount++;
        this.lastSeen = Instant.now();
    }

    public void resolve(AlertStatus status, String assignee, String note) {
        this.status = status;
        this.assignee = assignee;
        this.note = note;
    }
}
