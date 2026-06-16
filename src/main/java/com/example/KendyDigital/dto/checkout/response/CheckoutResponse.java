package com.example.KendyDigital.dto.checkout.response;

import com.example.KendyDigital.dto.deposit.response.DepositResponse;
import com.example.KendyDigital.dto.order.response.OrderResponse;
import com.example.KendyDigital.model.checkout.CheckoutSession;
import com.example.KendyDigital.model.checkout.CheckoutStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record CheckoutResponse(
        String checkoutCode,
        CheckoutStatus status,
        Long serviceId,
        String serviceName,
        BigDecimal amount,
        BigDecimal originalAmount,
        BigDecimal discountAmount,
        String couponCode,
        String statusMessage,
        DepositResponse deposit,
        OrderResponse order,
        Instant checkedAt) {
    public static CheckoutResponse from(CheckoutSession checkout) {
        return new CheckoutResponse(
                checkout.getCheckoutCode(),
                checkout.getStatus(),
                checkout.getService().getId(),
                checkout.getService().getName(),
                checkout.getDepositRequest().getAmount(),
                checkout.getOriginalAmount(),
                checkout.getDiscountAmount(),
                checkout.getCouponCode(),
                checkout.getStatusMessage(),
                DepositResponse.from(checkout.getDepositRequest()),
                checkout.getOrder() == null ? null : OrderResponse.from(checkout.getOrder()),
                Instant.now());
    }
}
