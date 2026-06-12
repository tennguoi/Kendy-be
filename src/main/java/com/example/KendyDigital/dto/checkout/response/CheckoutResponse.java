package com.example.KendyDigital.dto.checkout.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.KendyDigital.dto.deposit.response.DepositResponse;
import com.example.KendyDigital.dto.order.response.OrderResponse;
import com.example.KendyDigital.model.CheckoutSession;
import com.example.KendyDigital.model.CheckoutStatus;

public record CheckoutResponse(
        String checkoutCode,
        CheckoutStatus status,
        Long serviceId,
        String serviceName,
        BigDecimal amount,
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
                DepositResponse.from(checkout.getDepositRequest()),
                checkout.getOrder() == null ? null : OrderResponse.from(checkout.getOrder()),
                Instant.now());
    }
}
