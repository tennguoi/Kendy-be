package com.example.KendyDigital.dto.coupon.response;

import java.math.BigDecimal;

public record CouponValidationResponse(
        boolean valid,
        String couponCode,
        String message,
        BigDecimal originalAmount,
        BigDecimal discountAmount,
        BigDecimal payableAmount) {
    public static CouponValidationResponse invalid(String couponCode, String message, BigDecimal originalAmount) {
        BigDecimal amount = originalAmount == null ? BigDecimal.ZERO : originalAmount;
        return new CouponValidationResponse(false, couponCode, message, amount, BigDecimal.ZERO, amount);
    }
}
