package com.example.KendyDigital.service.product_inventory.impl;

import com.example.KendyDigital.service.product_inventory.*;

import com.example.KendyDigital.dto.inventory.request.BulkAccountCredentialImportRequest;
import com.example.KendyDigital.dto.inventory.request.CreateAccountCredentialRequest;
import com.example.KendyDigital.dto.inventory.request.UpdateAccountCredentialRequest;
import com.example.KendyDigital.dto.inventory.response.AccountCredentialAdminResponse;
import com.example.KendyDigital.dto.inventory.response.AccountCredentialRevealResponse;
import com.example.KendyDigital.dto.inventory.response.BulkAccountCredentialImportResponse;
import com.example.KendyDigital.dto.inventory.response.InventoryAlertSummaryResponse;
import com.example.KendyDigital.dto.inventory.response.UserAccountCredentialResponse;
import com.example.KendyDigital.model.catalog.ServiceItem;
import com.example.KendyDigital.model.catalog.ServiceStockStatus;
import com.example.KendyDigital.model.catalog.ServiceType;
import com.example.KendyDigital.model.checkout.CheckoutSession;
import com.example.KendyDigital.model.inventory.AccountCredential;
import com.example.KendyDigital.model.inventory.AccountCredentialStatus;
import com.example.KendyDigital.model.inventory.StockImportBatch;
import com.example.KendyDigital.model.order.OrderRecord;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserRole;
import com.example.KendyDigital.repository.AccountCredentialRepository;
import com.example.KendyDigital.repository.ServiceItemRepository;
import com.example.KendyDigital.repository.StockImportBatchRepository;
import com.example.KendyDigital.service.audit.AuditService;
import com.example.KendyDigital.service.user_management.AdminRoleService;
import com.example.KendyDigital.service.product_inventory.helper.CredentialCsvParser;
import com.example.KendyDigital.service.product_inventory.helper.CredentialHasher;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountInventoryServiceImpl implements AccountInventoryService {
    private final AccountCredentialRepository accountCredentialRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final AuditService auditService;
    private final AdminRoleService adminRoleService;
    private final EntityManager entityManager;
    private final StockImportBatchRepository stockImportBatchRepository;
    private final CredentialCsvParser credentialCsvParser;
    private final CredentialHasher credentialHasher;

    public AccountInventoryServiceImpl(AccountCredentialRepository accountCredentialRepository,
            ServiceItemRepository serviceItemRepository,
            AuditService auditService,
            AdminRoleService adminRoleService,
            EntityManager entityManager,
            StockImportBatchRepository stockImportBatchRepository,
            CredentialCsvParser credentialCsvParser,
            CredentialHasher credentialHasher) {
        this.accountCredentialRepository = accountCredentialRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.auditService = auditService;
        this.adminRoleService = adminRoleService;
        this.entityManager = entityManager;
        this.stockImportBatchRepository = stockImportBatchRepository;
        this.credentialCsvParser = credentialCsvParser;
        this.credentialHasher = credentialHasher;
    }

    @Transactional(readOnly = true)
    public List<UserAccountCredentialResponse> listForUser(Long userId, Integer limit) {
        return accountCredentialRepository.findAllDeliveredForUser(userId, page(normalizedLimit(limit)))
                .stream()
                .map(UserAccountCredentialResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AccountCredentialAdminResponse> listByService(Long serviceId, AccountCredentialStatus status,
            Integer limit) {
        int normalizedLimit = normalizedLimit(limit);
        List<AccountCredential> credentials = status == null
                ? accountCredentialRepository.findAllByService_IdOrderByCreatedAtDesc(serviceId, page(normalizedLimit))
                : accountCredentialRepository.findAllByService_IdAndStatusOrderByCreatedAtDesc(serviceId, status,
                        page(normalizedLimit));
        return credentials.stream().map(AccountCredentialAdminResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AccountCredentialAdminResponse> searchByService(Long serviceId, AccountCredentialStatus status,
            String query, Instant createdFrom, Instant createdTo, Instant deliveredFrom, Instant deliveredTo,
            Instant expiresBefore, Integer limit) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        return accountCredentialRepository.findAll(
                        com.example.KendyDigital.repository.specification.AccountCredentialSpecifications.searchForAdmin(
                                serviceId,
                                status,
                                normalizedQuery,
                                createdFrom,
                                createdTo,
                                deliveredFrom,
                                deliveredTo,
                                expiresBefore),
                        page(normalizedLimit(limit))).getContent()
                .stream()
                .map(AccountCredentialAdminResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AccountCredentialAdminResponse> searchAssigned(AccountCredentialStatus status, String query,
            Instant deliveredFrom, Instant deliveredTo, Instant expiresBefore, Integer limit) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        return accountCredentialRepository.findAll(
                        com.example.KendyDigital.repository.specification.AccountCredentialSpecifications.searchAssignedForAdmin(
                                status,
                                normalizedQuery,
                                deliveredFrom,
                                deliveredTo,
                                expiresBefore),
                        page(normalizedLimit(limit))).getContent()
                .stream()
                .map(AccountCredentialAdminResponse::from)
                .toList();
    }

    @Transactional
    public AccountCredentialAdminResponse create(Long adminUserId, Long serviceId, CreateAccountCredentialRequest request) {
        ServiceItem service = serviceItemRepository.findById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
        if (accountCredentialRepository.existsByService_IdAndLoginIdentifierIgnoreCase(serviceId,
                request.loginIdentifier().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Credential login already exists for this service");
        }
        String passwordSecret = request.passwordSecret().trim();
        String payloadHash = credentialHasher.computePayloadHash(serviceId, request.loginIdentifier().trim(), passwordSecret,
                blankToNull(request.recoveryInfo()), blankToNull(request.twoFactorSecret()));
        if (accountCredentialRepository.existsByService_IdAndPayloadHash(serviceId, payloadHash)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Credential payload already exists for this service");
        }

        AccountCredential credential = new AccountCredential(
                service,
                request.loginIdentifier().trim(),
                passwordSecret,
                blankToNull(request.recoveryInfo()),
                blankToNull(request.twoFactorSecret()),
                blankToNull(request.usageNote()),
                blankToNull(request.internalNote()),
                request.expiresAt(),
                request.warrantyUntil());
        credential.updatePayloadHash(payloadHash);
        credential = accountCredentialRepository.save(credential);
        if (service.getType() == ServiceType.ACCOUNT_STOCK && service.getStockStatus() == ServiceStockStatus.OUT_OF_STOCK) {
            service.updatePricingMetadata(ServiceStockStatus.AVAILABLE, service.getCtaType(), service.getPricingBadge(),
                    service.isFeatured(), service.isPublicVisible());
        }
        stockImportBatchRepository.save(new StockImportBatch(service, adminUserId, 1, "Single import login: " + credential.getLoginIdentifier()));
        auditService.recordAdmin(adminUserId, "ACCOUNT_CREDENTIAL_CREATED", "ACCOUNT_CREDENTIAL", credential.getId(),
                "serviceId=" + serviceId + ",login=" + credential.getLoginIdentifier());
        return AccountCredentialAdminResponse.from(credential);
    }

    @Transactional
    public BulkAccountCredentialImportResponse bulkImport(Long adminUserId, Long serviceId,
            BulkAccountCredentialImportRequest request) {
        ServiceItem service = serviceItemRepository.findById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
        List<CreateAccountCredentialRequest> rows = new ArrayList<>();
        if (request.credentials() != null) {
            rows.addAll(request.credentials());
        }
        rows.addAll(credentialCsvParser.parseCsv(request.csvContent()));
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No credentials to import");
        }

        boolean skipDuplicates = Boolean.TRUE.equals(request.skipDuplicates());
        List<AccountCredentialAdminResponse> created = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int skipped = 0;
        for (int index = 0; index < rows.size(); index++) {
            CreateAccountCredentialRequest row = rows.get(index);
            try {
                if (row.loginIdentifier() == null || row.loginIdentifier().isBlank()
                        || row.passwordSecret() == null || row.passwordSecret().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing login/password");
                }
                String login = row.loginIdentifier().trim();
                String password = row.passwordSecret().trim();
                String hash = credentialHasher.computePayloadHash(serviceId, login, password, blankToNull(row.recoveryInfo()),
                        blankToNull(row.twoFactorSecret()));
                boolean duplicate = accountCredentialRepository.existsByService_IdAndLoginIdentifierIgnoreCase(serviceId, login)
                        || accountCredentialRepository.existsByService_IdAndPayloadHash(serviceId, hash);
                if (duplicate) {
                    if (skipDuplicates) {
                        skipped++;
                        continue;
                    }
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate credential");
                }
                AccountCredential credential = new AccountCredential(
                        service,
                        login,
                        password,
                        blankToNull(row.recoveryInfo()),
                        blankToNull(row.twoFactorSecret()),
                        blankToNull(row.usageNote()),
                        blankToNull(row.internalNote()),
                        row.expiresAt(),
                        row.warrantyUntil());
                credential.updatePayloadHash(hash);
                created.add(AccountCredentialAdminResponse.from(accountCredentialRepository.save(credential)));
            } catch (Exception exception) {
                errors.add("row " + (index + 1) + ": " + exception.getMessage());
                if (!skipDuplicates) {
                    throw exception;
                }
                skipped++;
            }
        }
        if (!created.isEmpty() && service.getType() == ServiceType.ACCOUNT_STOCK) {
            service.updatePricingMetadata(ServiceStockStatus.AVAILABLE, service.getCtaType(), service.getPricingBadge(),
                    service.isFeatured(), service.isPublicVisible());
        }
        if (!created.isEmpty()) {
            stockImportBatchRepository.save(new StockImportBatch(service, adminUserId, created.size(), "Bulk import: " + created.size() + " items success, " + skipped + " skipped."));
        }
        auditService.recordAdmin(adminUserId, "ACCOUNT_CREDENTIAL_BULK_IMPORTED", "SERVICE", serviceId,
                "created=" + created.size() + ",skipped=" + skipped);
        return new BulkAccountCredentialImportResponse(created.size(), skipped, errors, created);
    }

    @Transactional
    public AccountCredentialRevealResponse reveal(Long adminUserId, UserRole adminRole, Long credentialId) {
        requireCredentialViewPermission(adminUserId, adminRole);
        AccountCredential credential = accountCredentialRepository.findById(credentialId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Credential not found"));
        auditService.recordAdmin(adminUserId, "ACCOUNT_CREDENTIAL_REVEALED", "ACCOUNT_CREDENTIAL",
                credential.getId(), "serviceId=" + credential.getService().getId());
        return AccountCredentialRevealResponse.from(credential);
    }

    @Transactional
    public AccountCredentialAdminResponse update(Long adminUserId, Long credentialId,
            UpdateAccountCredentialRequest request) {
        AccountCredential credential = accountCredentialRepository.findById(credentialId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Credential not found"));
        if (request.version() != null && !request.version().equals(credential.getVersion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CONCURRENT_ADMIN_CONFLICT");
        }
        if (credential.getStatus() == AccountCredentialStatus.DELIVERED && request.status() == AccountCredentialStatus.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Delivered credential cannot be returned to stock");
        }
        if (credential.getStatus() == AccountCredentialStatus.RESERVED
                && request.status() != null
                && request.status() != AccountCredentialStatus.RESERVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reserved credential cannot be changed manually");
        }
        String nextLoginIdentifier = valueOrCurrent(request.loginIdentifier(), credential.getLoginIdentifier());
        String nextPasswordSecret = valueOrCurrent(request.passwordSecret(), credential.getPasswordSecret());
        String nextRecoveryInfo = nullableOrCurrent(request.recoveryInfo(), credential.getRecoveryInfo());
        String nextTwoFactorSecret = nullableOrCurrent(request.twoFactorSecret(), credential.getTwoFactorSecret());
        if (!nextLoginIdentifier.equalsIgnoreCase(credential.getLoginIdentifier())
                && accountCredentialRepository.existsByService_IdAndLoginIdentifierIgnoreCase(
                        credential.getService().getId(),
                        nextLoginIdentifier)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Credential login already exists for this service");
        }
        String nextPayloadHash = credentialHasher.computePayloadHash(credential.getService().getId(), nextLoginIdentifier, nextPasswordSecret,
                nextRecoveryInfo, nextTwoFactorSecret);
        if (!nextPayloadHash.equals(credential.getPayloadHash())
                && accountCredentialRepository.existsByService_IdAndPayloadHash(credential.getService().getId(),
                        nextPayloadHash)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Credential payload already exists for this service");
        }

        credential.updateSecrets(
                nextLoginIdentifier,
                nextPasswordSecret,
                nextRecoveryInfo,
                nextTwoFactorSecret,
                nullableOrCurrent(request.usageNote(), credential.getUsageNote()),
                nullableOrCurrent(request.internalNote(), credential.getInternalNote()),
                request.expiresAt() == null ? credential.getExpiresAt() : request.expiresAt(),
                request.warrantyUntil() == null ? credential.getWarrantyUntil() : request.warrantyUntil());
        credential.updatePayloadHash(nextPayloadHash);
        if (request.status() != null && request.status() != credential.getStatus()) {
            credential.changeStatus(request.status());
        }
        auditService.recordAdmin(adminUserId, "ACCOUNT_CREDENTIAL_UPDATED", "ACCOUNT_CREDENTIAL", credential.getId(),
                "status=" + credential.getStatus());
        syncServiceStockStatus(credential.getService());
        return AccountCredentialAdminResponse.from(credential);
    }

    @Transactional
    public AccountCredentialAdminResponse disable(Long adminUserId, Long credentialId) {
        AccountCredential credential = accountCredentialRepository.findById(credentialId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Credential not found"));
        if (credential.getStatus() == AccountCredentialStatus.DELIVERED
                || credential.getStatus() == AccountCredentialStatus.RESERVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Delivered or reserved credential cannot be disabled here");
        }
        credential.changeStatus(AccountCredentialStatus.DISABLED);
        auditService.recordAdmin(adminUserId, "ACCOUNT_CREDENTIAL_DISABLED", "ACCOUNT_CREDENTIAL", credential.getId(),
                "serviceId=" + credential.getService().getId());
        syncServiceStockStatus(credential.getService());
        return AccountCredentialAdminResponse.from(credential);
    }

    @Transactional
    public AccountCredential takeAvailableForOrder(Long serviceId) {
        return accountCredentialRepository.findAvailableForDelivery(serviceId, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "No account credentials available for this service"));
    }

    @Transactional
    public AccountCredential reserveForCheckout(Long serviceId, CheckoutSession checkout, UserAccount user,
            Instant reservedUntil) {
        releaseExpiredReservations(Instant.now());
        AccountCredential credential = takeAvailableForOrder(serviceId);
        credential.reserve(checkout, user, reservedUntil);
        auditService.recordSystem("ACCOUNT_CREDENTIAL_RESERVED", "ACCOUNT_CREDENTIAL", credential.getId(),
                "checkoutId=" + checkout.getId() + ",userId=" + user.getId());
        syncServiceStockStatus(credential.getService());
        return credential;
    }

    @Transactional
    public AccountCredential takeReservedForOrder(Long serviceId, Long checkoutId, Long userId) {
        AccountCredential credential = accountCredentialRepository.findReservedForCheckout(serviceId, checkoutId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Reserved account credential not found for this checkout"));
        if (credential.getReservedByUser() == null || !credential.getReservedByUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Reserved account credential does not belong to this user");
        }
        if (credential.getReservedUntil() != null && credential.getReservedUntil().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Reserved account credential has expired");
        }
        return credential;
    }

    @Transactional
    public boolean hasUsableReservation(Long serviceId, Long checkoutId, Long userId, Instant now) {
        return accountCredentialRepository.findReservedForCheckout(serviceId, checkoutId)
                .filter(credential -> credential.getReservedByUser() != null
                        && credential.getReservedByUser().getId().equals(userId))
                .filter(credential -> credential.getReservedUntil() == null
                        || !credential.getReservedUntil().isBefore(now))
                .isPresent();
    }

    @Transactional
    public void releaseReservationForCheckout(Long checkoutId) {
        accountCredentialRepository.findReservedByCheckoutId(checkoutId).ifPresent(credential -> {
            credential.releaseReservation();
            auditService.recordSystem("ACCOUNT_CREDENTIAL_RESERVATION_RELEASED", "ACCOUNT_CREDENTIAL",
                    credential.getId(), "checkoutId=" + checkoutId);
            syncServiceStockStatus(credential.getService());
        });
    }

    @Transactional
    public int releaseExpiredReservations(Instant now) {
        int released = 0;
        while (true) {
            List<AccountCredential> expiredCredentials =
                    accountCredentialRepository.findExpiredReservations(now, PageRequest.of(0, 200));
            expiredCredentials.forEach(credential -> {
                Long checkoutId =
                        credential.getReservedCheckout() == null ? null : credential.getReservedCheckout().getId();
                credential.releaseReservation();
                auditService.recordSystem("ACCOUNT_CREDENTIAL_RESERVATION_EXPIRED", "ACCOUNT_CREDENTIAL",
                        credential.getId(), "checkoutId=" + checkoutId);
                syncServiceStockStatus(credential.getService());
            });
            released += expiredCredentials.size();
            if (expiredCredentials.size() < 200) {
                return released;
            }
            entityManager.flush();
            entityManager.clear();
        }
    }

    @Transactional(readOnly = true)
    public long availableCount(Long serviceId) {
        return accountCredentialRepository.countByService_IdAndStatus(serviceId, AccountCredentialStatus.AVAILABLE);
    }

    @Transactional
    public AccountCredential replaceForOrder(Long adminUserId, OrderRecord order, Long replacementCredentialId) {
        AccountCredential current = order.getDeliveredCredential();
        if (current != null) {
            current.markReplaced();
            auditService.recordAdmin(adminUserId, "ACCOUNT_CREDENTIAL_REPLACED_OLD", "ACCOUNT_CREDENTIAL",
                    current.getId(), "orderId=" + order.getId());
            entityManager.flush();
        }
        AccountCredential replacement = replacementCredentialId == null
                ? takeAvailableForOrder(order.getService().getId())
                : accountCredentialRepository.findById(replacementCredentialId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Replacement credential not found"));
        if (!replacement.getService().getId().equals(order.getService().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Replacement credential belongs to another service");
        }
        if (replacement.getStatus() != AccountCredentialStatus.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Replacement credential is not available");
        }
        replacement.deliver(order, order.getUser());
        order.attachDeliveredCredential(replacement);
        auditService.recordAdmin(adminUserId, "ACCOUNT_CREDENTIAL_REPLACEMENT_DELIVERED", "ACCOUNT_CREDENTIAL",
                replacement.getId(), "orderId=" + order.getId());
        syncServiceStockStatus(order.getService());
        return replacement;
    }

    @Transactional
    public InventoryAlertSummaryResponse alertSummary(Integer lowStockThreshold, Integer expiringDays) {
        int released = releaseExpiredReservations(Instant.now());
        long threshold = lowStockThreshold == null ? 2 : Math.max(0, lowStockThreshold);
        int days = expiringDays == null ? 7 : Math.max(1, expiringDays);
        Instant now = Instant.now();
        return new InventoryAlertSummaryResponse(
                accountCredentialRepository.countLowStockServices(threshold),
                accountCredentialRepository.countExpiringCredentials(now, now.plus(days, ChronoUnit.DAYS)),
                released);
    }

    private void syncServiceStockStatus(ServiceItem service) {
        if (service.getType() != ServiceType.ACCOUNT_STOCK) {
            return;
        }
        ServiceStockStatus nextStatus = availableCount(service.getId()) > 0
                ? ServiceStockStatus.AVAILABLE
                : ServiceStockStatus.OUT_OF_STOCK;
        if (service.getStockStatus() != nextStatus) {
            service.updatePricingMetadata(nextStatus, service.getCtaType(), service.getPricingBadge(),
                    service.isFeatured(), service.isPublicVisible());
        }
    }

    private PageRequest page(int limit) {
        return PageRequest.of(0, limit);
    }

    private int normalizedLimit(Integer limit) {
        return limit == null ? 100 : Math.max(1, Math.min(limit, 200));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String valueOrCurrent(String value, String current) {
        return value == null || value.isBlank() ? current : value.trim();
    }

    private String nullableOrCurrent(String value, String current) {
        return value == null ? current : blankToNull(value);
    }

    private void requireCredentialViewPermission(Long adminUserId, UserRole adminRole) {
        if (adminRole == UserRole.SUPER_ADMIN) {
            return;
        }
        if (adminRole != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin permission required");
        }
        List<String> permissions = adminRoleService.getUserEffectivePermissions(adminUserId);
        if (!permissions.contains("CREDENTIAL_VIEW") && !permissions.contains("CREDENTIAL_MANAGE")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Credential view permission required");
        }
    }
}

