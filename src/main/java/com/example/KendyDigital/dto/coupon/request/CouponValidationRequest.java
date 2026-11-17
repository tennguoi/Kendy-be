package com.example.KendyDigital.dto.coupon.request;


import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CouponValidationRequest(
        @NotNull UUID serviceId,
        @NotBlank @Size(max = 50) String couponCode) {
}
