package com.example.KendyDigital.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.KendyDigital.dto.AdminOrderUpdateRequest;
import com.example.KendyDigital.dto.CancelOrderRequest;
import com.example.KendyDigital.dto.CreateOrderRequest;
import com.example.KendyDigital.dto.OrderResponse;
import com.example.KendyDigital.dto.RefundOrderRequest;
import com.example.KendyDigital.model.OrderStatus;
import com.example.KendyDigital.security.CurrentUser;
import com.example.KendyDigital.service.OrderService;

import jakarta.validation.Valid;

@RestController
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/api/orders")
    public OrderResponse create(Authentication authentication, @Valid @RequestBody CreateOrderRequest request) {
        return orderService.create(CurrentUser.require(authentication).userId(), request);
    }

    @GetMapping("/api/orders")
    public List<OrderResponse> listByUser(Authentication authentication,
            @RequestParam(required = false) OrderStatus status) {
        return orderService.listByUser(CurrentUser.require(authentication).userId(), status);
    }

    @GetMapping("/api/orders/{orderCode}")
    public OrderResponse getByCode(Authentication authentication, @PathVariable String orderCode) {
        return orderService.getByCodeForUser(CurrentUser.require(authentication).userId(), orderCode);
    }

    @PostMapping("/api/orders/{orderCode}/cancel")
    public OrderResponse cancelForUser(Authentication authentication, @PathVariable String orderCode,
            @Valid @RequestBody CancelOrderRequest request) {
        return orderService.cancelForUser(CurrentUser.require(authentication).userId(), orderCode, request);
    }

    @GetMapping("/api/admin/orders")
    public List<OrderResponse> listForAdmin(@RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long userId) {
        return orderService.listForAdmin(status, userId);
    }

    @GetMapping("/api/admin/orders/{orderCode}")
    public OrderResponse getByCodeForAdmin(@PathVariable String orderCode) {
        return orderService.getByCodeForAdmin(orderCode);
    }

    @PostMapping("/api/admin/orders/{orderCode}/complete")
    public OrderResponse complete(Authentication authentication, @PathVariable String orderCode,
            @RequestBody AdminOrderUpdateRequest request) {
        return orderService.complete(orderCode, CurrentUser.require(authentication).userId(), request);
    }

    @PostMapping("/api/admin/orders/{orderCode}/fail")
    public OrderResponse fail(Authentication authentication, @PathVariable String orderCode,
            @RequestBody AdminOrderUpdateRequest request) {
        return orderService.fail(orderCode, CurrentUser.require(authentication).userId(), request);
    }

    @PostMapping("/api/admin/orders/{orderCode}/cancel")
    public OrderResponse cancelByAdmin(Authentication authentication, @PathVariable String orderCode,
            @Valid @RequestBody CancelOrderRequest request) {
        return orderService.cancelByAdmin(orderCode, CurrentUser.require(authentication).userId(), request);
    }

    @PostMapping("/api/admin/orders/{orderCode}/refund")
    public OrderResponse refund(Authentication authentication, @PathVariable String orderCode,
            @Valid @RequestBody RefundOrderRequest request) {
        return orderService.refund(orderCode, CurrentUser.require(authentication).userId(), request);
    }
}
