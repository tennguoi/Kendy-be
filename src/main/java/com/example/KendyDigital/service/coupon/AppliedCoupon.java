package com.example.KendyDigital.service.coupon;

import com.example.KendyDigital.model.coupon.Coupon;
import java.math.BigDecimal;

public record AppliedCoupon(
        Coupon coupon,
        BigDecimal originalAmount,
        BigDecimal discountAmount,
        BigDecimal payableAmount) {
    public String code() {
        return coupon == null ? null : coupon.getCode();
    }
}
