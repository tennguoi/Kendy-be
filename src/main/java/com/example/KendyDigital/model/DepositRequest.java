package com.example.KendyDigital.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.KendyDigital.model.UserAccount;
import com.example.KendyDigital.common.TimestampedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
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

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "deposit_requests",
        indexes = {
                @Index(name = "idx_deposit_requests_user_id", columnList = "user_id"),
                @Index(name = "idx_deposit_requests_status", columnList = "status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_deposit_requests_code", columnNames = "deposit_code"),
                @UniqueConstraint(name = "uk_deposit_requests_bank_tx", columnNames = "matched_bank_transaction_id"),
                @UniqueConstraint(name = "uk_deposit_requests_wallet_tx", columnNames = "wallet_transaction_id")
        })
public class DepositRequest extends TimestampedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deposit_code", nullable = false)
    private String depositCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "bank_name", nullable = false)
    private String bankName;

    @Column(name = "bank_account", nullable = false)
    private String bankAccount;

    @Column(name = "bank_owner", nullable = false)
    private String bankOwner;

    @Column(name = "transfer_content", nullable = false)
    private String transferContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DepositStatus status = DepositStatus.PENDING;

    @Column(name = "expired_at", nullable = false)
    private Instant expiredAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_bank_transaction_id")
    private BankTransaction matchedBankTransaction;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_transaction_id")
    private WalletTransaction walletTransaction;

    public DepositRequest(String depositCode, UserAccount user, BigDecimal amount, String bankName,
            String bankAccount, String bankOwner, String transferContent, Instant expiredAt) {
        this.depositCode = depositCode;
        this.user = user;
        this.amount = amount;
        this.bankName = bankName;
        this.bankAccount = bankAccount;
        this.bankOwner = bankOwner;
        this.transferContent = transferContent;
        this.expiredAt = expiredAt;
    }

    public void complete(BankTransaction bankTransaction, WalletTransaction walletTransaction) {
        this.status = DepositStatus.COMPLETED;
        this.matchedBankTransaction = bankTransaction;
        this.walletTransaction = walletTransaction;
        this.completedAt = Instant.now();
    }

    public void markManualReview() {
        this.status = DepositStatus.MANUAL_REVIEW;
    }

    public void cancel() {
        this.status = DepositStatus.CANCELLED;
    }

    public void extendTo(Instant expiredAt) {
        this.expiredAt = expiredAt;
        if (this.status == DepositStatus.EXPIRED) {
            this.status = DepositStatus.PENDING;
        }
    }
}
