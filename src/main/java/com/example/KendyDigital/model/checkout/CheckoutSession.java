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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CheckoutStatus status = CheckoutStatus.PENDING_PAYMENT;

    public CheckoutSession(String checkoutCode, UserAccount user, ServiceItem service, DepositRequest depositRequest,
            String inputData, String idempotencyKey) {
        this.checkoutCode = checkoutCode;
        this.user = user;
        this.service = service;
        this.depositRequest = depositRequest;
        this.inputData = inputData;
        this.idempotencyKey = idempotencyKey;
    }

    public void markPaid() {
        this.status = CheckoutStatus.PAID;
    }

    public void attachOrder(OrderRecord order) {
        this.order = order;
        this.status = CheckoutStatus.ORDER_CREATED;
    }

    public void markExpired() {
        this.status = CheckoutStatus.EXPIRED;
    }

    public void markCancelled() {
        this.status = CheckoutStatus.CANCELLED;
    }
}
