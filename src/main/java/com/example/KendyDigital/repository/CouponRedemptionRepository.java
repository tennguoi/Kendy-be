package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.coupon.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {
    long countByCoupon_IdAndUser_Id(Long couponId, Long userId);

    boolean existsByOrder_Id(Long orderId);
}
