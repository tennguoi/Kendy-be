package com.example.KendyDigital.service.order.impl;

import com.example.KendyDigital.service.order.AdminOrderManagerService;
import com.example.KendyDigital.service.order.ManualOrderTaskService;
import com.example.KendyDigital.dto.order.request.AdminOrderUpdateRequest;
import com.example.KendyDigital.dto.order.request.BulkRefundOrdersRequest;
import com.example.KendyDigital.dto.order.request.CancelOrderRequest;
import com.example.KendyDigital.dto.order.request.ExtendOrderRequest;
import com.example.KendyDigital.dto.order.request.ManualOrderTaskStatusRequest;
import com.example.KendyDigital.dto.order.request.ManualOrderWorkflowRequest;
import com.example.KendyDigital.dto.order.request.OrderNoteRequest;
import com.example.KendyDigital.dto.order.request.RefundOrderRequest;
import com.example.KendyDigital.dto.order.request.ReprocessOrderRequest;
import com.example.KendyDigital.dto.order.response.OrderResponse;
import com.example.KendyDigital.model.catalog.ServiceType;
import com.example.KendyDigital.model.inventory.AccountCredential;
import com.example.KendyDigital.model.order.OrderEvent;
import com.example.KendyDigital.model.order.OrderRecord;
import com.example.KendyDigital.model.order.OrderStatus;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserRole;
import com.example.KendyDigital.model.wallet.WalletTransaction;
import com.example.KendyDigital.model.wallet.WalletTransactionType;
import com.example.KendyDigital.repository.OrderEventRepository;
import com.example.KendyDigital.repository.OrderRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.service.audit.AuditService;
import com.example.KendyDigital.service.notification.UserNotificationService;
import com.example.KendyDigital.service.product_inventory.EntitlementService;
import com.example.KendyDigital.service.wallet.WalletLedgerService;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminOrderManagerServiceImpl implements AdminOrderManagerService {
    private final OrderRepository orderRepository;
    private final UserAccountRepository userAccountRepository;
    private final OrderEventRepository orderEventRepository;
    private final WalletLedgerService walletLedgerService;
    private final AuditService auditService;
    private final EntitlementService entitlementService;
    private final UserNotificationService userNotificationService;
    private final ManualOrderTaskService manualOrderTaskService;

    public AdminOrderManagerServiceImpl(
            OrderRepository orderRepository,
            UserAccountRepository userAccountRepository,
            OrderEventRepository orderEventRepository,
            WalletLedgerService walletLedgerService,
            AuditService auditService,
            EntitlementService entitlementService,
            UserNotificationService userNotificationService,
            ManualOrderTaskService manualOrderTaskService) {
        this.orderRepository = orderRepository;
        this.userAccountRepository = userAccountRepository;
        this.orderEventRepository = orderEventRepository;
        this.walletLedgerService = walletLedgerService;
        this.auditService = auditService;
        this.entitlementService = entitlementService;
        this.userNotificationService = userNotificationService;
        this.manualOrderTaskService = manualOrderTaskService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders(OrderStatus status, Long userId, Instant fromDate, Instant toDate, int page, int size) {
        if (fromDate == null && toDate == null) {
            if (userId != null && status != null) {
                return orderRepository.findAllByUser_IdAndStatusOrderByCreatedAtDesc(userId, status, paged(page, size))
                        .stream().map(this::toResponse).toList();
            }
            if (userId != null) {
                return orderRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, paged(page, size))
                        .stream().map(this::toResponse).toList();
            }
            if (status != null) {
                return orderRepository.findAllByStatusOrderByCreatedAtDesc(status, paged(page, size))
                        .stream().map(this::toResponse).toList();
            }
            return orderRepository.findAllByOrderByCreatedAtDesc(paged(page, size))
                    .stream().map(this::toResponse).toList();
        }
        List<OrderRecord> orders = orderRepository.searchAdmin(
                null, null, status, userId, fromDate, toDate, paged(page, size));
        return orders.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> searchOrders(String query, OrderStatus status, Long userId,
            Instant fromDate, Instant toDate, int page, int size) {
        String normalizedQuery = normalizeQuery(query);
        List<OrderRecord> orders = orderRepository.searchAdmin(
                likePattern(normalizedQuery),
                parseLongOrNull(normalizedQuery),
                status,
                userId,
                fromDate,
                toDate,
                paged(page, size));
        return orders.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> listForAdmin(OrderStatus status, Long userId) {
        return listOrders(status, userId, null, null, 0, 50);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> listForAdmin(OrderStatus status, Long userId, Integer limit) {
        return listOrders(status, userId, null, null, 0, limit == null ? 50 : limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> searchForAdmin(String query, OrderStatus status, Long userId, Integer limit) {
        return searchOrders(query, status, userId, null, null, 0, limit == null ? 50 : limit);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getByCodeForAdmin(String orderCode) {
        OrderRecord order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse complete(String orderCode, Long adminUserId, AdminOrderUpdateRequest request) {
        OrderRecord order = orderRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        requireActiveOrder(order, "Order cannot be completed");

        OrderStatus oldStatus = order.getStatus();
        order.complete(blankToNull(request.resultData()), blankToNull(request.adminNote()));
        entitlementService.activateForOrder(order);
        orderEventRepository.save(new OrderEvent(order, oldStatus, order.getStatus(), adminUserId, "ADMIN", request.adminNote()));
        auditService.recordAdmin(
                adminUserId,
                "ORDER_COMPLETED",
                "ORDER",
                order.getId(),
                "adminNote=" + blankToNull(request.adminNote()));
        notifyOrderUser(order,
                "notification.order.completed.title",
                "notification.order.completed.body");
        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse fail(String orderCode, Long adminUserId, AdminOrderUpdateRequest request) {
        OrderRecord order = orderRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        requireActiveOrder(order, "Order cannot be failed");

        OrderStatus oldStatus = order.getStatus();
        order.fail(blankToNull(request.resultData()), blankToNull(request.adminNote()));
        entitlementService.revokeForOrder(order, request.adminNote());
        orderEventRepository.save(new OrderEvent(order, oldStatus, order.getStatus(), adminUserId, "ADMIN", request.adminNote()));
        auditService.recordAdmin(
                adminUserId,
                "ORDER_FAILED",
                "ORDER",
                order.getId(),
                "adminNote=" + blankToNull(request.adminNote()));
        notifyOrderUser(order,
                "notification.order.failed.title",
                "notification.order.failed.body");
        return toResponse(order);
    }

    @Override
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
        OrderStatus oldStatus = order.getStatus();
        order.cancelByAdmin(reason, refundTransaction);
        entitlementService.revokeForOrder(order, reason);
        orderEventRepository.save(new OrderEvent(order, oldStatus, order.getStatus(), adminUserId, "ADMIN", reason));

        auditService.recordAdmin(
                adminUserId,
                "ORDER_CANCELLED_BY_ADMIN",
                "ORDER",
                order.getId(),
                "refundTransactionId=" + refundTransaction.getId() + ",reason=" + reason);
        notifyOrderUser(order,
                "notification.order.cancelled.title",
                "notification.order.cancelled.body");
        return toResponse(order);
    }

    @Override
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
        AccountCredential deliveredCredential = order.getDeliveredCredential();
        if (deliveredCredential != null) {
            deliveredCredential.markRefunded();
            auditService.recordAdmin(
                    adminUserId,
                    "ACCOUNT_CREDENTIAL_REFUNDED",
                    "ACCOUNT_CREDENTIAL",
                    deliveredCredential.getId(),
                    "orderId=" + order.getId() + ",orderCode=" + order.getOrderCode());
        }
        OrderStatus oldStatus = order.getStatus();
        order.refund(refundTransaction, request.reason());
        entitlementService.revokeForOrder(order, request.reason());
        orderEventRepository.save(new OrderEvent(order, oldStatus, order.getStatus(), adminUserId, "ADMIN", request.reason()));

        auditService.recordAdmin(
                adminUserId,
                "ORDER_REFUNDED",
                "ORDER",
                order.getId(),
                "refundTransactionId=" + refundTransaction.getId() + ",reason=" + request.reason());
        notifyOrderUser(order,
                "notification.order.refunded.title",
                "notification.order.refunded.body");
        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateAdminNote(String orderCode, Long adminUserId, OrderNoteRequest request) {
        OrderRecord order = orderRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        order.updateAdminNote(request.note().trim());
        auditService.recordAdmin(adminUserId, "ORDER_ADMIN_NOTE_UPDATED", "ORDER", order.getId(), request.note());
        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateUserNote(String orderCode, Long adminUserId, OrderNoteRequest request) {
        OrderRecord order = orderRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        order.updateUserNote(request.note().trim());
        auditService.recordAdmin(adminUserId, "ORDER_USER_NOTE_UPDATED", "ORDER", order.getId(), request.note());
        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse extend(String orderCode, Long adminUserId, ExtendOrderRequest request) {
        OrderRecord order = orderRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT && order.getStatus() != OrderStatus.PROCESSING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending/processing orders can be extended");
        }
        Instant baseTime = order.getProcessingDeadlineAt() != null && order.getProcessingDeadlineAt().isAfter(Instant.now())
                ? order.getProcessingDeadlineAt()
                : Instant.now();
        order.extendProcessing(baseTime.plusSeconds(request.minutes() * 60L), request.reason().trim());
        auditService.recordAdmin(adminUserId, "ORDER_EXTENDED", "ORDER", order.getId(),
                "minutes=" + request.minutes() + ",reason=" + request.reason());
        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse reprocess(String orderCode, Long adminUserId, ReprocessOrderRequest request) {
        OrderRecord order = orderRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (order.getStatus() == OrderStatus.REFUNDED || order.getRefundTransaction() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Refunded order cannot be reprocessed");
        }
        OrderStatus oldStatus = order.getStatus();
        order.reprocess(request.reason().trim());
        orderEventRepository.save(new OrderEvent(order, oldStatus, order.getStatus(), adminUserId, "ADMIN", request.reason()));
        auditService.recordAdmin(adminUserId, "ORDER_REPROCESSED", "ORDER", order.getId(),
                "reason=" + request.reason());
        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateManualWorkflow(String orderCode, Long adminUserId, ManualOrderWorkflowRequest request) {
        OrderRecord order = orderRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (order.getService().getType() != ServiceType.MANUAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only manual service orders have workflow fields");
        }
        UserAccount assignedAdmin = null;
        if (request.assignedAdminId() != null) {
            assignedAdmin = userAccountRepository.findById(request.assignedAdminId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assigned admin not found"));
            if (assignedAdmin.getRole() == UserRole.USER) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assigned user is not an admin");
            }
        }
        order.updateManualWorkflow(
                assignedAdmin,
                request.processingDeadlineAt(),
                request.manualChecklist(),
                blankToNull(request.adminNote()),
                request.manualWorkflowStatus());
        manualOrderTaskService.syncTasks(order, assignedAdmin == null ? userAccountRepository.findById(adminUserId).orElse(null) : assignedAdmin,
                request.tasks());
        auditService.recordAdmin(adminUserId, "ORDER_MANUAL_WORKFLOW_UPDATED", "ORDER", order.getId(),
                "assignedAdminId=" + request.assignedAdminId());
        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateManualTask(String orderCode, Long adminUserId, Long taskId,
            ManualOrderTaskStatusRequest request) {
        OrderRecord order = orderRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        manualOrderTaskService.updateTaskStatus(order, adminUserId, taskId, request);
        return toResponse(order);
    }

    @Override
    @Transactional
    public List<OrderResponse> bulkRefund(Long adminUserId, BulkRefundOrdersRequest request) {
        return request.orderCodes().stream()
                .map(code -> refund(code, adminUserId, new RefundOrderRequest(request.reason())))
                .toList();
    }

    private OrderResponse toResponse(OrderRecord order) {
        return OrderResponse.from(order, manualOrderTaskService.findTasksForOrder(order));
    }

    private WalletTransaction createRefundTransaction(OrderRecord order, Long createdBy, String description) {
        if (order.getRefundTransaction() != null || order.getStatus() == OrderStatus.REFUNDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order has already been refunded");
        }
        return walletLedgerService.credit(
                order.getUser().getId(),
                order.getAmount(),
                WalletTransactionType.REFUND,
                "ORDER",
                order.getId(),
                description,
                createdBy);
    }

    private void notifyOrderUser(OrderRecord order, String titleKey, String bodyKey) {
        userNotificationService.createLocalized(order.getUser().getId(),
                titleKey,
                bodyKey,
                null,
                new Object[]{order.getOrderCode()},
                "ORDER",
                "/orders/" + order.getOrderCode());
    }

    private void requireActiveOrder(OrderRecord order, String message) {
        if (order.getStatus() == OrderStatus.COMPLETED
                || order.getStatus() == OrderStatus.CANCELLED
                || order.getStatus() == OrderStatus.REFUNDED
                || order.getStatus() == OrderStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }

    private PageRequest paged(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 200)));
    }

    private String normalizeQuery(String query) {
        return query == null || query.isBlank() ? null : query.trim();
    }

    private String likePattern(String query) {
        return query == null ? null : "%" + query.toLowerCase(Locale.ROOT) + "%";
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

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
