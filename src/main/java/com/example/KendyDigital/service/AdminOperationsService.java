package com.example.KendyDigital.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.KendyDigital.config.SePayWebhookProperties;
import com.example.KendyDigital.dto.AdminNotificationResponse;
import com.example.KendyDigital.dto.AdminPermissionsRequest;
import com.example.KendyDigital.dto.AdminRolesRequest;
import com.example.KendyDigital.dto.AdminUserResponse;
import com.example.KendyDigital.dto.AuthSessionResponse;
import com.example.KendyDigital.dto.JobRecordResponse;
import com.example.KendyDigital.dto.OrderResponse;
import com.example.KendyDigital.dto.ReprocessBankTransactionRequest;
import com.example.KendyDigital.dto.StoredFileResponse;
import com.example.KendyDigital.dto.SystemSettingHistoryResponse;
import com.example.KendyDigital.dto.SystemSettingResponse;
import com.example.KendyDigital.dto.SystemSettingsBulkUpdateRequest;
import com.example.KendyDigital.dto.TicketResponse;
import com.example.KendyDigital.dto.TotpSetupResponse;
import com.example.KendyDigital.dto.TwoFactorVerifyRequest;
import com.example.KendyDigital.dto.WebhookConfigRequest;
import com.example.KendyDigital.dto.WebhookRetryRequest;
import com.example.KendyDigital.dto.WalletTransactionResponse;
import com.example.KendyDigital.model.AdminNotification;
import com.example.KendyDigital.model.AuditLog;
import com.example.KendyDigital.model.AuthSession;
import com.example.KendyDigital.model.BankTransaction;
import com.example.KendyDigital.model.JobRecord;
import com.example.KendyDigital.model.OrderRecord;
import com.example.KendyDigital.model.OrderStatus;
import com.example.KendyDigital.model.StoredFile;
import com.example.KendyDigital.model.SystemSetting;
import com.example.KendyDigital.model.SystemSettingHistory;
import com.example.KendyDigital.model.Ticket;
import com.example.KendyDigital.model.UserAccount;
import com.example.KendyDigital.model.UserRole;
import com.example.KendyDigital.model.UserStatus;
import com.example.KendyDigital.model.WalletTransactionDirection;
import com.example.KendyDigital.model.WalletTransactionType;
import com.example.KendyDigital.repository.AdminNotificationRepository;
import com.example.KendyDigital.repository.AuditLogRepository;
import com.example.KendyDigital.repository.AuthSessionRepository;
import com.example.KendyDigital.repository.BankTransactionRepository;
import com.example.KendyDigital.repository.JobRecordRepository;
import com.example.KendyDigital.repository.OrderRepository;
import com.example.KendyDigital.repository.StoredFileRepository;
import com.example.KendyDigital.repository.SystemSettingHistoryRepository;
import com.example.KendyDigital.repository.SystemSettingRepository;
import com.example.KendyDigital.repository.TicketRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.repository.WalletTransactionRepository;

@Service
public class AdminOperationsService {
    private final UserAccountRepository userAccountRepository;
    private final AuthSessionRepository authSessionRepository;
    private final OrderRepository orderRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final TicketRepository ticketRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final SystemSettingHistoryRepository systemSettingHistoryRepository;
    private final AdminNotificationRepository adminNotificationRepository;
    private final StoredFileRepository storedFileRepository;
    private final JobRecordRepository jobRecordRepository;
    private final AuditService auditService;
    private final AdminFinanceService adminFinanceService;
    private final SePayWebhookProperties sePayWebhookProperties;
    private final TwoFactorService twoFactorService;
    private final AdminRoleService adminRoleService;

    public AdminOperationsService(UserAccountRepository userAccountRepository,
            AuthSessionRepository authSessionRepository,
            OrderRepository orderRepository,
            WalletTransactionRepository walletTransactionRepository,
            TicketRepository ticketRepository,
            BankTransactionRepository bankTransactionRepository,
            AuditLogRepository auditLogRepository,
            SystemSettingRepository systemSettingRepository,
            SystemSettingHistoryRepository systemSettingHistoryRepository,
            AdminNotificationRepository adminNotificationRepository,
            StoredFileRepository storedFileRepository,
            JobRecordRepository jobRecordRepository,
            AuditService auditService,
            AdminFinanceService adminFinanceService,
            SePayWebhookProperties sePayWebhookProperties,
            TwoFactorService twoFactorService,
            AdminRoleService adminRoleService) {
        this.userAccountRepository = userAccountRepository;
        this.authSessionRepository = authSessionRepository;
        this.orderRepository = orderRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.ticketRepository = ticketRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.auditLogRepository = auditLogRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.systemSettingHistoryRepository = systemSettingHistoryRepository;
        this.adminNotificationRepository = adminNotificationRepository;
        this.storedFileRepository = storedFileRepository;
        this.jobRecordRepository = jobRecordRepository;
        this.auditService = auditService;
        this.adminFinanceService = adminFinanceService;
        this.sePayWebhookProperties = sePayWebhookProperties;
        this.twoFactorService = twoFactorService;
        this.adminRoleService = adminRoleService;
    }

