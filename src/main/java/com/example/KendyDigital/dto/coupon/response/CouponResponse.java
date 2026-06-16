package com.example.KendyDigital.dto.coupon.response;

import com.example.KendyDigital.model.coupon.Coupon;
import com.example.KendyDigital.model.coupon.CouponStatus;
import com.example.KendyDigital.model.coupon.CouponType;
import java.math.BigDecimal;
import java.time.Instant;

public record CouponResponse(
        Long id,
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
        Long serviceId,
        String serviceName,
        String adminNote,
        Instant createdAt) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
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
                coupon.getService() == null ? null : coupon.getService().getId(),
                coupon.getService() == null ? null : coupon.getService().getName(),
                coupon.getAdminNote(),
                coupon.getCreatedAt());
    }
}
