package com.example.KendyDigital.service.coupon;

import com.example.KendyDigital.dto.coupon.request.CouponValidationRequest;
import com.example.KendyDigital.dto.coupon.request.UpsertCouponRequest;
import com.example.KendyDigital.dto.coupon.response.CouponResponse;
import com.example.KendyDigital.dto.coupon.response.CouponValidationResponse;
import com.example.KendyDigital.model.catalog.ServiceItem;
import com.example.KendyDigital.model.coupon.CouponStatus;
import com.example.KendyDigital.model.order.OrderRecord;
import com.example.KendyDigital.model.user.UserAccount;
import java.math.BigDecimal;
import java.util.List;

public interface CouponService {
    List<CouponResponse> listForAdmin(CouponStatus status, Integer limit);
    CouponResponse create(Long adminUserId, UpsertCouponRequest request);
    CouponResponse update(Long adminUserId, Long couponId, UpsertCouponRequest request);
    CouponResponse setStatus(Long adminUserId, Long couponId, CouponStatus status);
    CouponValidationResponse validate(Long userId, CouponValidationRequest request);
    AppliedCoupon applyForPurchase(UserAccount user, ServiceItem service, BigDecimal originalAmount, String couponCode);
    void redeemForOrder(UserAccount user, OrderRecord order, AppliedCoupon appliedCoupon);
}
