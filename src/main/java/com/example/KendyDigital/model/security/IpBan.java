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
        name = "ip_bans",
        indexes = {
                @Index(name = "idx_ip_bans_ip", columnList = "ip_or_cidr"),
                @Index(name = "idx_ip_bans_expires_at", columnList = "expires_at")
        })
public class IpBan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ip_or_cidr", nullable = false, length = 64)
    private String ipOrCidr;

    @Column(length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private IpBanSource source = IpBanSource.MANUAL;

    @Column(name = "rule_code", length = 32)
    private String ruleCode;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_by")
    private Long revokedBy;

    @Column(name = "hit_count", nullable = false)
    private long hitCount = 0L;

    public IpBan(String ipOrCidr, String reason, IpBanSource source, String ruleCode, Long createdBy,
            Instant expiresAt) {
        this.ipOrCidr = ipOrCidr;
        this.reason = reason;
        this.source = source;
        this.ruleCode = ruleCode;
        this.createdBy = createdBy;
        this.expiresAt = expiresAt;
    }

    public boolean isActive() {
        return revokedAt == null && (expiresAt == null || expiresAt.isAfter(Instant.now()));
    }

    public void revoke(Long adminUserId) {
        this.revokedAt = Instant.now();
        this.revokedBy = adminUserId;
    }

    public void registerHit() {
        this.hitCount++;
    }
}
