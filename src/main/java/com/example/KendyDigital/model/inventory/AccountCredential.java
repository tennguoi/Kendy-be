package com.example.KendyDigital.model.inventory;

import com.example.KendyDigital.common.TimestampedEntity;
import com.example.KendyDigital.model.catalog.ServiceItem;
import com.example.KendyDigital.model.checkout.CheckoutSession;
import com.example.KendyDigital.model.order.OrderRecord;
import com.example.KendyDigital.model.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "account_credentials",
        indexes = {
                @Index(name = "idx_account_credentials_service_status", columnList = "service_id,status"),
                @Index(name = "idx_account_credentials_order", columnList = "assigned_order_id"),
                @Index(name = "idx_account_credentials_user", columnList = "delivered_to_user_id"),
                @Index(name = "idx_account_credentials_reserved_until", columnList = "reserved_until")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_account_credentials_service_login", columnNames = {"service_id", "login_identifier"}),
                @UniqueConstraint(name = "uk_account_credentials_service_payload_hash", columnNames = {"service_id", "payload_hash"}),
                @UniqueConstraint(name = "uk_account_credentials_order", columnNames = "assigned_order_id"),
                @UniqueConstraint(name = "uk_account_credentials_checkout", columnNames = "reserved_checkout_id")
        })
public class AccountCredential extends TimestampedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceItem service;

    @Column(name = "login_identifier", nullable = false)
    private String loginIdentifier;

    @Column(name = "password_secret", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = EncryptedCredentialAttributeConverter.class)
    private String passwordSecret;

    @Column(name = "recovery_info", columnDefinition = "TEXT")
    @Convert(converter = EncryptedCredentialAttributeConverter.class)
    private String recoveryInfo;

    @Column(name = "two_factor_secret", columnDefinition = "TEXT")
    @Convert(converter = EncryptedCredentialAttributeConverter.class)
    private String twoFactorSecret;

    @Column(name = "usage_note", columnDefinition = "TEXT")
    private String usageNote;

    @Column(name = "internal_note", columnDefinition = "TEXT")
    private String internalNote;

    @Column(name = "payload_hash", length = 128)
    private String payloadHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AccountCredentialStatus status = AccountCredentialStatus.AVAILABLE;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_order_id")
    private OrderRecord assignedOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivered_to_user_id")
    private UserAccount deliveredToUser;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserved_checkout_id")
    private CheckoutSession reservedCheckout;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserved_by_user_id")
    private UserAccount reservedByUser;

    @Column(name = "reserved_at")
    private Instant reservedAt;

    @Column(name = "reserved_until")
    private Instant reservedUntil;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "warranty_until")
    private Instant warrantyUntil;

    public AccountCredential(ServiceItem service, String loginIdentifier, String passwordSecret, String recoveryInfo,
            String twoFactorSecret, String usageNote, String internalNote, Instant expiresAt, Instant warrantyUntil) {
        this.service = service;
        this.loginIdentifier = loginIdentifier;
        this.passwordSecret = passwordSecret;
        this.recoveryInfo = recoveryInfo;
        this.twoFactorSecret = twoFactorSecret;
        this.usageNote = usageNote;
        this.internalNote = internalNote;
        this.expiresAt = expiresAt;
        this.warrantyUntil = warrantyUntil;
    }

    public void updateSecrets(String loginIdentifier, String passwordSecret, String recoveryInfo,
            String twoFactorSecret, String usageNote, String internalNote, Instant expiresAt, Instant warrantyUntil) {
        this.loginIdentifier = loginIdentifier;
        this.passwordSecret = passwordSecret;
        this.recoveryInfo = recoveryInfo;
        this.twoFactorSecret = twoFactorSecret;
        this.usageNote = usageNote;
        this.internalNote = internalNote;
        this.expiresAt = expiresAt;
        this.warrantyUntil = warrantyUntil;
    }

    public void updatePayloadHash(String payloadHash) {
        this.payloadHash = payloadHash;
    }

    public void deliver(OrderRecord order, UserAccount user) {
        this.status = AccountCredentialStatus.DELIVERED;
        this.assignedOrder = order;
        this.deliveredToUser = user;
        this.deliveredAt = Instant.now();
        clearReservation();
    }

    public void reserve(CheckoutSession checkout, UserAccount user, Instant reservedUntil) {
        this.status = AccountCredentialStatus.RESERVED;
        this.reservedCheckout = checkout;
        this.reservedByUser = user;
        this.reservedAt = Instant.now();
        this.reservedUntil = reservedUntil;
    }

    public void releaseReservation() {
        if (this.status == AccountCredentialStatus.RESERVED) {
            this.status = AccountCredentialStatus.AVAILABLE;
        }
        clearReservation();
    }

    public void changeStatus(AccountCredentialStatus status) {
        this.status = status;
        if (status == AccountCredentialStatus.AVAILABLE) {
            this.assignedOrder = null;
            this.deliveredToUser = null;
            this.deliveredAt = null;
            clearReservation();
        }
        if (status == AccountCredentialStatus.DISABLED
                || status == AccountCredentialStatus.EXPIRED
                || status == AccountCredentialStatus.REPLACED) {
            clearReservation();
        }
    }

    public void markRefunded() {
        this.status = AccountCredentialStatus.REFUNDED;
        clearReservation();
    }

    public void markReplaced() {
        this.status = AccountCredentialStatus.REPLACED;
        this.assignedOrder = null;
        clearReservation();
    }

    private void clearReservation() {
        this.reservedCheckout = null;
        this.reservedByUser = null;
        this.reservedAt = null;
        this.reservedUntil = null;
    }
}
