package com.example.KendyDigital.model.coupon;

import com.example.KendyDigital.common.TimestampedEntity;
import com.example.KendyDigital.model.catalog.ServiceItem;
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
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "coupons",
        indexes = {
                @Index(name = "idx_coupons_status", columnList = "status"),
                @Index(name = "idx_coupons_code", columnList = "code")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_coupons_code", columnNames = "code")
        })
public class Coupon extends TimestampedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CouponType type;

    @Column(name = "discount_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal value;

    @Column(name = "max_discount_amount", precision = 18, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "min_order_amount", precision = 18, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "per_user_limit")
    private Integer perUserLimit;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CouponStatus status = CouponStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private ServiceItem service;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    public Coupon(String code, String name, CouponType type, BigDecimal value, BigDecimal maxDiscountAmount,
            BigDecimal minOrderAmount, Integer usageLimit, Integer perUserLimit, Instant startsAt, Instant endsAt,
            ServiceItem service, String adminNote) {
        this.code = normalizeCode(code);
        this.name = name;
        this.type = type;
        this.value = value;
        this.maxDiscountAmount = maxDiscountAmount;
        this.minOrderAmount = minOrderAmount;
        this.usageLimit = usageLimit;
        this.perUserLimit = perUserLimit;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.service = service;
        this.adminNote = adminNote;
    }

    public void update(String code, String name, CouponType type, BigDecimal value, BigDecimal maxDiscountAmount,
            BigDecimal minOrderAmount, Integer usageLimit, Integer perUserLimit, Instant startsAt, Instant endsAt,
            CouponStatus status, ServiceItem service, String adminNote) {
        this.code = normalizeCode(code);
        this.name = name;
        this.type = type;
        this.value = value;
        this.maxDiscountAmount = maxDiscountAmount;
        this.minOrderAmount = minOrderAmount;
        this.usageLimit = usageLimit;
        this.perUserLimit = perUserLimit;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = status;
        this.service = service;
        this.adminNote = adminNote;
    }

    public void enable() {
        this.status = CouponStatus.ACTIVE;
    }

    public void disable() {
        this.status = CouponStatus.DISABLED;
    }

    public void incrementUsedCount() {
        this.usedCount++;
    }

    public static String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
