package com.example.KendyDigital.model.wallet;

import com.example.KendyDigital.common.TimestampedEntity;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "wallet_transactions",
        indexes = {
                @Index(name = "idx_wallet_transactions_user_id", columnList = "user_id"),
                @Index(name = "idx_wallet_transactions_created_at", columnList = "created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_wallet_transactions_code", columnNames = "transaction_code")
        })
public class WalletTransaction extends TimestampedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_code", nullable = false)
    private String transactionCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WalletTransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WalletTransactionDirection direction;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_before", nullable = false, precision = 18, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 18, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    private String description;

    @Column(name = "created_by")
    private Long createdBy;

    public WalletTransaction(String transactionCode, UserAccount user, WalletTransactionType type,
            WalletTransactionDirection direction, BigDecimal amount, BigDecimal balanceBefore,
            BigDecimal balanceAfter, String referenceType, Long referenceId, String description, Long createdBy) {
        this.transactionCode = transactionCode;
        this.user = user;
        this.type = type;
        this.direction = direction;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.description = description;
        this.createdBy = createdBy;
    }

}
