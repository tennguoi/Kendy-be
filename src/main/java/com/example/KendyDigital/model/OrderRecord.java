package com.example.KendyDigital.model;

import java.math.BigDecimal;
import java.time.Instant;

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
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_user_id", columnList = "user_id"),
                @Index(name = "idx_orders_status", columnList = "status"),
                @Index(name = "idx_orders_created_at", columnList = "created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_orders_order_code", columnNames = "order_code"),
                @UniqueConstraint(name = "uk_orders_user_idempotency", columnNames = {"user_id", "idempotency_key"}),
                @UniqueConstraint(name = "uk_orders_wallet_tx", columnNames = "wallet_transaction_id"),
                @UniqueConstraint(name = "uk_orders_refund_tx", columnNames = "refund_transaction_id")
        })
public class OrderRecord extends TimestampedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceItem service;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "input_data", columnDefinition = "TEXT")
    private String inputData;

    @Column(name = "result_data", columnDefinition = "TEXT")
    private String resultData;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status = OrderStatus.PROCESSING;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_transaction_id")
    private WalletTransaction walletTransaction;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_transaction_id")
    private WalletTransaction refundTransaction;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "user_note", columnDefinition = "TEXT")
    private String userNote;

    @Column(name = "processing_at")
    private Instant processingAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    public OrderRecord(String orderCode, UserAccount user, ServiceItem service, BigDecimal amount, String inputData,
            String idempotencyKey) {
        this.orderCode = orderCode;
        this.user = user;
        this.service = service;
        this.amount = amount;
        this.inputData = inputData;
        this.idempotencyKey = idempotencyKey;
    }

    public void attachPurchaseTransaction(WalletTransaction walletTransaction) {
        this.walletTransaction = walletTransaction;
    }

    public void refund(WalletTransaction refundTransaction, String reason) {
        this.status = OrderStatus.REFUNDED;
        this.refundTransaction = refundTransaction;
        this.adminNote = reason;
        this.cancelledAt = Instant.now();
    }

    public void complete(String resultData, String adminNote) {
        this.status = OrderStatus.COMPLETED;
        this.resultData = resultData;
        this.adminNote = adminNote;
        this.completedAt = Instant.now();
    }

    public void fail(String resultData, String adminNote) {
        this.status = OrderStatus.FAILED;
        this.resultData = resultData;
        this.adminNote = adminNote;
        this.cancelledAt = Instant.now();
    }

    public void cancelByUser(String reason, WalletTransaction refundTransaction) {
        this.status = OrderStatus.CANCELLED;
        this.userNote = reason;
        this.refundTransaction = refundTransaction;
        this.cancelledAt = Instant.now();
    }

    public void cancelByAdmin(String reason, WalletTransaction refundTransaction) {
        this.status = OrderStatus.CANCELLED;
        this.adminNote = reason;
        this.refundTransaction = refundTransaction;
        this.cancelledAt = Instant.now();
    }
}
