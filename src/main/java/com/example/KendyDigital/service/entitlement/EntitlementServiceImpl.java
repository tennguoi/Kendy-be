package com.example.KendyDigital.service.entitlement;

import com.example.KendyDigital.dto.entitlement.request.AdminEntitlementUpdateRequest;
import com.example.KendyDigital.dto.entitlement.response.UserEntitlementResponse;
import com.example.KendyDigital.dto.ticket.request.CreateTicketRequest;
import com.example.KendyDigital.model.admin.AdminNotification;
import com.example.KendyDigital.model.catalog.AccessStrategy;
import com.example.KendyDigital.model.entitlement.EntitlementStatus;
import com.example.KendyDigital.model.entitlement.UserEntitlement;
import com.example.KendyDigital.model.inventory.AccountCredential;
import com.example.KendyDigital.model.inventory.AccountCredentialStatus;
import com.example.KendyDigital.model.order.OrderRecord;
import com.example.KendyDigital.model.order.OrderStatus;
import com.example.KendyDigital.model.ticket.TicketCategory;
import com.example.KendyDigital.model.ticket.TicketPriority;
import com.example.KendyDigital.repository.AdminNotificationRepository;
import com.example.KendyDigital.repository.OrderRepository;
import com.example.KendyDigital.repository.UserEntitlementRepository;
import com.example.KendyDigital.service.audit.AuditService;
import com.example.KendyDigital.service.notification.UserNotificationService;
import com.example.KendyDigital.service.ticket.TicketService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EntitlementServiceImpl implements EntitlementService {
    private static final List<EntitlementStatus> EXPIRABLE =
            List.of(EntitlementStatus.ACTIVE, EntitlementStatus.EXPIRING);

    private final UserEntitlementRepository entitlementRepository;
    private final OrderRepository orderRepository;
    private final TicketService ticketService;
    private final AuditService auditService;
    private final UserNotificationService userNotificationService;
    private final AdminNotificationRepository adminNotificationRepository;

    public EntitlementServiceImpl(UserEntitlementRepository entitlementRepository,
            OrderRepository orderRepository,
            TicketService ticketService,
            AuditService auditService,
            UserNotificationService userNotificationService,
            AdminNotificationRepository adminNotificationRepository) {
        this.entitlementRepository = entitlementRepository;
        this.orderRepository = orderRepository;
        this.ticketService = ticketService;
        this.auditService = auditService;
        this.userNotificationService = userNotificationService;
        this.adminNotificationRepository = adminNotificationRepository;
    }

    @Override
    @Transactional
    public void createForOrder(OrderRecord order, AccountCredential credential) {
        if (entitlementRepository.existsBySourceOrder_Id(order.getId())) {
            return;
        }
        AccessStrategy strategy = order.getService().resolvedAccessStrategy();
        String accessIdentifier = credential != null
                ? credential.getLoginIdentifier()
                : order.getUser().getEmail();
        UserEntitlement entitlement = new UserEntitlement(
                order.getUser(), order.getService(), order, strategy, accessIdentifier);
        entitlementRepository.save(entitlement);
        if (order.getStatus() == OrderStatus.COMPLETED) {
            activate(entitlement, credential);
        }
        auditService.recordSystem("ENTITLEMENT_CREATED", "ENTITLEMENT", entitlement.getId(),
                "orderId=" + order.getId() + ",strategy=" + strategy);
    }

    @Override
    @Transactional
    public void activateForOrder(OrderRecord order) {
        UserEntitlement entitlement = entitlementRepository.findBySourceOrder_Id(order.getId())
                .orElseGet(() -> entitlementRepository.save(new UserEntitlement(
                        order.getUser(),
                        order.getService(),
                        order,
                        order.getService().resolvedAccessStrategy(),
                        order.getDeliveredCredential() == null
                                ? order.getUser().getEmail()
                                : order.getDeliveredCredential().getLoginIdentifier())));
        activate(entitlement, order.getDeliveredCredential());
    }

    @Override
    @Transactional
    public void revokeForOrder(OrderRecord order, String reason) {
        entitlementRepository.findBySourceOrder_Id(order.getId()).ifPresent(entitlement -> {
            entitlement.revoke(reason);
            AccountCredential credential = order.getDeliveredCredential();
            if (credential != null && credential.getStatus() == AccountCredentialStatus.DELIVERED) {
                credential.changeStatus(AccountCredentialStatus.DISABLED);
            }
            auditService.recordSystem("ENTITLEMENT_REVOKED", "ENTITLEMENT", entitlement.getId(), reason);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserEntitlementResponse> listForUser(Long userId, Integer limit) {
        int size = limit == null ? 100 : Math.max(1, Math.min(limit, 200));
        return entitlementRepository.findAllForUser(userId, PageRequest.of(0, size))
                .stream()
                .map(UserEntitlementResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserEntitlementResponse> listForAdmin(Integer limit) {
        int size = limit == null ? 200 : Math.max(1, Math.min(limit, 500));
        return entitlementRepository.findAllForAdmin(PageRequest.of(0, size))
                .stream()
                .map(UserEntitlementResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public UserEntitlementResponse requestRenewal(Long userId, Long entitlementId) {
        UserEntitlement entitlement = entitlementRepository.findByIdForUserUpdate(entitlementId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entitlement not found"));
        if (entitlement.getStatus() == EntitlementStatus.REVOKED
                || entitlement.getStatus() == EntitlementStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This access cannot be renewed");
        }
        if (entitlement.getRenewalRequestedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A renewal request is already pending");
        }
        entitlement.requestRenewal();
        ticketService.create(userId, new CreateTicketRequest(
                TicketCategory.SERVICE,
                "Gia hạn " + entitlement.getService().getName(),
                "Yêu cầu gia hạn entitlement #" + entitlement.getId()
                        + ". Giữ nguyên member/resource hiện tại; không cấp tài khoản chủ hoặc credential mới.",
                entitlement.getSourceOrder().getOrderCode(),
                null,
                TicketPriority.NORMAL));
        auditService.recordSystem("ENTITLEMENT_RENEWAL_REQUESTED", "ENTITLEMENT", entitlement.getId(),
                "userId=" + userId);
        return UserEntitlementResponse.from(entitlement);
    }

    @Override
    @Transactional
    public UserEntitlementResponse updateByAdmin(Long adminUserId, Long entitlementId,
            AdminEntitlementUpdateRequest request) {
        UserEntitlement entitlement = entitlementRepository.findByIdForUpdate(entitlementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entitlement not found"));
        entitlement.updateProviderReference(
                request.accessIdentifier(), request.externalResourceId(), request.providerMetadata());
        String reason = request.reason() == null ? null : request.reason().trim();
        switch (request.action()) {
            case ACTIVATE -> activate(entitlement, entitlement.getSourceOrder().getDeliveredCredential());
            case SUSPEND -> entitlement.suspend(reason);
            case REVOKE -> entitlement.revoke(reason);
            case EXTEND -> {
                if (request.extendDays() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "extendDays is required");
                }
                Instant base = entitlement.getExpiresAt() != null
                        && entitlement.getExpiresAt().isAfter(Instant.now())
                        ? entitlement.getExpiresAt()
                        : Instant.now();
                entitlement.extendTo(base.plus(request.extendDays(), ChronoUnit.DAYS));
            }
        }
        synchronizeCredentialStatus(entitlement);
        auditService.recordAdmin(adminUserId, "ENTITLEMENT_" + request.action(), "ENTITLEMENT",
                entitlement.getId(), reason);
        userNotificationService.create(entitlement.getUser().getId(),
                "Cập nhật quyền truy cập",
                entitlement.getService().getName() + " đã chuyển sang " + entitlement.getStatus(),
                "SERVICE",
                "/locker");
        return UserEntitlementResponse.from(entitlement);
    }

    @Override
    @Transactional
    public int backfillExistingOrders() {
        int created = 0;
        for (OrderRecord order : orderRepository.findAllByStatusOrderByCreatedAtDesc(
                OrderStatus.COMPLETED, PageRequest.of(0, 5000))) {
            if (!entitlementRepository.existsBySourceOrder_Id(order.getId())) {
                createForOrder(order, order.getDeliveredCredential());
                created++;
            }
        }
        return created;
    }

    @Override
    @Transactional
    public int processLifecycle() {
        Instant now = Instant.now();
        Instant warningUntil = now.plus(3, ChronoUnit.DAYS);
        int changed = 0;
        for (UserEntitlement entitlement : entitlementRepository
                .findAllByStatusAndExpiresAtIsNotNullAndExpiresAtBetweenOrderByExpiresAtAsc(
                        EntitlementStatus.ACTIVE, now, warningUntil, PageRequest.of(0, 500))) {
            entitlement.markExpiring();
            userNotificationService.create(entitlement.getUser().getId(),
                    "Dịch vụ sắp hết hạn",
                    entitlement.getService().getName() + " sẽ hết hạn vào " + entitlement.getExpiresAt(),
                    "SERVICE",
                    "/locker");
            changed++;
        }
        for (UserEntitlement entitlement : entitlementRepository
                .findAllByStatusInAndExpiresAtIsNotNullAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
                        EXPIRABLE, now, PageRequest.of(0, 500))) {
            entitlement.suspend("Access period expired; provider access must be removed or suspended");
            AccountCredential credential = entitlement.getSourceOrder().getDeliveredCredential();
            if (credential != null && credential.getStatus() == AccountCredentialStatus.DELIVERED) {
                credential.changeStatus(AccountCredentialStatus.EXPIRED);
            }
            adminNotificationRepository.save(new AdminNotification(
                    null,
                    "Cần thu hồi quyền truy cập",
                    "Entitlement #" + entitlement.getId() + " - "
                            + entitlement.getService().getName() + " đã hết hạn. Strategy: "
                            + entitlement.getAccessStrategy()));
            userNotificationService.create(entitlement.getUser().getId(),
                    "Dịch vụ đã hết hạn",
                    entitlement.getService().getName()
                            + " đã bị tạm ngưng. Gia hạn để tiếp tục sử dụng.",
                    "SERVICE",
                    "/locker");
            auditService.recordSystem("ENTITLEMENT_EXPIRED", "ENTITLEMENT", entitlement.getId(), null);
            changed++;
        }
        return changed;
    }

    private void activate(UserEntitlement entitlement, AccountCredential credential) {
        Instant startsAt = entitlement.getStartsAt() == null ? Instant.now() : entitlement.getStartsAt();
        Instant expiresAt = credential == null ? null : credential.getExpiresAt();
        if (expiresAt == null && entitlement.getService().getAccessDurationDays() != null) {
            expiresAt = startsAt.plus(entitlement.getService().getAccessDurationDays(), ChronoUnit.DAYS);
        }
        entitlement.activate(
                startsAt,
                expiresAt,
                entitlement.getExternalResourceId(),
                entitlement.getProviderMetadata());
        synchronizeCredentialStatus(entitlement);
    }

    private void synchronizeCredentialStatus(UserEntitlement entitlement) {
        AccountCredential credential = entitlement.getSourceOrder().getDeliveredCredential();
        if (credential == null) {
            return;
        }
        switch (entitlement.getStatus()) {
            case ACTIVE, EXPIRING -> {
                if (credential.getStatus() == AccountCredentialStatus.EXPIRED
                        || credential.getStatus() == AccountCredentialStatus.DISABLED) {
                    credential.changeStatus(AccountCredentialStatus.DELIVERED);
                }
            }
            case SUSPENDED -> credential.changeStatus(AccountCredentialStatus.EXPIRED);
            case REVOKED, FAILED -> credential.changeStatus(AccountCredentialStatus.DISABLED);
            case PENDING -> {
                // Credential delivery state is unchanged while provisioning is pending.
            }
        }
    }
}
