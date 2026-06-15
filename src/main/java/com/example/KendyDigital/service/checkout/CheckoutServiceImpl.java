package com.example.KendyDigital.service.checkout;

import com.example.KendyDigital.common.CodeGenerator;
import com.example.KendyDigital.dto.checkout.request.CreateServiceCheckoutRequest;
import com.example.KendyDigital.dto.checkout.response.CheckoutResponse;
import com.example.KendyDigital.dto.order.request.CreateOrderRequest;
import com.example.KendyDigital.model.catalog.ServiceCtaType;
import com.example.KendyDigital.model.catalog.ServiceItem;
import com.example.KendyDigital.model.catalog.ServiceStatus;
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
import com.example.KendyDigital.repository.ServiceItemRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.service.audit.AuditService;
import com.example.KendyDigital.service.deposit.DepositService;
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

    public CheckoutServiceImpl(CheckoutSessionRepository checkoutSessionRepository,
            DepositRequestRepository depositRequestRepository,
            OrderRepository orderRepository,
            ServiceItemRepository serviceItemRepository,
            UserAccountRepository userAccountRepository,
            DepositService depositService,
            OrderService orderService,
            CodeGenerator codeGenerator,
            AuditService auditService) {
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.depositRequestRepository = depositRequestRepository;
        this.orderRepository = orderRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.userAccountRepository = userAccountRepository;
        this.depositService = depositService;
        this.orderService = orderService;
        this.codeGenerator = codeGenerator;
        this.auditService = auditService;
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

        BigDecimal amount = service.getPrice().setScale(2, RoundingMode.HALF_UP);
        DepositRequest deposit = depositService.createDepositForAmount(userId, amount);
        CheckoutSession checkout = checkoutSessionRepository.save(new CheckoutSession(
                nextCheckoutCode(),
                user,
                service,
                deposit,
                request.inputData(),
                idempotencyKey));

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

    private void refreshCheckoutStatus(CheckoutSession checkout) {
        if (checkout.getOrder() != null) {
            checkout.setStatus(CheckoutStatus.ORDER_CREATED);
            return;
        }

        DepositRequest deposit = depositRequestRepository.findByDepositCodeForUpdate(checkout.getDepositRequest().getDepositCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deposit not found"));
        checkout.setDepositRequest(deposit);

        if (deposit.getStatus() == DepositStatus.CANCELLED) {
            checkout.markCancelled();
            return;
        }

        if (deposit.getStatus() == DepositStatus.EXPIRED || deposit.getExpiredAt().isBefore(Instant.now())) {
            checkout.markExpired();
            return;
        }

        if (deposit.getStatus() != DepositStatus.COMPLETED) {
            checkout.setStatus(CheckoutStatus.PENDING_PAYMENT);
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

    private OrderRecord createOrderForPaidCheckout(CheckoutSession checkout) {
        String idempotencyKey = checkout.getIdempotencyKey() == null
                ? "checkout:" + checkout.getCheckoutCode()
                : checkout.getIdempotencyKey();
        orderService.create(checkout.getUser().getId(), new CreateOrderRequest(
                checkout.getService().getId(),
                checkout.getInputData(),
                idempotencyKey));
        return orderRepository.findByUser_IdAndIdempotencyKey(checkout.getUser().getId(), idempotencyKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Checkout order not found"));
    }

    private void validateServiceForCheckout(ServiceItem service) {
        if (service.getStatus() != ServiceStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Service is not active");
        }
        if (service.getCtaType() != null && service.getCtaType() != ServiceCtaType.BUY_NOW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Service requires consultation before purchase");
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
