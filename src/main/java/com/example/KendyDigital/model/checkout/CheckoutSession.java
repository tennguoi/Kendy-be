package com.example.KendyDigital.model.checkout;

import com.example.KendyDigital.common.TimestampedEntity;
import com.example.KendyDigital.model.catalog.ServiceItem;
import com.example.KendyDigital.model.deposit.DepositRequest;
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
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "checkout_sessions",
        indexes = {
                @Index(name = "idx_checkout_sessions_user_id", columnList = "user_id"),
                @Index(name = "idx_checkout_sessions_status", columnList = "status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_checkout_sessions_code", columnNames = "checkout_code"),
                @UniqueConstraint(name = "uk_checkout_sessions_deposit", columnNames = "deposit_request_id"),
                @UniqueConstraint(name = "uk_checkout_sessions_order", columnNames = "order_id"),
                @UniqueConstraint(name = "uk_checkout_sessions_user_idempotency", columnNames = {"user_id", "idempotency_key"})
        })
public class CheckoutSession extends TimestampedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "checkout_code", nullable = false)
    private String checkoutCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceItem service;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deposit_request_id", nullable = false)
    private DepositRequest depositRequest;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private OrderRecord order;

    @Column(name = "input_data", columnDefinition = "TEXT")
    private String inputData;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "original_amount", precision = 18, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "discount_amount", precision = 18, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "coupon_code")
    private String couponCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CheckoutStatus status = CheckoutStatus.PENDING_PAYMENT;

    @Column(name = "status_message", columnDefinition = "TEXT")
    private String statusMessage;

    public CheckoutSession(String checkoutCode, UserAccount user, ServiceItem service, DepositRequest depositRequest,
            String inputData, String idempotencyKey) {
        this.checkoutCode = checkoutCode;
        this.user = user;
        this.service = service;
        this.depositRequest = depositRequest;
        this.inputData = inputData;
        this.idempotencyKey = idempotencyKey;
    }

    public void applyPricing(BigDecimal originalAmount, BigDecimal discountAmount, String couponCode) {
        this.originalAmount = originalAmount;
        this.discountAmount = discountAmount;
        this.couponCode = couponCode;
    }

    public void markPaid() {
        this.status = CheckoutStatus.PAID;
        this.statusMessage = null;
    }

    public void attachOrder(OrderRecord order) {
        this.order = order;
        this.status = CheckoutStatus.ORDER_CREATED;
        this.statusMessage = null;
    }

    public void replaceDepositRequest(DepositRequest depositRequest) {
        this.depositRequest = depositRequest;
    }

    public void markPendingPayment() {
        this.status = CheckoutStatus.PENDING_PAYMENT;
    }

    public void markWalletCredited(String statusMessage) {
        this.status = CheckoutStatus.WALLET_CREDITED;
        this.statusMessage = statusMessage;
    }

    public void markExpired() {
        this.status = CheckoutStatus.EXPIRED;
        this.statusMessage = null;
    }

    public void markCancelled() {
        this.status = CheckoutStatus.CANCELLED;
        this.statusMessage = null;
    }
}
