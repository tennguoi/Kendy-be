package com.example.KendyDigital.dto.coupon;

import com.example.KendyDigital.model.coupon.Coupon;
import java.math.BigDecimal;

public record AppliedCoupon(
        Coupon coupon,
        BigDecimal originalAmount,
        BigDecimal discountAmount,
        BigDecimal payableAmount,
        boolean acquiredFromRedis) {
    public AppliedCoupon(Coupon coupon, BigDecimal originalAmount, BigDecimal discountAmount, BigDecimal payableAmount) {
        this(coupon, originalAmount, discountAmount, payableAmount, false);
    }

    public String code() {
        return coupon == null ? null : coupon.getCode();
    }
}
