package com.example.KendyDigital.service;

import java.math.RoundingMode;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.KendyDigital.common.CodeGenerator;
import com.example.KendyDigital.dto.AdminOrderUpdateRequest;
import com.example.KendyDigital.dto.CancelOrderRequest;
import com.example.KendyDigital.dto.CreateOrderRequest;
import com.example.KendyDigital.dto.OrderResponse;
import com.example.KendyDigital.dto.RefundOrderRequest;
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

    public OrderService(OrderRepository orderRepository,
            UserAccountRepository userAccountRepository,
            ServiceItemRepository serviceItemRepository,
            WalletLedgerService walletLedgerService,
            CodeGenerator codeGenerator,
            AuditService auditService) {
        this.orderRepository = orderRepository;
        this.userAccountRepository = userAccountRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.walletLedgerService = walletLedgerService;
        this.codeGenerator = codeGenerator;
        this.auditService = auditService;
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
        List<OrderRecord> orders = status == null
                ? orderRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, PageRequest.of(0, 50))
                : orderRepository.findAllByUser_IdAndStatusOrderByCreatedAtDesc(userId, status, PageRequest.of(0, 50));
        return orders
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listForAdmin(OrderStatus status, Long userId) {
        List<OrderRecord> orders = userId != null
                ? orderRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, PageRequest.of(0, 100))
                : status == null
                        ? orderRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 100))
                        : orderRepository.findAllByStatusOrderByCreatedAtDesc(status, PageRequest.of(0, 100));
        return orders.stream()
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
        return OrderResponse.from(order);
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
        return OrderResponse.from(order);
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
}
