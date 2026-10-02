package com.example.KendyDigital.service.order.impl;

import com.example.KendyDigital.service.order.*;

import com.example.KendyDigital.common.CodeGenerator;
import com.example.KendyDigital.dto.order.request.AdminOrderUpdateRequest;
import com.example.KendyDigital.dto.order.request.BulkRefundOrdersRequest;
import com.example.KendyDigital.dto.order.request.CancelOrderRequest;
import com.example.KendyDigital.dto.order.request.CreateOrderRequest;
import com.example.KendyDigital.dto.order.request.ExtendOrderRequest;
import com.example.KendyDigital.dto.order.request.ManualOrderWorkflowRequest;
import com.example.KendyDigital.dto.order.request.ManualOrderTaskRequest;
import com.example.KendyDigital.dto.order.request.ManualOrderTaskStatusRequest;
import com.example.KendyDigital.dto.order.request.OrderNoteRequest;
import com.example.KendyDigital.dto.order.request.RefundOrderRequest;
import com.example.KendyDigital.dto.order.request.ReprocessOrderRequest;
import com.example.KendyDigital.dto.order.response.ManualOrderTaskResponse;
import com.example.KendyDigital.dto.order.response.OrderResponse;
import com.example.KendyDigital.dto.notification.response.AdminNotificationResponse;
import com.example.KendyDigital.model.catalog.ServiceCtaType;
import com.example.KendyDigital.model.catalog.ServiceItem;
import com.example.KendyDigital.model.catalog.ServiceStatus;
import com.example.KendyDigital.model.catalog.ServiceStockStatus;
import com.example.KendyDigital.model.catalog.ServiceType;
import com.example.KendyDigital.model.admin.AdminNotification;
import com.example.KendyDigital.model.inventory.AccountCredential;
import com.example.KendyDigital.model.order.ManualOrderTask;
import com.example.KendyDigital.model.order.OrderEvent;
import com.example.KendyDigital.model.order.OrderRecord;
import com.example.KendyDigital.model.order.OrderStatus;
import com.example.KendyDigital.model.ticket.Ticket;
import com.example.KendyDigital.model.ticket.TicketCategory;
import com.example.KendyDigital.model.ticket.TicketMessage;
import com.example.KendyDigital.model.ticket.TicketPriority;
import com.example.KendyDigital.model.ticket.TicketSenderRole;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserRole;
import com.example.KendyDigital.model.user.UserStatus;
import com.example.KendyDigital.model.wallet.WalletTransaction;
import com.example.KendyDigital.model.wallet.WalletTransactionType;
import com.example.KendyDigital.repository.OrderEventRepository;
import com.example.KendyDigital.repository.OrderRepository;
import com.example.KendyDigital.repository.ServiceItemRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.service.audit.AuditService;
import com.example.KendyDigital.dto.coupon.AppliedCoupon;
import com.example.KendyDigital.service.product_inventory.CouponService;
import com.example.KendyDigital.service.product_inventory.EntitlementService;
import com.example.KendyDigital.service.product_inventory.AccountInventoryService;
import com.example.KendyDigital.service.notification.UserNotificationService;
import com.example.KendyDigital.service.order.helper.ManualOrderSupportService;
import com.example.KendyDigital.service.wallet.WalletLedgerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final UserAccountRepository userAccountRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final WalletLedgerService walletLedgerService;
    private final CodeGenerator codeGenerator;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final UserNotificationService userNotificationService;
    private final AccountInventoryService accountInventoryService;
    private final CouponService couponService;
    private final OrderEventRepository orderEventRepository;
    private final EntitlementService entitlementService;
    private final ManualOrderTaskService manualOrderTaskService;
    private final ManualOrderSupportService manualOrderSupportService;
    private final AdminOrderManagerService adminOrderManagerService;

    public OrderServiceImpl(OrderRepository orderRepository,
            UserAccountRepository userAccountRepository,
            ServiceItemRepository serviceItemRepository,
            WalletLedgerService walletLedgerService,
            CodeGenerator codeGenerator,
            AuditService auditService,
            ObjectMapper objectMapper,
            UserNotificationService userNotificationService,
            AccountInventoryService accountInventoryService,
            CouponService couponService,
            OrderEventRepository orderEventRepository,
            EntitlementService entitlementService,
            ManualOrderTaskService manualOrderTaskService,
            ManualOrderSupportService manualOrderSupportService,
            AdminOrderManagerService adminOrderManagerService) {
        this.orderRepository = orderRepository;
        this.userAccountRepository = userAccountRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.walletLedgerService = walletLedgerService;
        this.codeGenerator = codeGenerator;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.userNotificationService = userNotificationService;
        this.accountInventoryService = accountInventoryService;
        this.couponService = couponService;
        this.orderEventRepository = orderEventRepository;
        this.entitlementService = entitlementService;
        this.manualOrderTaskService = manualOrderTaskService;
        this.manualOrderSupportService = manualOrderSupportService;
        this.adminOrderManagerService = adminOrderManagerService;
    }

    @Transactional
    public OrderResponse create(Long userId, CreateOrderRequest request) {
        return createInternal(userId, request, null);
    }

    @Transactional
    public OrderResponse createForCheckout(Long userId, CreateOrderRequest request, Long checkoutId) {
        return createInternal(userId, request, checkoutId);
    }

    private OrderResponse createInternal(Long userId, CreateOrderRequest request, Long checkoutId) {
        String idempotencyKey = blankToNull(request.idempotencyKey());
        if (idempotencyKey != null) {
            OrderRecord existingOrder = orderRepository
                    .findByUser_IdAndIdempotencyKey(userId, idempotencyKey)
                    .orElse(null);
            if (existingOrder != null) {
                return toResponse(existingOrder);
            }
        }

        UserAccount user = userAccountRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not active");
        }

        ServiceItem service = serviceItemRepository.findById(request.serviceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
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
        BigDecimal originalAmount = service.getPrice().setScale(2, RoundingMode.HALF_UP);
        if (originalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service price must be greater than zero");
        }
        AppliedCoupon appliedCoupon = couponService.applyForPurchase(user, service, originalAmount,
                request.couponCode());
        BigDecimal orderAmount;
        AccountCredential credential = null;
        OrderRecord order;
        WalletTransaction walletTransaction;
        try {
            orderAmount = appliedCoupon.payableAmount();
            BigDecimal currentBalance = user.getBalance() == null
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : user.getBalance().setScale(2, RoundingMode.HALF_UP);
            if (currentBalance.compareTo(orderAmount) < 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient wallet balance");
            }
            validateInputData(service, request.inputData());

            if (service.getType() == ServiceType.ACCOUNT_STOCK) {
                if (checkoutId != null) {
                    credential = accountInventoryService.takeReservedForOrder(service.getId(), checkoutId, userId);
                } else {
                    if (accountInventoryService.availableCount(service.getId()) <= 0) {
                        service.updatePricingMetadata(ServiceStockStatus.OUT_OF_STOCK, service.getCtaType(),
                                service.getPricingBadge(), service.isFeatured(), service.isPublicVisible());
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "No account credentials available for this service");
                    }
                    credential = accountInventoryService.takeAvailableForOrder(service.getId());
                }
            }

            order = orderRepository.save(new OrderRecord(
                    nextOrderCode(),
                    user,
                    service,
                    orderAmount,
                    request.inputData(),
                    idempotencyKey));
            order.applyPricing(appliedCoupon.originalAmount(), appliedCoupon.discountAmount(), appliedCoupon.code());
            orderEventRepository.save(new OrderEvent(order, null, OrderStatus.PROCESSING, userId, "USER", "Order created"));

            walletTransaction = walletLedgerService.debit(
                    userId,
                    order.getAmount(),
                    WalletTransactionType.PURCHASE,
                    "ORDER",
                    order.getId(),
                    "Purchase order " + order.getOrderCode(),
                    null);
            order.attachPurchaseTransaction(walletTransaction);
            couponService.redeemForOrder(user, order, appliedCoupon);

            if (credential != null) {
                credential.deliver(order, user);
                order.attachDeliveredCredential(credential);
                OrderStatus oldStatus = order.getStatus();
                order.complete(deliveryResultData(order, credential, service), "Auto-delivered account credential");
                orderEventRepository.save(new OrderEvent(order, oldStatus, order.getStatus(), null, "SYSTEM", "Auto-delivered account credential"));
                if (accountInventoryService.availableCount(service.getId()) <= 0) {
                    service.updatePricingMetadata(ServiceStockStatus.OUT_OF_STOCK, service.getCtaType(),
                            service.getPricingBadge(), service.isFeatured(), service.isPublicVisible());
                }
                userNotificationService.createLocalized(userId,
                        "notification.order.credential_delivered.title",
                        "notification.order.credential_delivered.body",
                        null,
                        new Object[]{order.getOrderCode()},
                        "ORDER",
                        "/locker");
            } else {
                order.initializeManualWorkflow(Instant.now().plusSeconds(24 * 60 * 60));
                manualOrderTaskService.createDefaultTasks(order);
                Ticket supportTicket = manualOrderSupportService.createManualOrderTicket(order, user);
                order.attachSupportTicket(supportTicket);
                userNotificationService.createLocalized(userId,
                        "notification.order.manual_created.title",
                        "notification.order.manual_created.body",
                        null,
                        new Object[]{order.getOrderCode(), supportTicket.getTicketCode()},
                        "ORDER",
                        "/orders/" + order.getOrderCode());
                manualOrderSupportService.notifyAdminsManualOrder(order);
            }

            entitlementService.createForOrder(order, credential);
            auditService.recordSystem(
                    "ORDER_PURCHASED",
                    "ORDER",
                    order.getId(),
                    "walletTransactionId=" + walletTransaction.getId());
            return toResponse(order);
        } catch (Exception exception) {
            couponService.rollbackPreClaim(userId, appliedCoupon);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listByUser(Long userId, OrderStatus status) {
        return listByUser(userId, status, 0, 50);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listByUser(Long userId, OrderStatus status, int page, int size) {
        List<OrderRecord> orders = status == null
                ? orderRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, paged(page, size))
                : orderRepository.findAllByUser_IdAndStatusOrderByCreatedAtDesc(userId, status, paged(page, size));
        return orders
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> searchForUser(Long userId, String query, OrderStatus status, int page, int size) {
        String normalizedQuery = normalizeQuery(query);
        return orderRepository.searchUser(
                        userId,
                        likePattern(normalizedQuery),
                        parseLongOrNull(normalizedQuery),
                        status,
                        paged(page, size))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listForAdmin(OrderStatus status, Long userId) {
        return adminOrderManagerService.listForAdmin(status, userId);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listForAdmin(OrderStatus status, Long userId, Integer limit) {
        return adminOrderManagerService.listForAdmin(status, userId, limit);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> searchForAdmin(String query, OrderStatus status, Long userId, Integer limit) {
        return adminOrderManagerService.searchForAdmin(query, status, userId, limit);
    }

    @Transactional(readOnly = true)
    public OrderResponse getByCodeForUser(Long userId, String orderCode) {
        OrderRecord order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getByCodeForAdmin(String orderCode) {
        return adminOrderManagerService.getByCodeForAdmin(orderCode);
    }

    @Transactional
    public OrderResponse cancelForUser(Long userId, String orderCode, CancelOrderRequest request) {
        OrderRecord order = orderRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        requireActiveOrder(order, "Order cannot be cancelled");

        String reason = request.reason().trim();
        WalletTransaction refundTransaction = createRefundTransaction(
                order,
                null,
                "Cancel order " + order.getOrderCode());
        OrderStatus oldStatus = order.getStatus();
        order.cancelByUser(reason, refundTransaction);
        entitlementService.revokeForOrder(order, reason);
        orderEventRepository.save(new OrderEvent(order, oldStatus, order.getStatus(), userId, "USER", reason));

        auditService.recordSystem(
                "ORDER_CANCELLED_BY_USER",
                "ORDER",
                order.getId(),
                "refundTransactionId=" + refundTransaction.getId() + ",reason=" + reason);
        userNotificationService.createLocalized(userId,
                "notification.order.cancelled.title",
                "notification.order.cancelled.body",
                null,
                new Object[]{order.getOrderCode()},
                "ORDER",
                "/orders/" + order.getOrderCode());
        return toResponse(order);
    }

    @Transactional
    public OrderResponse complete(String orderCode, Long adminUserId, AdminOrderUpdateRequest request) {
        return adminOrderManagerService.complete(orderCode, adminUserId, request);
    }

    @Transactional
    public OrderResponse fail(String orderCode, Long adminUserId, AdminOrderUpdateRequest request) {
        return adminOrderManagerService.fail(orderCode, adminUserId, request);
    }

    @Transactional
    public OrderResponse cancelByAdmin(String orderCode, Long adminUserId, CancelOrderRequest request) {
        return adminOrderManagerService.cancelByAdmin(orderCode, adminUserId, request);
    }

    @Transactional
    public OrderResponse refund(String orderCode, Long adminUserId, RefundOrderRequest request) {
        return adminOrderManagerService.refund(orderCode, adminUserId, request);
    }

    @Transactional
    public OrderResponse updateAdminNote(String orderCode, Long adminUserId, OrderNoteRequest request) {
        return adminOrderManagerService.updateAdminNote(orderCode, adminUserId, request);
    }

    @Transactional
    public OrderResponse updateUserNote(String orderCode, Long adminUserId, OrderNoteRequest request) {
        return adminOrderManagerService.updateUserNote(orderCode, adminUserId, request);
    }

    @Transactional
    public OrderResponse extend(String orderCode, Long adminUserId, ExtendOrderRequest request) {
        return adminOrderManagerService.extend(orderCode, adminUserId, request);
    }

    @Transactional
    public OrderResponse reprocess(String orderCode, Long adminUserId, ReprocessOrderRequest request) {
        return adminOrderManagerService.reprocess(orderCode, adminUserId, request);
    }

    @Transactional
    public OrderResponse updateManualWorkflow(String orderCode, Long adminUserId, ManualOrderWorkflowRequest request) {
        return adminOrderManagerService.updateManualWorkflow(orderCode, adminUserId, request);
    }

    @Transactional
    public OrderResponse updateManualTask(String orderCode, Long adminUserId, Long taskId,
            ManualOrderTaskStatusRequest request) {
        return adminOrderManagerService.updateManualTask(orderCode, adminUserId, taskId, request);
    }

    @Transactional
    public List<OrderResponse> bulkRefund(Long adminUserId, BulkRefundOrdersRequest request) {
        return adminOrderManagerService.bulkRefund(adminUserId, request);
    }

    private OrderResponse toResponse(OrderRecord order) {
        return OrderResponse.from(order, manualOrderTaskService.findTasksForOrder(order));
    }

    private WalletTransaction createRefundTransaction(OrderRecord order, Long createdBy, String description) {
        if (order.getRefundTransaction() != null || order.getStatus() == OrderStatus.REFUNDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order has already been refunded");
        }
        Long userId = order.getUser().getId();
        return walletLedgerService.credit(
                userId,
                order.getAmount(),
                WalletTransactionType.REFUND,
                "ORDER",
                order.getId(),
                description,
                createdBy);
    }

    private void requireActiveOrder(OrderRecord order, String message) {
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT && order.getStatus() != OrderStatus.PROCESSING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message + " from status " + order.getStatus());
        }
    }

    private String nextOrderCode() {
        String code;
        do {
            code = codeGenerator.generate("OD", 10);
        } while (orderRepository.existsByOrderCode(code));
        return code;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeQuery(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String likePattern(String value) {
        return value == null ? null : "%" + value.toLowerCase(Locale.ROOT) + "%";
    }

    private Long parseLongOrNull(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private PageRequest page(Integer limit) {
        int normalizedLimit = limit == null ? 100 : Math.max(1, Math.min(limit, 200));
        return PageRequest.of(0, normalizedLimit);
    }

    private PageRequest paged(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 200)));
    }

    private String deliveryResultData(OrderRecord order, AccountCredential credential, ServiceItem service) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("deliveryType", "ACCOUNT_CREDENTIAL");
        payload.put("orderCode", order.getOrderCode());
        payload.put("serviceName", service.getName());
        payload.put("credentialId", credential.getId());
        payload.put("loginIdentifier", credential.getLoginIdentifier());
        payload.put("serviceUsageNotes", service.getUsageNotes());
        payload.put("warrantyPolicy", service.getWarrantyPolicy());
        payload.put("deliveredAt", Instant.now().toString());
        payload.put("expiresAt", credential.getExpiresAt() == null ? null : credential.getExpiresAt().toString());
        payload.put("warrantyUntil", credential.getWarrantyUntil() == null ? null : credential.getWarrantyUntil().toString());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            return "Login: " + credential.getLoginIdentifier()
                    + "\nCredential ID: " + credential.getId()
                    + "\nOpen order detail to reveal delivered credential.";
        }
    }

    private void validateInputData(ServiceItem service, String inputData) {
        String schemaText = blankToNull(service.getInputSchema());
        if (schemaText == null) {
            return;
        }
        try {
            JsonNode schema = objectMapper.readTree(schemaText);
            if (!schema.isObject()) {
                return;
            }
            JsonNode input = blankToNull(inputData) == null ? objectMapper.createObjectNode()
                    : objectMapper.readTree(inputData);
            if (schema.has("type") && "object".equals(schema.get("type").asText()) && !input.isObject()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Input data must be a JSON object");
            }
            JsonNode required = schema.get("required");
            if (required != null && required.isArray()) {
                for (JsonNode field : required) {
                    String name = field.asText();
                    if (!input.hasNonNull(name)) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Missing required input field: " + name);
                    }
                }
            }
            JsonNode properties = schema.get("properties");
            if (properties != null && properties.isObject() && input.isObject()) {
                properties.fields().forEachRemaining(entry -> validateInputType(entry.getKey(), input.get(entry.getKey()),
                        entry.getValue()));
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Input data must be valid JSON");
        }
    }

    private void validateInputType(String field, JsonNode value, JsonNode propertySchema) {
        if (value == null || value.isNull() || propertySchema == null || !propertySchema.has("type")) {
            return;
        }
        String type = propertySchema.get("type").asText();
        boolean valid = switch (type) {
            case "string" -> value.isTextual();
            case "number" -> value.isNumber();
            case "integer" -> value.isIntegralNumber();
            case "boolean" -> value.isBoolean();
            case "array" -> value.isArray();
            case "object" -> value.isObject();
            default -> true;
        };
        if (!valid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid type for input field: " + field);
        }
    }
}

