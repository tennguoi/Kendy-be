package com.example.KendyDigital.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.KendyDigital.dto.checkout.request.CreateServiceCheckoutRequest;
import com.example.KendyDigital.dto.checkout.response.CheckoutResponse;
import com.example.KendyDigital.security.CurrentUser;
import com.example.KendyDigital.service.CheckoutService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {
    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/service")
    public CheckoutResponse createServiceCheckout(Authentication authentication,
            @Valid @RequestBody CreateServiceCheckoutRequest request) {
        return checkoutService.createServiceCheckout(CurrentUser.require(authentication).userId(), request);
    }

    @GetMapping("/{checkoutCode}/status")
    public CheckoutResponse getCheckoutStatus(Authentication authentication, @PathVariable String checkoutCode) {
        return checkoutService.getStatus(CurrentUser.require(authentication).userId(), checkoutCode);
    }
}
