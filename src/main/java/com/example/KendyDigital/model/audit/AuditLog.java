package com.example.KendyDigital.model.audit;

import com.example.KendyDigital.common.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_logs_action", columnList = "action"),
                @Index(name = "idx_audit_logs_created_at", columnList = "created_at"),
                @Index(name = "idx_audit_logs_actor_created", columnList = "actor_user_id, created_at DESC")
        })
public class AuditLog extends TimestampedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_role", length = 50)
    private String actorRole;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    public AuditLog(Long actorUserId, String actorRole, String action, String targetType, Long targetId,
            String metadata) {
        this.actorUserId = actorUserId;
        this.actorRole = actorRole;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.metadata = metadata;
    }

    public void recordIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

}
