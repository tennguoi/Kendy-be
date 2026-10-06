package com.example.KendyDigital.model.user;

import com.example.KendyDigital.common.VersionedEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_status", columnList = "status"),
                @Index(name = "idx_users_oauth_provider", columnList = "oauth_provider, oauth_provider_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_users_public_id", columnNames = "public_id")
        })
public class UserAccount extends VersionedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(length = 11)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "oauth_provider", length = 50)
    private String oauthProvider;

    @Column(name = "oauth_provider_id", length = 100)
    private String oauthProviderId;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "two_factor_enabled", nullable = false)
    private boolean twoFactorEnabled = false;

    @Column(name = "failed_login_attempts", nullable = false, columnDefinition = "integer default 0")
    private int failedLoginAttempts = 0;

    @Column(name = "failed_two_factor_attempts", nullable = false, columnDefinition = "integer default 0")
    private int failedTwoFactorAttempts = 0;

    @Column(name = "two_factor_locked_until")
    private Instant twoFactorLockedUntil;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    @Column(name = "last_login_country", length = 2)
    private String lastLoginCountry;

    @Column(name = "wallet_frozen", nullable = false)
    private boolean walletFrozen = false;

    @Column(name = "wallet_frozen_reason", length = 255)
    private String walletFrozenReason;

    @Column(name = "wallet_frozen_at")
    private Instant walletFrozenAt;

    @Column(name = "wallet_frozen_by")
    private Long walletFrozenBy;

    @Column(name = "locale", length = 10)
    private String locale;

    @Column(name = "admin_permissions", columnDefinition = "TEXT")
    private String adminPermissions;

    @Column(name = "two_factor_secret", columnDefinition = "TEXT")
    private String twoFactorSecret;

    @Column(name = "backup_codes", columnDefinition = "TEXT")
    private String backupCodes;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserAdminRole> adminRoles = new HashSet<>();

    public UserAccount(String name, String email, String phone, String passwordHash) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
    }

    public void enableTwoFactor(String secret, String backupCodes) {
        this.twoFactorEnabled = true;
        this.twoFactorSecret = secret;
        this.backupCodes = backupCodes;
    }

    public void disableTwoFactor() {
        this.twoFactorEnabled = false;
        this.twoFactorSecret = null;
        this.backupCodes = null;
    }

    public void resetTwoFactor() {
        disableTwoFactor();
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        this.passwordChangedAt = Instant.now();
    }

    public void verifyEmail() {
        this.emailVerifiedAt = Instant.now();
        if (this.status == UserStatus.PENDING_VERIFY) {
            this.status = UserStatus.ACTIVE;
        }
    }

    public void linkOAuth(String provider, String providerId, String avatarUrl) {
        this.oauthProvider = provider;
        this.oauthProviderId = providerId;
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            this.avatarUrl = avatarUrl;
        }
    }

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    public void recordFailedLogin() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.lockedUntil = Instant.now().plus(java.time.Duration.ofMinutes(15));
        }
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    public void recordSuccessfulLogin() {
        resetFailedLoginAttempts();
        this.failedTwoFactorAttempts = 0;
        this.twoFactorLockedUntil = null;
    }

    public void recordFailedTwoFactor() {
        this.failedTwoFactorAttempts++;
        if (this.failedTwoFactorAttempts >= 5) {
            this.twoFactorLockedUntil = Instant.now().plus(java.time.Duration.ofMinutes(30));
        }
    }

    public boolean isTwoFactorLocked() {
        return twoFactorLockedUntil != null && twoFactorLockedUntil.isAfter(Instant.now());
    }

    public void resetTwoFactorAttempts() {
        this.failedTwoFactorAttempts = 0;
        this.twoFactorLockedUntil = null;
    }

    public void recordLoginContext(Instant at, String ip, String country) {
        this.lastLoginAt = at;
        this.lastLoginIp = ip;
        this.lastLoginCountry = country;
    }

    /**
     * Returns true if the user has a password they know (set during registration or
     * explicitly changed later). OAuth-only accounts have a random password the user
     * never saw, so this returns false for them until they set one via /set-password.
     */
    public boolean hasPassword() {
        return oauthProvider == null || passwordChangedAt != null;
    }

    public void freezeWallet(String reason, Long frozenBy) {
        this.walletFrozen = true;
        this.walletFrozenReason = reason;
        this.walletFrozenAt = Instant.now();
        this.walletFrozenBy = frozenBy;
    }

    public void unfreezeWallet() {
        this.walletFrozen = false;
        this.walletFrozenReason = null;
        this.walletFrozenAt = null;
        this.walletFrozenBy = null;
    }

}
