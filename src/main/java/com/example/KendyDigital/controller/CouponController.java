package com.example.KendyDigital.controller;

import com.example.KendyDigital.dto.coupon.request.CouponValidationRequest;
import com.example.KendyDigital.dto.coupon.request.UpsertCouponRequest;
import com.example.KendyDigital.dto.coupon.response.CouponResponse;
import com.example.KendyDigital.dto.coupon.response.CouponValidationResponse;
import com.example.KendyDigital.model.coupon.CouponStatus;
import com.example.KendyDigital.security.CurrentUser;
import com.example.KendyDigital.service.coupon.CouponService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CouponController {
    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping("/api/coupons/validate")
    public CouponValidationResponse validate(Authentication authentication,
            @Valid @RequestBody CouponValidationRequest request) {
        return couponService.validate(CurrentUser.require(authentication).userId(), request);
    }

    @GetMapping("/api/admin/coupons")
    public List<CouponResponse> listForAdmin(@RequestParam(required = false) CouponStatus status,
            @RequestParam(required = false) Integer limit) {
        return couponService.listForAdmin(status, limit);
    }

    @PostMapping("/api/admin/coupons")
    public CouponResponse create(Authentication authentication, @Valid @RequestBody UpsertCouponRequest request) {
        return couponService.create(CurrentUser.require(authentication).userId(), request);
    }

    @PutMapping("/api/admin/coupons/{couponId}")
    public CouponResponse update(Authentication authentication, @PathVariable Long couponId,
            @Valid @RequestBody UpsertCouponRequest request) {
        return couponService.update(CurrentUser.require(authentication).userId(), couponId, request);
    }

    @PostMapping("/api/admin/coupons/{couponId}/enable")
    public CouponResponse enable(Authentication authentication, @PathVariable Long couponId) {
        return couponService.setStatus(CurrentUser.require(authentication).userId(), couponId, CouponStatus.ACTIVE);
    }

    @PostMapping("/api/admin/coupons/{couponId}/disable")
    public CouponResponse disable(Authentication authentication, @PathVariable Long couponId) {
        return couponService.setStatus(CurrentUser.require(authentication).userId(), couponId, CouponStatus.DISABLED);
    }
}
