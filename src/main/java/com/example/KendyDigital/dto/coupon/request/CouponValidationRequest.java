package com.example.KendyDigital.dto.coupon.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CouponValidationRequest(
        @NotNull Long serviceId,
        @NotBlank String couponCode) {
}
