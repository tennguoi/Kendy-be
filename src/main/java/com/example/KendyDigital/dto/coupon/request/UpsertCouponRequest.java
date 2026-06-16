package com.example.KendyDigital.dto.coupon.request;

import com.example.KendyDigital.model.coupon.CouponStatus;
import com.example.KendyDigital.model.coupon.CouponType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record UpsertCouponRequest(
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 255) String name,
        @NotNull CouponType type,
        @NotNull @DecimalMin("0.01") BigDecimal value,
        BigDecimal maxDiscountAmount,
        BigDecimal minOrderAmount,
        Integer usageLimit,
        Integer perUserLimit,
        Instant startsAt,
        Instant endsAt,
        CouponStatus status,
        Long serviceId,
        String adminNote) {
}