    @Transactional(readOnly = true)
    public List<AuthSessionResponse> listSessions(Long userId, int page, int size) {
        return authSessionRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, paged(page, size))
                .stream()
                .map(AuthSessionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuthSessionResponse> listSessions(Long userId, Integer limit) {
        return listSessions(userId, 0, limit == null ? 100 : limit);
    }

    @Transactional
    public void revokeSession(Long adminUserId, Long userId, Long sessionId) {
        AuthSession session = authSessionRepository.findByIdAndUser_Id(sessionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        session.revoke();
        auditService.recordAdmin(adminUserId, "AUTH_SESSION_REVOKED", "AUTH_SESSION", session.getId(),
                "userId=" + userId);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listUserOrders(Long userId, int page, int size) {
        return orderRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, paged(page, size))
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listUserOrders(Long userId, Integer limit) {
        return listUserOrders(userId, 0, limit == null ? 100 : limit);
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> listUserWalletTransactions(Long userId, int page, int size) {
        return walletTransactionRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, paged(page, size))
                .stream()
                .map(WalletTransactionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> listUserWalletTransactions(Long userId, Integer limit) {
        return listUserWalletTransactions(userId, 0, limit == null ? 100 : limit);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listUserTickets(Long userId, int page, int size) {
        return ticketRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, paged(page, size))
                .stream()
                .map(ticket -> TicketResponse.from(ticket, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listUserTickets(Long userId, Integer limit) {
        return listUserTickets(userId, 0, limit == null ? 100 : limit);
    }

    @Transactional
    public List<AdminUserResponse> bulkUserStatus(Long adminUserId, List<Long> ids, UserStatus status, String reason) {
        return ids.stream()
                .map(id -> {
                    UserAccount user = userAccountRepository.findByIdForUpdate(id)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                    "User not found: " + id));
                    user.setStatus(status);
                    auditService.recordAdmin(adminUserId, "USER_BULK_STATUS_UPDATED", "USER", user.getId(),
                            "status=" + status + ",reason=" + blankToNull(reason));
                    return AdminUserResponse.from(user);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listAdmins(Integer limit) {
        return userAccountRepository.findAllByRoleInOrderByCreatedAtDesc(
                        List.of(UserRole.ADMIN, UserRole.SUPER_ADMIN), page(limit))
                .stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    @Transactional
    public TotpSetupResponse setupTwoFactor(Long adminUserId, Long targetAdminId) {
        UserAccount admin = requireAdminForUpdate(targetAdminId);
        String secret = twoFactorService.generateSecret();
        List<String> backupCodes = twoFactorService.generateBackupCodes();
        String qrBase64 = twoFactorService.qrCodeBase64(secret, admin.getEmail(), "KendyDigital");
        admin.setTwoFactorSecret(secret);
        admin.setBackupCodes(twoFactorService.hashStoredBackupCodes(backupCodes));
        auditService.recordAdmin(adminUserId, "ADMIN_2FA_SETUP", "USER", admin.getId(), null);
        return new TotpSetupResponse(secret, qrBase64, backupCodes);
    }

    @Transactional
    public AdminUserResponse verifyAndEnableTwoFactor(Long adminUserId, Long targetAdminId,
            TwoFactorVerifyRequest request) {
        UserAccount admin = requireAdminForUpdate(targetAdminId);
        if (admin.getTwoFactorSecret() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "2FA not initialized. Call setup first.");
        }
        if (twoFactorService.verify(admin.getTwoFactorSecret(), request.code())) {
            admin.enableTwoFactor(admin.getTwoFactorSecret(), admin.getBackupCodes());
            auditService.recordAdmin(adminUserId, "ADMIN_2FA_ENABLED", "USER", admin.getId(), null);
            return AdminUserResponse.from(admin);
        }
        if (twoFactorService.verifyBackupCode(admin.getBackupCodes(), request.code())) {
            admin.setBackupCodes(twoFactorService.removeUsedBackupCode(admin.getBackupCodes(), request.code()));
            admin.enableTwoFactor(admin.getTwoFactorSecret(), admin.getBackupCodes());
            auditService.recordAdmin(adminUserId, "ADMIN_2FA_ENABLED_VIA_BACKUP", "USER", admin.getId(), null);
            return AdminUserResponse.from(admin);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification code");
    }

    @Transactional
    public AdminUserResponse disableTwoFactor(Long adminUserId, Long targetAdminId) {
        UserAccount admin = requireAdminForUpdate(targetAdminId);
        admin.disableTwoFactor();
        auditService.recordAdmin(adminUserId, "ADMIN_2FA_DISABLED", "USER", admin.getId(), null);
        return AdminUserResponse.from(admin);
    }

    @Transactional
    public TotpSetupResponse resetTwoFactor(Long adminUserId, Long targetAdminId) {
        UserAccount admin = requireAdminForUpdate(targetAdminId);
        admin.resetTwoFactor();
        auditService.recordAdmin(adminUserId, "ADMIN_2FA_RESET", "USER", admin.getId(), null);
        return setupTwoFactor(adminUserId, targetAdminId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPermissions(Long adminId) {
        UserAccount admin = requireAdmin(adminId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("adminId", admin.getId());
        response.put("permissions", parsePermissions(admin.getAdminPermissions()));
        return response;
    }

    @Transactional
    public Map<String, Object> updatePermissions(Long adminUserId, Long targetAdminId,
            AdminPermissionsRequest request) {
        UserAccount admin = requireAdminForUpdate(targetAdminId);
        String permissions = request.permissions() == null ? "" : request.permissions().stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.joining(","));
        admin.setAdminPermissions(permissions);
        auditService.recordAdmin(adminUserId, "ADMIN_PERMISSIONS_UPDATED", "USER", admin.getId(),
                "permissions=" + permissions + ",reason=" + blankToNull(request.reason()));
        return getPermissions(admin.getId());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRoles(Long adminId) {
        UserAccount admin = requireAdmin(adminId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("adminId", admin.getId());
        response.put("role", admin.getRole());
        return response;
    }

    @Transactional
    public Map<String, Object> updateRoles(Long adminUserId, Long targetAdminId, AdminRolesRequest request) {
        UserAccount admin = requireAdminForUpdate(targetAdminId);
        if (request.role() == UserRole.USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin role cannot be USER");
        }
        admin.setRole(request.role());
        auditService.recordAdmin(adminUserId, "ADMIN_ROLE_UPDATED", "USER", admin.getId(),
                "role=" + request.role() + ",reason=" + blankToNull(request.reason()));
        return getRoles(admin.getId());
    }

    @Transactional(readOnly = true)
    public List<SystemSettingResponse> searchSettings(String query, Integer limit) {
        return systemSettingRepository.search(normalizeQuery(query), page(limit))
                .stream()
                .map(SystemSettingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SystemSettingHistoryResponse> settingHistory(String key, Integer limit) {
        return systemSettingHistoryRepository.findAllByKeyOrderByCreatedAtDesc(key, page(limit))
                .stream()
                .map(SystemSettingHistoryResponse::from)
                .toList();
    }

    @Transactional
    public List<SystemSettingResponse> bulkUpdateSettings(Long adminUserId, SystemSettingsBulkUpdateRequest request) {
        return request.settings().stream()
                .map(item -> upsertSetting(adminUserId, item.key(), item.value(),
                        item.publicSetting() != null && item.publicSetting()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SystemSettingResponse> backupSettings() {
        return systemSettingRepository.findAll().stream()
                .map(SystemSettingResponse::from)
                .toList();
    }

    @Transactional
    public List<SystemSettingResponse> restoreSettings(Long adminUserId, SystemSettingsBulkUpdateRequest request) {
        List<SystemSettingResponse> restored = bulkUpdateSettings(adminUserId, request);
        auditService.recordAdmin(adminUserId, "SETTINGS_RESTORED", "SYSTEM_SETTING", null,
                "count=" + restored.size());
        return restored;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> sepayStatus() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("requireApiKey", sePayWebhookProperties.isRequireApiKey());
        response.put("apiKeyHeader", sePayWebhookProperties.getApiKeyHeader());
        response.put("requireHmac", sePayWebhookProperties.isRequireHmac());
        response.put("signatureHeader", sePayWebhookProperties.getSignatureHeader());
        response.put("recentWebhookLogs", auditLogRepository.searchAdmin("SEPAY", null, null, null, null, PageRequest.of(0, 10))
                .stream()
                .map(com.example.KendyDigital.dto.AuditLogResponse::from)
                .toList());
        return response;
    }

    @Transactional
    public Object retrySePayWebhook(Long adminUserId, WebhookRetryRequest request) {
        return adminFinanceService.reprocessBankTransaction(adminUserId, request.bankTransactionId(),
                new ReprocessBankTransactionRequest(request.depositCode(), request.reason()));
    }

    @Transactional(readOnly = true)
    public List<com.example.KendyDigital.dto.AuditLogResponse> sepayLogs(Integer limit) {
        return auditLogRepository.searchAdmin("SEPAY", null, null, null, null, page(limit))
                .stream()
                .map(com.example.KendyDigital.dto.AuditLogResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SystemSettingResponse> getSePayConfig() {
        return searchSettings("sepay.", 100);
    }

    @Transactional
    public List<SystemSettingResponse> updateSePayConfig(Long adminUserId, WebhookConfigRequest request) {
        if (request.config() == null) {
            return List.of();
        }
        return request.config().entrySet().stream()
                .map(entry -> upsertSetting(adminUserId, "sepay." + entry.getKey(), entry.getValue(), false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminNotificationResponse> notifications(Long adminUserId, Integer limit) {
        return adminNotificationRepository.findAllByAdminUserIdIsNullOrAdminUserIdOrderByCreatedAtDesc(
                        adminUserId, page(limit))
                .stream()
                .map(AdminNotificationResponse::from)
                .toList();
    }

    @Transactional
    public AdminNotificationResponse markNotificationRead(Long adminUserId, Long id) {
        AdminNotification notification = adminNotificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (notification.getAdminUserId() != null && !notification.getAdminUserId().equals(adminUserId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found");
        }
        notification.markRead();
        return AdminNotificationResponse.from(notification);
    }

    @Transactional
    public List<AdminNotificationResponse> bulkReadNotifications(Long adminUserId, List<Long> ids) {
        return ids.stream()
                .map(id -> markNotificationRead(adminUserId, id))
                .toList();
    }

    @Transactional(readOnly = true)
    public SystemSettingResponse notificationSettings() {
        return systemSettingRepository.findById("notifications.settings")
                .map(SystemSettingResponse::from)
                .orElseGet(() -> new SystemSettingResponse("notifications.settings", "{}", false, null, null));
    }

    @Transactional
    public SystemSettingResponse updateNotificationSettings(Long adminUserId, String value) {
        return upsertSetting(adminUserId, "notifications.settings", value, false);
    }

    @Transactional
    public StoredFileResponse uploadFile(Long adminUserId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }
        try {
            StoredFile storedFile = storedFileRepository.save(new StoredFile(
                    file.getOriginalFilename() == null ? "upload.bin" : file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    adminUserId,
                    file.getBytes()));
            auditService.recordAdmin(adminUserId, "FILE_UPLOADED", "FILE", storedFile.getId(),
                    "fileName=" + storedFile.getFileName());
            return StoredFileResponse.from(storedFile);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot read uploaded file");
        }
    }

    @Transactional(readOnly = true)
    public StoredFile getFile(Long fileId) {
        return storedFileRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
    }

    @Transactional
    public void deleteFile(Long adminUserId, Long fileId) {
        StoredFile file = getFile(fileId);
        storedFileRepository.delete(file);
        auditService.recordAdmin(adminUserId, "FILE_DELETED", "FILE", fileId, "fileName=" + file.getFileName());
    }

    @Transactional(readOnly = true)
    public List<JobRecordResponse> jobs(Integer limit) {
        return jobRecordRepository.findAllByOrderByCreatedAtDesc(page(limit))
                .stream()
                .map(JobRecordResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public JobRecordResponse job(Long jobId) {
        return JobRecordResponse.from(requireJob(jobId));
    }

    @Transactional
    public JobRecordResponse retryJob(Long adminUserId, Long jobId) {
        JobRecord job = requireJob(jobId);
        job.retry();
        auditService.recordAdmin(adminUserId, "JOB_RETRY_REQUESTED", "JOB", job.getId(), "name=" + job.getName());
        return JobRecordResponse.from(job);
    }

    @Transactional
    public JobRecordResponse cancelJob(Long adminUserId, Long jobId) {
        JobRecord job = requireJob(jobId);
        job.cancel();
        auditService.recordAdmin(adminUserId, "JOB_CANCELLED", "JOB", job.getId(), "name=" + job.getName());
        return JobRecordResponse.from(job);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dashboardSummary() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("users", userAccountRepository.count());
        response.put("activeUsers", userAccountRepository.countByStatus(UserStatus.ACTIVE));
        response.put("lockedUsers", userAccountRepository.countByStatus(UserStatus.LOCKED));
        response.put("orders", orderRepository.count());
        response.put("processingOrders", orderRepository.countByStatus(OrderStatus.PROCESSING));
        response.put("completedOrders", orderRepository.countByStatus(OrderStatus.COMPLETED));
        response.put("bankTransactions", bankTransactionRepository.count());
        response.put("tickets", ticketRepository.count());
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> revenueChart(Integer days) {
        int normalizedDays = days == null ? 14 : Math.max(1, Math.min(days, 90));
        List<Map<String, Object>> rows = new ArrayList<>();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (int i = normalizedDays - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Instant from = date.atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant to = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", date.toString());
            row.put("depositVolume", walletTransactionRepository.sumAmountByTypeAndDirectionBetween(
                    WalletTransactionType.DEPOSIT, WalletTransactionDirection.CREDIT, from, to));
            row.put("grossRevenue", orderRepository.sumAmountByStatusBetween(OrderStatus.COMPLETED, from, to));
            row.put("refunds", orderRepository.sumAmountByStatusBetween(OrderStatus.REFUNDED, from, to));
            rows.add(row);
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> userActivity() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("newUsersToday", userAccountRepository.countByCreatedAtGreaterThanEqual(
                LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC)));
        response.put("activeUsers", userAccountRepository.countByStatus(UserStatus.ACTIVE));
        response.put("lockedUsers", userAccountRepository.countByStatus(UserStatus.LOCKED));
        response.put("pendingVerifyUsers", userAccountRepository.countByStatus(UserStatus.PENDING_VERIFY));
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> servicePerformance(Integer limit) {
        return orderRepository.servicePerformance(page(limit))
                .stream()
                .map(row -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("serviceId", row[0]);
                    item.put("serviceName", row[1]);
                    item.put("orderCount", row[2]);
                    item.put("revenue", row[3]);
                    return item;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public String exportRevenue() {
        var report = adminFinanceService.getRevenueReport();
        return csv(List.of("depositVolume,grossRevenue,totalRefunds,netRevenue,walletLiability,totalCost,profit",
                csvRow(report.depositVolume(), report.grossRevenue(), report.totalRefunds(), report.netRevenue(),
                        report.walletLiability(), report.totalCost(), report.profit())));
    }

    @Transactional(readOnly = true)
    public String exportUsers() {
        List<String> rows = new ArrayList<>();
        rows.add("id,email,name,phone,role,status,balance,createdAt");
        userAccountRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 5000))
                .forEach(user -> rows.add(csvRow(user.getId(), user.getEmail(), user.getName(), user.getPhone(),
                        user.getRole(), user.getStatus(), user.getBalance(), user.getCreatedAt())));
        return csv(rows);
    }

    @Transactional(readOnly = true)
    public String exportOrders() {
        List<String> rows = new ArrayList<>();
        rows.add("id,orderCode,userId,serviceId,amount,status,createdAt");
        orderRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 5000))
                .forEach(order -> rows.add(csvRow(order.getId(), order.getOrderCode(), order.getUser().getId(),
                        order.getService().getId(), order.getAmount(), order.getStatus(), order.getCreatedAt())));
        return csv(rows);
    }

    @Transactional(readOnly = true)
    public String exportBank() {
        List<String> rows = new ArrayList<>();
        rows.add("id,sepayId,referenceCode,transferType,transferAmount,status,receivedAt");
        bankTransactionRepository.findAllByOrderByReceivedAtDesc(PageRequest.of(0, 5000))
                .forEach(tx -> rows.add(csvRow(tx.getId(), tx.getSepayId(), tx.getReferenceCode(),
                        tx.getTransferType(), tx.getTransferAmount(), tx.getStatus(), tx.getReceivedAt())));
        return csv(rows);
    }

    @Transactional(readOnly = true)
    public String exportTickets() {
        List<String> rows = new ArrayList<>();
        rows.add("id,ticketCode,userId,category,priority,status,createdAt,closedAt");
        ticketRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 5000))
                .forEach(ticket -> rows.add(csvRow(ticket.getId(), ticket.getTicketCode(), ticket.getUser().getId(),
                        ticket.getCategory(), ticket.getPriority(), ticket.getStatus(), ticket.getCreatedAt(),
                        ticket.getClosedAt())));
        return csv(rows);
    }

    @Transactional(readOnly = true)
    public String exportAudit() {
        List<String> rows = new ArrayList<>();
        rows.add("id,actorUserId,actorRole,action,targetType,targetId,metadata,createdAt");
        auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 5000))
                .forEach(log -> rows.add(csvRow(log.getId(), log.getActorUserId(), log.getActorRole(),
                        log.getAction(), log.getTargetType(), log.getTargetId(), log.getMetadata(),
                        log.getCreatedAt())));
        return csv(rows);
    }

    @Transactional(readOnly = true)
    public List<com.example.KendyDigital.dto.AuditLogResponse> searchAudit(String query, String action,
            Long actorUserId, String targetType, Long targetId, Integer limit) {
        return auditLogRepository.searchAdmin(normalizeQuery(query), blankToNull(action), actorUserId,
                        blankToNull(targetType), targetId, page(limit))
                .stream()
                .map(com.example.KendyDigital.dto.AuditLogResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public com.example.KendyDigital.dto.AuditLogResponse auditDetail(Long id) {
        return auditLogRepository.findById(id)
                .map(com.example.KendyDigital.dto.AuditLogResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit log not found"));
    }

    @Transactional(readOnly = true)
    public List<com.example.KendyDigital.dto.AuditLogResponse> adminActions(Long adminId, Integer limit) {
        return auditLogRepository.findAllByActorUserIdOrderByCreatedAtDesc(adminId, page(limit))
                .stream()
                .map(com.example.KendyDigital.dto.AuditLogResponse::from)
                .toList();
    }

    private SystemSettingResponse upsertSetting(Long adminUserId, String key, String value, boolean publicSetting) {
        SystemSetting setting = systemSettingRepository.findById(key).orElse(null);
        String oldValue = setting == null ? null : setting.getValue();
        if (setting == null) {
            setting = systemSettingRepository.save(new SystemSetting(key, value, publicSetting, adminUserId));
        } else {
            setting.update(value, publicSetting, adminUserId);
        }
        systemSettingHistoryRepository.save(new SystemSettingHistory(key, oldValue, value, adminUserId));
        auditService.recordAdmin(adminUserId, "SETTING_UPDATED", "SYSTEM_SETTING", null, "key=" + key);
        return SystemSettingResponse.from(setting);
    }

    private UserAccount requireAdmin(Long adminId) {
        UserAccount admin = userAccountRepository.findById(adminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));
        if (admin.getRole() == UserRole.USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not an admin");
        }
        return admin;
    }

    private UserAccount requireAdminForUpdate(Long adminId) {
        UserAccount admin = userAccountRepository.findByIdForUpdate(adminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));
        if (admin.getRole() == UserRole.USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not an admin");
        }
        return admin;
    }

    private JobRecord requireJob(Long jobId) {
        return jobRecordRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
    }

    private List<String> parsePermissions(String permissions) {
        if (permissions == null || permissions.isBlank()) {
            return List.of();
        }
        return Arrays.stream(permissions.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private PageRequest page(Integer limit) {
        int normalizedLimit = limit == null ? 100 : Math.max(1, Math.min(limit, 500));
        return PageRequest.of(0, normalizedLimit);
    }

    private PageRequest paged(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 500)));
    }

    private String normalizeQuery(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String csv(List<String> rows) {
        return String.join("\n", rows) + "\n";
    }

    private String csvRow(Object... values) {
        return Arrays.stream(values)
                .map(value -> value == null ? "" : value.toString())
                .map(value -> "\"" + value.replace("\"", "\"\"") + "\"")
                .collect(Collectors.joining(","));
    }

    public byte[] previewBytes(StoredFile file) {
        if (file.getContentType() != null && file.getContentType().startsWith("text/")) {
            String content = new String(file.getContent(), StandardCharsets.UTF_8);
            return content.substring(0, Math.min(content.length(), 4000)).getBytes(StandardCharsets.UTF_8);
        }
        return file.getContent();
    }
}
