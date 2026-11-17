package com.example.KendyDigital.dto.coupon.response;

import com.example.KendyDigital.model.coupon.Coupon;
import com.example.KendyDigital.model.coupon.CouponStatus;
import com.example.KendyDigital.model.coupon.CouponType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CouponResponse(
        UUID id,
        String code,
        String name,
        CouponType type,
        BigDecimal value,
        BigDecimal maxDiscountAmount,
        BigDecimal minOrderAmount,
        Integer usageLimit,
        Integer perUserLimit,
        int usedCount,
        Instant startsAt,
        Instant endsAt,
        CouponStatus status,
        UUID serviceId,
        String serviceName,
        String adminNote,
        Instant createdAt,
        Long version) {

    public CouponResponse(
            UUID id, String code, String name, CouponType type, BigDecimal value,
            BigDecimal maxDiscountAmount, BigDecimal minOrderAmount, Integer usageLimit,
            Integer perUserLimit, int usedCount, Instant startsAt, Instant endsAt,
            CouponStatus status, UUID serviceId, String serviceName, String adminNote,
            Instant createdAt) {
        this(id, code, name, type, value, maxDiscountAmount, minOrderAmount,
                usageLimit, perUserLimit, usedCount, startsAt, endsAt, status,
                serviceId, serviceName, adminNote, createdAt, 0L);
    }

    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getPublicId(),
                coupon.getCode(),
                coupon.getName(),
                coupon.getType(),
                coupon.getValue(),
                coupon.getMaxDiscountAmount(),
                coupon.getMinOrderAmount(),
                coupon.getUsageLimit(),
                coupon.getPerUserLimit(),
                coupon.getUsedCount(),
                coupon.getStartsAt(),
                coupon.getEndsAt(),
                coupon.getStatus(),
                coupon.getService() == null ? null : coupon.getService().getPublicId(),
                coupon.getService() == null ? null : coupon.getService().getName(),
                coupon.getAdminNote(),
                coupon.getCreatedAt(),
                coupon.getVersion());
    }
}
