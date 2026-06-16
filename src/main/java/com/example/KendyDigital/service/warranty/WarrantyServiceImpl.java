package com.example.KendyDigital.service.warranty;

import com.example.KendyDigital.dto.order.request.RefundOrderRequest;
import com.example.KendyDigital.dto.warranty.request.AdminWarrantyReviewRequest;
import com.example.KendyDigital.dto.warranty.request.CreateWarrantyRequest;
import com.example.KendyDigital.dto.warranty.response.WarrantyRequestResponse;
import com.example.KendyDigital.model.admin.AdminNotification;
import com.example.KendyDigital.model.inventory.AccountCredential;
import com.example.KendyDigital.model.order.OrderRecord;
import com.example.KendyDigital.model.order.OrderStatus;
import com.example.KendyDigital.model.warranty.WarrantyRequest;
import com.example.KendyDigital.model.warranty.WarrantyRequestStatus;
import com.example.KendyDigital.repository.AdminNotificationRepository;
import com.example.KendyDigital.repository.OrderRepository;
import com.example.KendyDigital.repository.WarrantyRequestRepository;
import com.example.KendyDigital.service.audit.AuditService;
import com.example.KendyDigital.service.inventory.AccountInventoryService;
import com.example.KendyDigital.service.notification.UserNotificationService;
import com.example.KendyDigital.service.order.OrderService;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WarrantyServiceImpl implements WarrantyService {
    private static final List<WarrantyRequestStatus> OPEN_STATUSES =
            List.of(WarrantyRequestStatus.OPEN, WarrantyRequestStatus.REVIEWING);

    private final WarrantyRequestRepository warrantyRequestRepository;
    private final OrderRepository orderRepository;
    private final AccountInventoryService accountInventoryService;
    private final OrderService orderService;
    private final AuditService auditService;
    private final AdminNotificationRepository adminNotificationRepository;
    private final UserNotificationService userNotificationService;

    public WarrantyServiceImpl(WarrantyRequestRepository warrantyRequestRepository,
            OrderRepository orderRepository,
            AccountInventoryService accountInventoryService,
            OrderService orderService,
            AuditService auditService,
            AdminNotificationRepository adminNotificationRepository,
            UserNotificationService userNotificationService) {
        this.warrantyRequestRepository = warrantyRequestRepository;
        this.orderRepository = orderRepository;
        this.accountInventoryService = accountInventoryService;
        this.orderService = orderService;
        this.auditService = auditService;
        this.adminNotificationRepository = adminNotificationRepository;
        this.userNotificationService = userNotificationService;
    }

    @Transactional
    public WarrantyRequestResponse create(Long userId, String orderCode, CreateWarrantyRequest request) {
        OrderRecord order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only completed orders can request warranty");
        }
        AccountCredential deliveredCredential = order.getDeliveredCredential();
        if (deliveredCredential != null
                && deliveredCredential.getWarrantyUntil() != null
                && deliveredCredential.getWarrantyUntil().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Warranty period has expired");
        }
        if (warrantyRequestRepository.existsByOrder_IdAndStatusIn(order.getId(), OPEN_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This order already has an open warranty request");
        }
        WarrantyRequest warrantyRequest = warrantyRequestRepository.save(new WarrantyRequest(
                order,
                order.getUser(),
                deliveredCredential,
                request.reason().trim(),
                blankToNull(request.evidenceText())));
        auditService.recordSystem("WARRANTY_REQUEST_CREATED", "WARRANTY_REQUEST", warrantyRequest.getId(),
                "orderId=" + order.getId() + ",userId=" + userId);
        adminNotificationRepository.save(new AdminNotification(null,
                "Yêu cầu bảo hành mới " + order.getOrderCode(),
                "User #" + userId + " gửi bảo hành cho " + order.getService().getName()));
        return WarrantyRequestResponse.from(warrantyRequest);
    }

    @Transactional(readOnly = true)
    public List<WarrantyRequestResponse> listForUser(Long userId, Integer limit) {
        return warrantyRequestRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, page(limit))
                .stream()
                .map(WarrantyRequestResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WarrantyRequestResponse> listForAdmin(WarrantyRequestStatus status, Integer limit) {
        List<WarrantyRequest> requests = status == null
                ? warrantyRequestRepository.findAllByOrderByCreatedAtDesc(page(limit))
                : warrantyRequestRepository.findAllByStatusOrderByCreatedAtDesc(status, page(limit));
        return requests.stream().map(WarrantyRequestResponse::from).toList();
    }

    @Transactional
    public WarrantyRequestResponse review(Long adminUserId, Long warrantyRequestId,
            AdminWarrantyReviewRequest request) {
        WarrantyRequest warrantyRequest = warrantyRequestRepository.findByIdForUpdate(warrantyRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Warranty request not found"));
        if (isTerminal(warrantyRequest.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Warranty request is already resolved");
        }
        String adminNote = blankToNull(request.adminNote());
        switch (request.status()) {
            case REVIEWING -> warrantyRequest.markReviewing(adminNote);
            case APPROVED_REPLACE -> approveReplace(adminUserId, warrantyRequest, request.replacementCredentialId(),
                    adminNote);
            case APPROVED_REFUND -> approveRefund(adminUserId, warrantyRequest, adminNote);
            case REJECTED -> warrantyRequest.reject(requireNote(adminNote, "Reject reason is required"));
            case RESOLVED -> warrantyRequest.resolve(adminNote);
            case OPEN -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot move warranty back to OPEN");
        }
        auditService.recordAdmin(adminUserId, "WARRANTY_REQUEST_REVIEWED", "WARRANTY_REQUEST",
                warrantyRequest.getId(), "status=" + warrantyRequest.getStatus());
        userNotificationService.create(warrantyRequest.getUser().getId(),
                "Cập nhật bảo hành đơn " + warrantyRequest.getOrder().getOrderCode(),
                "Yêu cầu bảo hành đã chuyển sang trạng thái " + warrantyRequest.getStatus(),
                "ORDER",
                "/orders/" + warrantyRequest.getOrder().getOrderCode());
        return WarrantyRequestResponse.from(warrantyRequest);
    }

    private void approveReplace(Long adminUserId, WarrantyRequest warrantyRequest, Long replacementCredentialId,
            String adminNote) {
        if (warrantyRequest.getOrder().getDeliveredCredential() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This order has no delivered credential to replace");
        }
        AccountCredential replacement = accountInventoryService.replaceForOrder(
                adminUserId,
                warrantyRequest.getOrder(),
                replacementCredentialId);
        warrantyRequest.getOrder().setResultData("{\"deliveryType\":\"ACCOUNT_CREDENTIAL_REPLACED\","
                + "\"credentialId\":" + replacement.getId() + ","
                + "\"replacedAt\":\"" + Instant.now() + "\"}");
        warrantyRequest.approveReplace(replacement, adminNote);
    }

    private void approveRefund(Long adminUserId, WarrantyRequest warrantyRequest, String adminNote) {
        String reason = adminNote == null || adminNote.isBlank()
                ? "Warranty refund for order " + warrantyRequest.getOrder().getOrderCode()
                : adminNote;
        orderService.refund(warrantyRequest.getOrder().getOrderCode(), adminUserId, new RefundOrderRequest(reason));
        warrantyRequest.approveRefund(warrantyRequest.getOrder().getRefundTransaction(), reason);
    }

    private boolean isTerminal(WarrantyRequestStatus status) {
        return status == WarrantyRequestStatus.APPROVED_REPLACE
                || status == WarrantyRequestStatus.APPROVED_REFUND
                || status == WarrantyRequestStatus.REJECTED
                || status == WarrantyRequestStatus.RESOLVED;
    }

    private String requireNote(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private PageRequest page(Integer limit) {
        int normalizedLimit = limit == null ? 100 : Math.max(1, Math.min(limit, 200));
        return PageRequest.of(0, normalizedLimit);
    }
}
