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
        name = "bank_transactions",
        indexes = {
                @Index(name = "idx_bank_transactions_status", columnList = "status"),
                @Index(name = "idx_bank_transactions_received_at", columnList = "received_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_bank_transactions_sepay_id", columnNames = "sepay_id"),
                @UniqueConstraint(name = "uk_bank_transactions_reference", columnNames = "reference_code"),
                @UniqueConstraint(name = "uk_bank_transactions_wallet_tx", columnNames = "wallet_transaction_id")
        })
public class BankTransaction extends TimestampedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sepay_id")
    private Long sepayId;

    private String gateway;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "sub_account")
    private String subAccount;

    @Column(name = "transaction_date")
    private Instant transactionDate;

    @Column(name = "transfer_type")
    private String transferType;

    @Column(name = "transfer_amount", precision = 18, scale = 2)
    private BigDecimal transferAmount;

    @Column(precision = 18, scale = 2)
    private BigDecimal accumulated;

    private String code;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "reference_code")
    private String referenceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BankTransactionStatus status = BankTransactionStatus.NEW;

    @Column(name = "review_reason")
    private String reviewReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_user_id")
    private UserAccount matchedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_deposit_request_id")
    private DepositRequest matchedDepositRequest;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_transaction_id")
    private WalletTransaction walletTransaction;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();

    @Column(name = "credited_at")
    private Instant creditedAt;

    public void ignore(String reason) {
        this.status = BankTransactionStatus.IGNORED;
        this.reviewReason = reason;
    }

    public void manualReview(String reason, DepositRequest depositRequest) {
        this.status = BankTransactionStatus.MANUAL_REVIEW;
        this.reviewReason = reason;
        this.matchedDepositRequest = depositRequest;
        if (depositRequest != null) {
            this.matchedUser = depositRequest.getUser();
        }
    }

    public void match(DepositRequest depositRequest, String reason) {
        this.status = BankTransactionStatus.MATCHED;
        this.reviewReason = reason;
        this.matchedDepositRequest = depositRequest;
        this.matchedUser = depositRequest.getUser();
    }

    public void credit(DepositRequest depositRequest, WalletTransaction walletTransaction) {
        this.status = BankTransactionStatus.CREDITED;
        this.matchedDepositRequest = depositRequest;
        this.matchedUser = depositRequest.getUser();
        this.walletTransaction = walletTransaction;
        this.creditedAt = Instant.now();
    }

    public void creditManually(UserAccount user, DepositRequest depositRequest, WalletTransaction walletTransaction,
            String reason) {
        this.status = BankTransactionStatus.CREDITED;
        this.matchedUser = user;
        this.matchedDepositRequest = depositRequest;
        this.walletTransaction = walletTransaction;
        this.reviewReason = reason;
        this.creditedAt = Instant.now();
    }
}
