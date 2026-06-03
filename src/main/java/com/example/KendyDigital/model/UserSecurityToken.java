package com.example.KendyDigital.model;

import java.time.Instant;

import com.example.KendyDigital.common.TimestampedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_security_tokens",
        indexes = {
                @Index(name = "idx_user_security_tokens_user_id", columnList = "user_id"),
                @Index(name = "idx_user_security_tokens_type", columnList = "type"),
                @Index(name = "idx_user_security_tokens_expires_at", columnList = "expires_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_security_tokens_hash", columnNames = "token_hash")
        })
public class UserSecurityToken extends TimestampedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private UserSecurityTokenType type;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    public UserSecurityToken(UserAccount user, UserSecurityTokenType type, String tokenHash, Instant expiresAt) {
        this.user = user;
        this.type = type;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean isUsable() {
        return usedAt == null && expiresAt.isAfter(Instant.now());
    }

    public void markUsed() {
        this.usedAt = Instant.now();
    }
}
