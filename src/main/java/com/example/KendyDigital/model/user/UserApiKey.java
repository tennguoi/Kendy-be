package com.example.KendyDigital.model.user;

import com.example.KendyDigital.common.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
        name = "user_api_keys",
        indexes = {
                @Index(name = "idx_user_api_keys_user_id", columnList = "user_id"),
                @Index(name = "idx_user_api_keys_key_prefix", columnList = "key_prefix")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_api_keys_hash", columnNames = "key_hash")
        })
public class UserApiKey extends TimestampedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(nullable = false)
    private String name;

    @Column(name = "key_prefix", nullable = false, length = 20)
    private String keyPrefix;

    @Column(name = "key_hash", nullable = false, length = 64)
    private String keyHash;

    @Column(columnDefinition = "TEXT")
    private String scopes;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public UserApiKey(UserAccount user, String name, String keyPrefix, String keyHash, String scopes) {
        this.user = user;
        this.name = name;
        this.keyPrefix = keyPrefix;
        this.keyHash = keyHash;
        this.scopes = scopes;
    }

    public void markUsed() {
        this.lastUsedAt = Instant.now();
    }

    public void revoke() {
        this.revokedAt = Instant.now();
    }
}
