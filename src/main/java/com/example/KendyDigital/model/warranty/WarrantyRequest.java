package com.example.KendyDigital.model.warranty;

import com.example.KendyDigital.common.TimestampedEntity;
import com.example.KendyDigital.model.inventory.AccountCredential;
import com.example.KendyDigital.model.order.OrderRecord;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.wallet.WalletTransaction;
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
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "warranty_requests",
        indexes = {
                @Index(name = "idx_warranty_requests_order", columnList = "order_id"),
                @Index(name = "idx_warranty_requests_user", columnList = "user_id"),
                @Index(name = "idx_warranty_requests_status", columnList = "status")
        })
public class WarrantyRequest extends TimestampedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderRecord order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_credential_id")
    private AccountCredential originalCredential;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replacement_credential_id")
    private AccountCredential replacementCredential;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_wallet_transaction_id")
    private WalletTransaction refundWalletTransaction;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "evidence_text", columnDefinition = "TEXT")
    private String evidenceText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WarrantyRequestStatus status = WarrantyRequestStatus.OPEN;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public WarrantyRequest(OrderRecord order, UserAccount user, AccountCredential originalCredential,
            String reason, String evidenceText) {
        this.order = order;
        this.user = user;
        this.originalCredential = originalCredential;
        this.reason = reason;
        this.evidenceText = evidenceText;
    }

    public void markReviewing(String adminNote) {
        this.status = WarrantyRequestStatus.REVIEWING;
        this.adminNote = adminNote;
    }

    public void approveReplace(AccountCredential replacementCredential, String adminNote) {
        this.status = WarrantyRequestStatus.APPROVED_REPLACE;
        this.replacementCredential = replacementCredential;
        this.adminNote = adminNote;
        this.resolvedAt = Instant.now();
    }

    public void approveRefund(WalletTransaction refundWalletTransaction, String adminNote) {
        this.status = WarrantyRequestStatus.APPROVED_REFUND;
        this.refundWalletTransaction = refundWalletTransaction;
        this.adminNote = adminNote;
        this.resolvedAt = Instant.now();
    }

    public void reject(String adminNote) {
        this.status = WarrantyRequestStatus.REJECTED;
        this.adminNote = adminNote;
        this.resolvedAt = Instant.now();
    }

    public void resolve(String adminNote) {
        this.status = WarrantyRequestStatus.RESOLVED;
        this.adminNote = adminNote;
        this.resolvedAt = Instant.now();
    }
}
