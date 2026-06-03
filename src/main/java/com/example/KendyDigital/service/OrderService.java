package com.example.KendyDigital.service;

import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.KendyDigital.common.CodeGenerator;
import com.example.KendyDigital.dto.AdminOrderUpdateRequest;
import com.example.KendyDigital.dto.BulkRefundOrdersRequest;
import com.example.KendyDigital.dto.CancelOrderRequest;
import com.example.KendyDigital.dto.CreateOrderRequest;
import com.example.KendyDigital.dto.ExtendOrderRequest;
import com.example.KendyDigital.dto.OrderNoteRequest;
import com.example.KendyDigital.dto.OrderResponse;
import com.example.KendyDigital.dto.RefundOrderRequest;
import com.example.KendyDigital.dto.ReprocessOrderRequest;
import com.example.KendyDigital.model.OrderRecord;
import com.example.KendyDigital.model.OrderStatus;
import com.example.KendyDigital.model.ServiceItem;
import com.example.KendyDigital.model.ServiceStatus;
import com.example.KendyDigital.model.UserAccount;
import com.example.KendyDigital.model.UserStatus;
import com.example.KendyDigital.model.WalletTransaction;
import com.example.KendyDigital.model.WalletTransactionType;
import com.example.KendyDigital.repository.OrderRepository;
import com.example.KendyDigital.repository.ServiceItemRepository;
import com.example.KendyDigital.repository.UserAccountRepository;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserAccountRepository userAccountRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final WalletLedgerService walletLedgerService;
    private final CodeGenerator codeGenerator;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final UserNotificationService userNotificationService;

    public OrderService(OrderRepository orderRepository,
            UserAccountRepository userAccountRepository,
            ServiceItemRepository serviceItemRepository,
            WalletLedgerService walletLedgerService,
            CodeGenerator codeGenerator,
            AuditService auditService,
            ObjectMapper objectMapper,
            UserNotificationService userNotificationService) {
        this.orderRepository = orderRepository;
        this.userAccountRepository = userAccountRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.walletLedgerService = walletLedgerService;
        this.codeGenerator = codeGenerator;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.userNotificationService = userNotificationService;
    }

    @Transactional
    public OrderResponse create(Long userId, CreateOrderRequest request) {
        String idempotencyKey = blankToNull(request.idempotencyKey());
        if (idempotencyKey != null) {
            OrderRecord existingOrder = orderRepository
                    .findByUser_IdAndIdempotencyKey(userId, idempotencyKey)
                    .orElse(null);
            if (existingOrder != null) {
                return OrderResponse.from(existingOrder);
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
        validateInputData(service, request.inputData());

        OrderRecord order = orderRepository.save(new OrderRecord(
                nextOrderCode(),
                user,
                service,
                service.getPrice().setScale(2, RoundingMode.HALF_UP),
                request.inputData(),
                idempotencyKey));

        WalletTransaction walletTransaction = walletLedgerService.debit(
                user,
                order.getAmount(),
                WalletTransactionType.PURCHASE,
                "ORDER",
                order.getId(),
                "Purchase order " + order.getOrderCode(),
                null);
        order.attachPurchaseTransaction(walletTransaction);

        auditService.recordSystem(
                "ORDER_PURCHASED",
                "ORDER",
                order.getId(),
                "walletTransactionId=" + walletTransaction.getId());
        return OrderResponse.from(order);
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
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> searchForUser(Long userId, String query, OrderStatus status, int page, int size) {
        String normalizedQuery = normalizeQuery(query);
        return orderRepository.searchUser(
                        userId,
                        normalizedQuery,
                        parseLongOrNull(normalizedQuery),
                        status,
                        paged(page, size))
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listForAdmin(OrderStatus status, Long userId) {
        return listForAdmin(status, userId, null);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listForAdmin(OrderStatus status, Long userId, Integer limit) {
        List<OrderRecord> orders = userId != null
                ? orderRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, page(limit))
                : status == null
                        ? orderRepository.findAllByOrderByCreatedAtDesc(page(limit))
                        : orderRepository.findAllByStatusOrderByCreatedAtDesc(status, page(limit));
        return orders.stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> searchForAdmin(String query, OrderStatus status, Long userId, Integer limit) {
        String normalizedQuery = normalizeQuery(query);
        return orderRepository.searchAdmin(
                        normalizedQuery,
                        parseLongOrNull(normalizedQuery),
                        status,
                        userId,
                        page(limit))
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getByCodeForUser(Long userId, String orderCode) {
        OrderRecord order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getByCodeForAdmin(String orderCode) {
        return orderRepository.findByOrderCode(orderCode)
                .map(OrderResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
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
        order.cancelByUser(reason, refundTransaction);

        auditService.recordSystem(
                "ORDER_CANCELLED_BY_USER",
                "ORDER",
                order.getId(),
                "refundTransactionId=" + refundTransaction.getId() + ",reason=" + reason);
        userNotificationService.create(userId,
                "Order cancelled",
                "Order " + order.getOrderCode() + " was cancelled and refunded.",
                "ORDER",
                "/orders/" + order.getOrderCode());
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse reorder(Long userId, String orderCode) {
        OrderRecord source = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!source.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        return create(userId, new CreateOrderRequest(source.getService().getId(), source.getInputData(), null));
    }

    @Transactional
    public OrderResponse complete(String orderCode, Long adminUserId, AdminOrderUpdateRequest request) {
        OrderRecord order = orderRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        requireActiveOrder(order, "Order cannot be completed");

        order.complete(blankToNull(request.resultData()), blankToNull(request.adminNote()));
        auditService.recordAdmin(
                adminUserId,
                "ORDER_COMPLETED",
                "ORDER",
                order.getId(),
                "adminNote=" + blankToNull(request.adminNote()));
        notifyOrderUser(order, "Order completed", "Order " + order.getOrderCode() + " has been completed.");
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse fail(String orderCode, Long adminUserId, AdminOrderUpdateRequest request) {
        OrderRecord order = orderRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        requireActiveOrder(order, "Order cannot be failed");

        order.fail(blankToNull(request.resultData()), blankToNull(request.adminNote()));
        auditService.recordAdmin(
                adminUserId,
                "ORDER_FAILED",
                "ORDER",
                order.getId(),
                "adminNote=" + blankToNull(request.adminNote()));
        notifyOrderUser(order, "Order failed", "Order " + order.getOrderCode() + " could not be completed.");
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse cancelByAdmin(String orderCode, Long adminUserId, CancelOrderRequest request) {
        OrderRecord order = orderRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        requireActiveOrder(order, "Order cannot be cancelled");

        String reason = request.reason().trim();
        WalletTransaction refundTransaction = createRefundTransaction(
                order,
                adminUserId,
                "Admin cancel order " + order.getOrderCode());
        order.cancelByAdmin(reason, refundTransaction);

        auditService.recordAdmin(
                adminUserId,
                "ORDER_CANCELLED_BY_ADMIN",
                "ORDER",
                order.getId(),
                "refundTransactionId=" + refundTransaction.getId() + ",reason=" + reason);
        notifyOrderUser(order, "Order cancelled", "Order " + order.getOrderCode() + " was cancelled and refunded.");
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse refund(String orderCode, Long adminUserId, RefundOrderRequest request) {
        OrderRecord order = orderRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (order.getStatus() == OrderStatus.REFUNDED || order.getRefundTransaction() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order has already been refunded");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cancelled order has already been handled");
        }

        WalletTransaction refundTransaction = createRefundTransaction(
                order,
                adminUserId,
                "Refund order " + order.getOrderCode());
        order.refund(refundTransaction, request.reason());

        auditService.recordAdmin(
                adminUserId,
                "ORDER_REFUNDED",
                "ORDER",
                order.getId(),
                "refundTransactionId=" + refundTransaction.getId() + ",reason=" + request.reason());
        notifyOrderUser(order, "Order refunded", "Order " + order.getOrderCode() + " has been refunded.");
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse updateAdminNote(String orderCode, Long adminUserId, OrderNoteRequest request) {
        OrderRecord order = orderRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        order.updateAdminNote(request.note().trim());
        auditService.recordAdmin(adminUserId, "ORDER_ADMIN_NOTE_UPDATED", "ORDER", order.getId(), request.note());
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse updateUserNote(String orderCode, Long adminUserId, OrderNoteRequest request) {
        OrderRecord order = orderRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        order.updateUserNote(request.note().trim());
        auditService.recordAdmin(adminUserId, "ORDER_USER_NOTE_UPDATED", "ORDER", order.getId(), request.note());
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse extend(String orderCode, Long adminUserId, ExtendOrderRequest request) {
        OrderRecord order = orderRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PROCESSING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending/processing orders can be extended");
        }
        Instant baseTime = order.getProcessingDeadlineAt() != null && order.getProcessingDeadlineAt().isAfter(Instant.now())
                ? order.getProcessingDeadlineAt()
                : Instant.now();
        order.extendProcessing(baseTime.plusSeconds(request.minutes() * 60L), request.reason().trim());
        auditService.recordAdmin(adminUserId, "ORDER_EXTENDED", "ORDER", order.getId(),
                "minutes=" + request.minutes() + ",reason=" + request.reason());
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse reprocess(String orderCode, Long adminUserId, ReprocessOrderRequest request) {
        OrderRecord order = orderRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (order.getStatus() == OrderStatus.REFUNDED || order.getRefundTransaction() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Refunded order cannot be reprocessed");
        }
        order.reprocess(request.reason().trim());
        auditService.recordAdmin(adminUserId, "ORDER_REPROCESSED", "ORDER", order.getId(),
                "reason=" + request.reason());
        return OrderResponse.from(order);
    }

    @Transactional
    public List<OrderResponse> bulkRefund(Long adminUserId, BulkRefundOrdersRequest request) {
        return request.orderCodes().stream()
                .map(code -> refund(code, adminUserId, new RefundOrderRequest(request.reason())))
                .toList();
    }

    private WalletTransaction createRefundTransaction(OrderRecord order, Long createdBy, String description) {
        if (order.getRefundTransaction() != null || order.getStatus() == OrderStatus.REFUNDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order has already been refunded");
        }
        UserAccount user = userAccountRepository.findByIdForUpdate(order.getUser().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return walletLedgerService.credit(
                user,
                order.getAmount(),
                WalletTransactionType.REFUND,
                "ORDER",
                order.getId(),
                description,
                createdBy);
    }

    private void requireActiveOrder(OrderRecord order, String message) {
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PROCESSING) {
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

    private void notifyOrderUser(OrderRecord order, String title, String message) {
        userNotificationService.create(order.getUser().getId(), title, message, "ORDER",
                "/orders/" + order.getOrderCode());
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
