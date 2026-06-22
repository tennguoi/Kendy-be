package com.example.KendyDigital.model.entitlement;

import com.example.KendyDigital.common.TimestampedEntity;
import com.example.KendyDigital.model.catalog.AccessStrategy;
import com.example.KendyDigital.model.catalog.ServiceItem;
import com.example.KendyDigital.model.order.OrderRecord;
import com.example.KendyDigital.model.user.UserAccount;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_entitlements",
        indexes = {
                @Index(name = "idx_entitlements_user_status", columnList = "user_id,status"),
                @Index(name = "idx_entitlements_expires_at", columnList = "expires_at"),
                @Index(name = "idx_entitlements_external_resource", columnList = "external_resource_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_entitlements_source_order", columnNames = "source_order_id")
        })
public class UserEntitlement extends TimestampedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceItem service;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_order_id", nullable = false)
    private OrderRecord sourceOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_strategy", nullable = false, length = 32)
    private AccessStrategy accessStrategy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EntitlementStatus status = EntitlementStatus.PENDING;

    @Column(name = "access_identifier")
    private String accessIdentifier;

    @Column(name = "external_resource_id")
    private String externalResourceId;

    @Column(name = "provider_metadata", columnDefinition = "TEXT")
    private String providerMetadata;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "renewal_requested_at")
    private Instant renewalRequestedAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    public UserEntitlement(UserAccount user, ServiceItem service, OrderRecord sourceOrder,
            AccessStrategy accessStrategy, String accessIdentifier) {
        this.user = user;
        this.service = service;
        this.sourceOrder = sourceOrder;
        this.accessStrategy = accessStrategy;
        this.accessIdentifier = accessIdentifier;
    }

    public void activate(Instant startsAt, Instant expiresAt, String externalResourceId, String providerMetadata) {
        this.status = EntitlementStatus.ACTIVE;
        this.startsAt = startsAt == null ? Instant.now() : startsAt;
        this.expiresAt = expiresAt;
        this.externalResourceId = blankToNull(externalResourceId);
        this.providerMetadata = blankToNull(providerMetadata);
        this.suspendedAt = null;
        this.revokedAt = null;
        this.lastError = null;
        this.renewalRequestedAt = null;
    }

    public void markExpiring() {
        if (status == EntitlementStatus.ACTIVE) {
            status = EntitlementStatus.EXPIRING;
        }
    }

    public void suspend(String reason) {
        this.status = EntitlementStatus.SUSPENDED;
        this.suspendedAt = Instant.now();
        this.lastError = blankToNull(reason);
    }

    public void revoke(String reason) {
        this.status = EntitlementStatus.REVOKED;
        this.revokedAt = Instant.now();
        this.lastError = blankToNull(reason);
    }

    public void fail(String reason) {
        this.status = EntitlementStatus.FAILED;
        this.lastError = blankToNull(reason);
    }

    public void requestRenewal() {
        this.renewalRequestedAt = Instant.now();
    }

    public void extendTo(Instant newExpiresAt) {
        this.expiresAt = newExpiresAt;
        this.status = EntitlementStatus.ACTIVE;
        this.suspendedAt = null;
        this.lastError = null;
        this.renewalRequestedAt = null;
    }

    public void updateProviderReference(String accessIdentifier, String externalResourceId, String providerMetadata) {
        if (accessIdentifier != null) this.accessIdentifier = blankToNull(accessIdentifier);
        if (externalResourceId != null) this.externalResourceId = blankToNull(externalResourceId);
        if (providerMetadata != null) this.providerMetadata = blankToNull(providerMetadata);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
