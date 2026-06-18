package com.example.KendyDigital.service.coupon;

import com.example.KendyDigital.dto.coupon.request.CouponValidationRequest;
import com.example.KendyDigital.dto.coupon.request.UpsertCouponRequest;
import com.example.KendyDigital.dto.coupon.response.CouponResponse;
import com.example.KendyDigital.dto.coupon.response.CouponValidationResponse;
import com.example.KendyDigital.model.catalog.ServiceItem;
import com.example.KendyDigital.model.coupon.Coupon;
import com.example.KendyDigital.model.coupon.CouponRedemption;
import com.example.KendyDigital.model.coupon.CouponStatus;
import com.example.KendyDigital.model.coupon.CouponType;
import com.example.KendyDigital.model.order.OrderRecord;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.repository.CouponRedemptionRepository;
import com.example.KendyDigital.repository.CouponRepository;
import com.example.KendyDigital.repository.ServiceItemRepository;
import com.example.KendyDigital.service.audit.AuditService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CouponServiceImpl implements CouponService {
    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final AuditService auditService;

    public CouponServiceImpl(CouponRepository couponRepository,
            CouponRedemptionRepository couponRedemptionRepository,
            ServiceItemRepository serviceItemRepository,
            AuditService auditService) {
        this.couponRepository = couponRepository;
        this.couponRedemptionRepository = couponRedemptionRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> listForAdmin(CouponStatus status, Integer limit) {
        PageRequest page = PageRequest.of(0, limit == null ? 100 : Math.max(1, Math.min(limit, 200)));
        List<Coupon> coupons = status == null
                ? couponRepository.findAllByOrderByCreatedAtDesc(page)
                : couponRepository.findAllByStatusOrderByCreatedAtDesc(status, page);
        return coupons.stream().map(CouponResponse::from).toList();
    }

    @Transactional
    public CouponResponse create(Long adminUserId, UpsertCouponRequest request) {
        String code = Coupon.normalizeCode(request.code());
        if (couponRepository.existsByCodeIgnoreCase(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Coupon code already exists");
        }
        ServiceItem service = resolveService(request.serviceId());
        Coupon coupon = couponRepository.save(new Coupon(
                code,
                request.name().trim(),
                request.type(),
                money(request.value()),
                nullableMoney(request.maxDiscountAmount()),
                nullableMoney(request.minOrderAmount()),
                positiveOrNull(request.usageLimit()),
                positiveOrNull(request.perUserLimit()),
                request.startsAt(),
                request.endsAt(),
                service,
                blankToNull(request.adminNote())));
        if (request.status() == CouponStatus.DISABLED) {
            coupon.disable();
        }
        auditService.recordAdmin(adminUserId, "COUPON_CREATED", "COUPON", coupon.getId(), "code=" + coupon.getCode());
        return CouponResponse.from(coupon);
    }

    @Transactional
    public CouponResponse update(Long adminUserId, Long couponId, UpsertCouponRequest request) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coupon not found"));
        String code = Coupon.normalizeCode(request.code());
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon code is required");
        }
        couponRepository.findByCodeIgnoreCase(code)
                .filter(existing -> !existing.getId().equals(couponId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Coupon code already exists");
                });
        coupon.update(
                code,
                request.name().trim(),
                request.type(),
                money(request.value()),
                nullableMoney(request.maxDiscountAmount()),
                nullableMoney(request.minOrderAmount()),
                positiveOrNull(request.usageLimit()),
                positiveOrNull(request.perUserLimit()),
                request.startsAt(),
                request.endsAt(),
                request.status() == null ? coupon.getStatus() : request.status(),
                resolveService(request.serviceId()),
                blankToNull(request.adminNote()));
        auditService.recordAdmin(adminUserId, "COUPON_UPDATED", "COUPON", coupon.getId(), "code=" + coupon.getCode());
        return CouponResponse.from(coupon);
    }

    @Transactional
    public CouponResponse setStatus(Long adminUserId, Long couponId, CouponStatus status) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coupon not found"));
        if (status == CouponStatus.ACTIVE) {
            coupon.enable();
        } else {
            coupon.disable();
        }
        auditService.recordAdmin(adminUserId, "COUPON_STATUS_UPDATED", "COUPON", coupon.getId(),
                "status=" + coupon.getStatus());
        return CouponResponse.from(coupon);
    }

    @Transactional(readOnly = true)
    public CouponValidationResponse validate(Long userId, CouponValidationRequest request) {
        ServiceItem service = serviceItemRepository.findById(request.serviceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
        BigDecimal originalAmount = money(service.getPrice());
        try {
            Coupon coupon = couponRepository.findByCodeIgnoreCase(Coupon.normalizeCode(request.couponCode()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coupon not found"));
            validateCoupon(coupon, userId, service.getId(), originalAmount);
            BigDecimal discount = calculateDiscount(coupon, originalAmount);
            BigDecimal payable = originalAmount.subtract(discount).max(BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);
            ensurePositivePayable(payable);
            return new CouponValidationResponse(
                    true,
                    coupon.getCode(),
                    "Coupon applied",
                    originalAmount,
                    discount,
                    payable);
        } catch (ResponseStatusException exception) {
            return CouponValidationResponse.invalid(request.couponCode(), exception.getReason(), originalAmount);
        }
    }

    @Transactional
    public AppliedCoupon applyForPurchase(UserAccount user, ServiceItem service, BigDecimal originalAmount,
            String couponCode) {
        if (couponCode == null || couponCode.isBlank()) {
            BigDecimal amount = money(originalAmount);
            return new AppliedCoupon(null, amount, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), amount);
        }
        BigDecimal normalizedOriginal = money(originalAmount);
        Coupon coupon = couponRepository.findByCodeForUpdate(Coupon.normalizeCode(couponCode))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coupon not found"));
        validateCoupon(coupon, user.getId(), service.getId(), normalizedOriginal);
        BigDecimal discount = calculateDiscount(coupon, normalizedOriginal);
        BigDecimal payable = normalizedOriginal.subtract(discount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        ensurePositivePayable(payable);
        return new AppliedCoupon(coupon, normalizedOriginal, discount, payable);
    }

    @Transactional
    public void redeemForOrder(UserAccount user, OrderRecord order, AppliedCoupon appliedCoupon) {
        if (appliedCoupon == null || appliedCoupon.coupon() == null) {
            return;
        }
        if (couponRedemptionRepository.existsByOrder_Id(order.getId())) {
            return;
        }
        Coupon coupon = appliedCoupon.coupon();
        validateCoupon(coupon, user.getId(), order.getService().getId(), appliedCoupon.originalAmount());
        coupon.incrementUsedCount();
        couponRedemptionRepository.save(new CouponRedemption(
                coupon,
                user,
                order,
                appliedCoupon.originalAmount(),
                appliedCoupon.discountAmount(),
                appliedCoupon.payableAmount()));
        auditService.recordSystem("COUPON_REDEEMED", "COUPON", coupon.getId(),
                "orderId=" + order.getId() + ",code=" + coupon.getCode());
    }

    private void validateCoupon(Coupon coupon, Long userId, Long serviceId, BigDecimal originalAmount) {
        Instant now = Instant.now();
        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Coupon is disabled");
        }
        if (coupon.getStartsAt() != null && coupon.getStartsAt().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Coupon is not active yet");
        }
        if (coupon.getEndsAt() != null && coupon.getEndsAt().isBefore(now)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Coupon has expired");
        }
        if (coupon.getService() != null && !coupon.getService().getId().equals(serviceId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Coupon does not apply to this service");
        }
        if (coupon.getMinOrderAmount() != null && originalAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order amount is below coupon minimum");
        }
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Coupon usage limit reached");
        }
        if (coupon.getPerUserLimit() != null
                && couponRedemptionRepository.countByCoupon_IdAndUser_Id(coupon.getId(), userId) >= coupon.getPerUserLimit()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Coupon user limit reached");
        }
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal originalAmount) {
        BigDecimal discount = coupon.getType() == CouponType.PERCENT
                ? originalAmount.multiply(coupon.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : coupon.getValue();
        if (coupon.getMaxDiscountAmount() != null) {
            discount = discount.min(coupon.getMaxDiscountAmount());
        }
        return discount.min(originalAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private void ensurePositivePayable(BigDecimal payable) {
        if (payable.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Coupon discount cannot cover the full order amount");
        }
    }

    private ServiceItem resolveService(Long serviceId) {
        if (serviceId == null) {
            return null;
        }
        return serviceItemRepository.findById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
    }

    private BigDecimal money(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nullableMoney(BigDecimal value) {
        return value == null ? null : money(value);
    }

    private Integer positiveOrNull(Integer value) {
        return value == null || value <= 0 ? null : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
