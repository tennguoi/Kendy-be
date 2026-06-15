package com.example.KendyDigital.service.checkout;

import com.example.KendyDigital.dto.checkout.request.CreateServiceCheckoutRequest;
import com.example.KendyDigital.dto.checkout.response.CheckoutResponse;

public interface CheckoutService {
    CheckoutResponse createServiceCheckout(Long userId, CreateServiceCheckoutRequest request);
    CheckoutResponse getStatus(Long userId, String checkoutCode);
}
