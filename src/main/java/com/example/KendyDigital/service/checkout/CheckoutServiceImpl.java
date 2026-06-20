package com.example.KendyDigital.service.checkout;

import com.example.KendyDigital.common.CodeGenerator;
import com.example.KendyDigital.dto.checkout.request.CreateServiceCheckoutRequest;
import com.example.KendyDigital.dto.checkout.response.CheckoutResponse;
import com.example.KendyDigital.dto.order.request.CreateOrderRequest;
import com.example.KendyDigital.model.catalog.ServiceCtaType;
import com.example.KendyDigital.model.catalog.ServiceItem;
import com.example.KendyDigital.model.catalog.ServiceStatus;
import com.example.KendyDigital.model.catalog.ServiceStockStatus;
import com.example.KendyDigital.model.catalog.ServiceType;
import com.example.KendyDigital.model.admin.AdminNotification;
import com.example.KendyDigital.model.checkout.CheckoutSession;
import com.example.KendyDigital.model.checkout.CheckoutStatus;
import com.example.KendyDigital.model.deposit.DepositRequest;
import com.example.KendyDigital.model.deposit.DepositStatus;
import com.example.KendyDigital.model.order.OrderRecord;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserStatus;
import com.example.KendyDigital.repository.CheckoutSessionRepository;
import com.example.KendyDigital.repository.DepositRequestRepository;
import com.example.KendyDigital.repository.OrderRepository;
import com.example.KendyDigital.repository.AdminNotificationRepository;
import com.example.KendyDigital.repository.ServiceItemRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.service.audit.AuditService;
import com.example.KendyDigital.service.coupon.AppliedCoupon;
import com.example.KendyDigital.service.coupon.CouponService;
import com.example.KendyDigital.service.deposit.DepositService;
import com.example.KendyDigital.service.inventory.AccountInventoryService;
import com.example.KendyDigital.service.notification.UserNotificationService;
import com.example.KendyDigital.service.order.OrderService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CheckoutServiceImpl  implements CheckoutService{
    private final CheckoutSessionRepository checkoutSessionRepository;
    private final DepositRequestRepository depositRequestRepository;
    private final OrderRepository orderRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final UserAccountRepository userAccountRepository;
    private final DepositService depositService;
    private final OrderService orderService;
    private final CodeGenerator codeGenerator;
    private final AuditService auditService;
    private final AccountInventoryService accountInventoryService;
    private final UserNotificationService userNotificationService;
    private final AdminNotificationRepository adminNotificationRepository;
    private final CouponService couponService;

    public CheckoutServiceImpl(CheckoutSessionRepository checkoutSessionRepository,
            DepositRequestRepository depositRequestRepository,
            OrderRepository orderRepository,
            AdminNotificationRepository adminNotificationRepository,
            ServiceItemRepository serviceItemRepository,
            UserAccountRepository userAccountRepository,
            DepositService depositService,
            OrderService orderService,
            CodeGenerator codeGenerator,
            AuditService auditService,
            AccountInventoryService accountInventoryService,
            UserNotificationService userNotificationService,
            CouponService couponService) {
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.depositRequestRepository = depositRequestRepository;
        this.orderRepository = orderRepository;
        this.adminNotificationRepository = adminNotificationRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.userAccountRepository = userAccountRepository;
        this.depositService = depositService;
        this.orderService = orderService;
        this.codeGenerator = codeGenerator;
        this.auditService = auditService;
        this.accountInventoryService = accountInventoryService;
        this.userNotificationService = userNotificationService;
        this.couponService = couponService;
    }

    @Transactional
    public CheckoutResponse createServiceCheckout(Long userId, CreateServiceCheckoutRequest request) {
        String idempotencyKey = blankToNull(request.idempotencyKey());
        if (idempotencyKey != null) {
            CheckoutSession existing = checkoutSessionRepository
                    .findByUser_IdAndIdempotencyKey(userId, idempotencyKey)
                    .orElse(null);
            if (existing != null) {
                return CheckoutResponse.from(existing);
            }
        }

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not active");
        }

        ServiceItem service = serviceItemRepository.findById(request.serviceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
        validateServiceForCheckout(service);

        BigDecimal originalAmount = service.getPrice().setScale(2, RoundingMode.HALF_UP);
        AppliedCoupon appliedCoupon = couponService.applyForPurchase(user, service, originalAmount,
                request.couponCode());
        if (appliedCoupon.payableAmount().compareTo(BigDecimal.valueOf(1000)) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Transfer checkout amount must be greater than 1,000 VND");
        }
        DepositRequest deposit = depositService.createDepositForAmount(userId, appliedCoupon.payableAmount());
        CheckoutSession checkout = checkoutSessionRepository.save(new CheckoutSession(
                nextCheckoutCode(),
                user,
                service,
                deposit,
                request.inputData(),
                idempotencyKey));
        checkout.applyPricing(appliedCoupon.originalAmount(), appliedCoupon.discountAmount(), appliedCoupon.code());
        if (service.getType() == ServiceType.ACCOUNT_STOCK) {
            accountInventoryService.reserveForCheckout(service.getId(), checkout, user, deposit.getExpiredAt());
        }

        auditService.recordSystem(
                "CHECKOUT_CREATED",
                "CHECKOUT_SESSION",
                checkout.getId(),
                "depositId=" + deposit.getId() + ",serviceId=" + service.getId());
        return CheckoutResponse.from(checkout);
    }

    @Transactional
    public CheckoutResponse getStatus(Long userId, String checkoutCode) {
        CheckoutSession checkout = checkoutSessionRepository.findByCheckoutCodeForUpdate(checkoutCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Checkout not found"));
        if (!checkout.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Checkout not found");
        }

        refreshCheckoutStatus(checkout);
        return CheckoutResponse.from(checkout);
    }

    @Transactional
    public CheckoutResponse completePaidDeposit(Long depositId) {
        CheckoutSession checkout = checkoutSessionRepository.findByDepositRequestIdForUpdate(depositId)
                .orElse(null);
        if (checkout == null) {
            return null;
        }
        refreshCheckoutStatus(checkout);
        return CheckoutResponse.from(checkout);
    }

    private void refreshCheckoutStatus(CheckoutSession checkout) {
        if (checkout.getOrder() != null) {
            checkout.attachOrder(checkout.getOrder());
            return;
        }
        if (checkout.getStatus() == CheckoutStatus.WALLET_CREDITED) {
            return;
        }

        DepositRequest deposit = depositRequestRepository.findByDepositCodeForUpdate(checkout.getDepositRequest().getDepositCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deposit not found"));
        checkout.replaceDepositRequest(deposit);

        if (deposit.getStatus() == DepositStatus.CANCELLED) {
            accountInventoryService.releaseReservationForCheckout(checkout.getId());
            checkout.markCancelled();
            return;
        }

        if (deposit.getStatus() == DepositStatus.EXPIRED || deposit.getExpiredAt().isBefore(Instant.now())) {
            accountInventoryService.releaseReservationForCheckout(checkout.getId());
            checkout.markExpired();
            return;
        }

        if (deposit.getStatus() != DepositStatus.COMPLETED) {
            checkout.markPendingPayment();
            return;
        }

        if (checkout.getService().getType() == ServiceType.ACCOUNT_STOCK
                && !accountInventoryService.hasUsableReservation(
                        checkout.getService().getId(),
                        checkout.getId(),
                        checkout.getUser().getId(),
                        Instant.now())) {
            markWalletCredited(checkout, "Reserved account credential is no longer available");
            return;
        }

        checkout.markPaid();
        OrderRecord order = createOrderForPaidCheckout(checkout);
        checkout.attachOrder(order);
        auditService.recordSystem(
                "CHECKOUT_ORDER_CREATED",
                "CHECKOUT_SESSION",
                checkout.getId(),
                "orderId=" + order.getId() + ",depositId=" + deposit.getId());
    }

    private void markWalletCredited(CheckoutSession checkout, String reason) {
        accountInventoryService.releaseReservationForCheckout(checkout.getId());
        String message = "Thanh toán đã được cộng vào ví, nhưng đơn "
                + checkout.getService().getName()
                + " chưa được tạo tự động. Lý do: "
                + (reason == null || reason.isBlank() ? "không còn hàng khả dụng" : reason)
                + ". Bạn có thể đặt lại bằng số dư ví hoặc liên hệ admin hỗ trợ.";
        checkout.markWalletCredited(message);
        userNotificationService.create(
                checkout.getUser().getId(),
                "Tiền đã vào ví, đơn chưa tạo",
                message,
                "DEPOSIT",
                "/wallet");
        adminNotificationRepository.save(new AdminNotification(null,
                "Checkout đã cộng ví nhưng chưa tạo đơn",
                "Checkout " + checkout.getCheckoutCode()
                        + " của user #" + checkout.getUser().getId()
                        + " đã thanh toán cho " + checkout.getService().getName()
                        + " nhưng order chưa tạo. Lý do: "
                        + (reason == null || reason.isBlank() ? "không xác định" : reason)));
        auditService.recordSystem(
                "CHECKOUT_WALLET_CREDITED_WITHOUT_ORDER",
                "CHECKOUT_SESSION",
                checkout.getId(),
                "serviceId=" + checkout.getService().getId()
                        + ",depositId=" + checkout.getDepositRequest().getId()
                        + ",reason=" + reason);
    }

    private OrderRecord createOrderForPaidCheckout(CheckoutSession checkout) {
        String idempotencyKey = checkout.getIdempotencyKey() == null
                ? "checkout:" + checkout.getCheckoutCode()
                : checkout.getIdempotencyKey();
        orderService.createForCheckout(checkout.getUser().getId(), new CreateOrderRequest(
                checkout.getService().getId(),
                checkout.getInputData(),
                idempotencyKey,
                checkout.getCouponCode()), checkout.getId());
        return orderRepository.findByUser_IdAndIdempotencyKey(checkout.getUser().getId(), idempotencyKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Checkout order not found"));
    }

    private void validateServiceForCheckout(ServiceItem service) {
        if (service.getStatus() != ServiceStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Service is not active");
        }
        if (service.getType() == ServiceType.ACCOUNT_STOCK) {
            accountInventoryService.releaseExpiredReservations(Instant.now());
        }
        if (service.getStockStatus() == ServiceStockStatus.OUT_OF_STOCK) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Service is out of stock");
        }
        if (service.getStockStatus() == ServiceStockStatus.CONSULTING_ONLY) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Service requires consultation before purchase");
        }
        if (service.getCtaType() != null && service.getCtaType() != ServiceCtaType.BUY_NOW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Service requires consultation before purchase");
        }
        if (service.getType() == ServiceType.ACCOUNT_STOCK && accountInventoryService.availableCount(service.getId()) <= 0) {
            service.updatePricingMetadata(ServiceStockStatus.OUT_OF_STOCK, service.getCtaType(),
                    service.getPricingBadge(), service.isFeatured(), service.isPublicVisible());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No account credentials available for this service");
        }
        BigDecimal amount = service.getPrice() == null
                ? BigDecimal.ZERO
                : service.getPrice().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service price must be greater than zero");
        }
    }

    private String nextCheckoutCode() {
        String code;
        do {
            code = codeGenerator.generate("CHK", 10);
        } while (checkoutSessionRepository.existsByCheckoutCode(code));
        return code;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
