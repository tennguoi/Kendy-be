package com.example.KendyDigital.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.KendyDigital.dto.AdminNotificationResponse;
import com.example.KendyDigital.dto.AdminPermissionsRequest;
import com.example.KendyDigital.dto.AdminRolesRequest;
import com.example.KendyDigital.dto.AdminUserResponse;
import com.example.KendyDigital.dto.AuthSessionResponse;
import com.example.KendyDigital.dto.BulkReadNotificationsRequest;
import com.example.KendyDigital.dto.IdsRequest;
import com.example.KendyDigital.dto.JobRecordResponse;
import com.example.KendyDigital.dto.OrderResponse;
import com.example.KendyDigital.dto.StoredFileResponse;
import com.example.KendyDigital.dto.SystemSettingHistoryResponse;
import com.example.KendyDigital.dto.SystemSettingResponse;
import com.example.KendyDigital.dto.SystemSettingUpdateRequest;
import com.example.KendyDigital.dto.SystemSettingsBulkUpdateRequest;
import com.example.KendyDigital.dto.TicketResponse;
import com.example.KendyDigital.dto.WebhookConfigRequest;
import com.example.KendyDigital.dto.WebhookRetryRequest;
import com.example.KendyDigital.dto.WalletTransactionResponse;
import com.example.KendyDigital.model.StoredFile;
import com.example.KendyDigital.model.UserStatus;
import com.example.KendyDigital.security.CurrentUser;
import com.example.KendyDigital.service.AdminOperationsService;

import jakarta.validation.Valid;

@RestController
public class AdminOperationsController {
    private final AdminOperationsService adminOperationsService;

    public AdminOperationsController(AdminOperationsService adminOperationsService) {
        this.adminOperationsService = adminOperationsService;
    }

    @GetMapping("/api/admin/users/{id}/sessions")
    public List<AuthSessionResponse> userSessions(@PathVariable Long id,
            @RequestParam(required = false) Integer limit) {
        return adminOperationsService.listSessions(id, limit);
    }

    @DeleteMapping("/api/admin/users/{id}/sessions/{sessionId}")
    public void revokeUserSession(Authentication authentication, @PathVariable Long id,
            @PathVariable Long sessionId) {
        adminOperationsService.revokeSession(CurrentUser.require(authentication).userId(), id, sessionId);
    }

    @GetMapping("/api/admin/users/{id}/orders")
    public List<OrderResponse> userOrders(@PathVariable Long id, @RequestParam(required = false) Integer limit) {
        return adminOperationsService.listUserOrders(id, limit);
    }

    @GetMapping("/api/admin/users/{id}/wallet-transactions")
    public List<WalletTransactionResponse> userWalletTransactions(@PathVariable Long id,
            @RequestParam(required = false) Integer limit) {
        return adminOperationsService.listUserWalletTransactions(id, limit);
    }

    @GetMapping("/api/admin/users/{id}/tickets")
    public List<TicketResponse> userTickets(@PathVariable Long id, @RequestParam(required = false) Integer limit) {
        return adminOperationsService.listUserTickets(id, limit);
    }

    @PostMapping("/api/admin/users/bulk-lock")
    public List<AdminUserResponse> bulkLockUsers(Authentication authentication,
            @Valid @RequestBody IdsRequest request) {
        return adminOperationsService.bulkUserStatus(CurrentUser.require(authentication).userId(), request.ids(),
                UserStatus.LOCKED, request.reason());
    }

    @PostMapping("/api/admin/users/bulk-unlock")
    public List<AdminUserResponse> bulkUnlockUsers(Authentication authentication,
            @Valid @RequestBody IdsRequest request) {
        return adminOperationsService.bulkUserStatus(CurrentUser.require(authentication).userId(), request.ids(),
                UserStatus.ACTIVE, request.reason());
    }

    @GetMapping("/api/admin/dashboard/summary")
    public Map<String, Object> dashboardSummary() {
        return adminOperationsService.dashboardSummary();
    }

    @GetMapping("/api/admin/dashboard/revenue-chart")
    public List<Map<String, Object>> revenueChart(@RequestParam(required = false) Integer days) {
        return adminOperationsService.revenueChart(days);
    }

    @GetMapping("/api/admin/dashboard/user-activity")
    public Map<String, Object> userActivity() {
        return adminOperationsService.userActivity();
    }

    @GetMapping("/api/admin/dashboard/service-performance")
    public List<Map<String, Object>> servicePerformance(@RequestParam(required = false) Integer limit) {
        return adminOperationsService.servicePerformance(limit);
    }

    @GetMapping("/api/admin/reports/revenue/export")
    public ResponseEntity<String> exportRevenue() {
        return csv("revenue.csv", adminOperationsService.exportRevenue());
    }

    @GetMapping("/api/admin/reports/users/export")
    public ResponseEntity<String> exportUsers() {
        return csv("users.csv", adminOperationsService.exportUsers());
    }

    @GetMapping("/api/admin/reports/orders/export")
    public ResponseEntity<String> exportOrders() {
        return csv("orders.csv", adminOperationsService.exportOrders());
    }

    @GetMapping("/api/admin/reports/bank/export")
    public ResponseEntity<String> exportBank() {
        return csv("bank-transactions.csv", adminOperationsService.exportBank());
    }

    @GetMapping("/api/admin/reports/tickets/export")
    public ResponseEntity<String> exportTickets() {
        return csv("tickets.csv", adminOperationsService.exportTickets());
    }

    @GetMapping("/api/admin/audit-logs/search")
    public List<com.example.KendyDigital.dto.AuditLogResponse> searchAudit(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long actorUserId,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) Integer limit) {
        return adminOperationsService.searchAudit(query, action, actorUserId, targetType, targetId, limit);
    }

    @GetMapping("/api/admin/audit-logs/{id}/details")
    public com.example.KendyDigital.dto.AuditLogResponse auditDetail(@PathVariable Long id) {
        return adminOperationsService.auditDetail(id);
    }

    @GetMapping("/api/admin/audit-logs/export")
    public ResponseEntity<String> exportAudit() {
        return csv("audit-logs.csv", adminOperationsService.exportAudit());
    }

    @GetMapping("/api/admin/audit-logs/admin-actions")
    public List<com.example.KendyDigital.dto.AuditLogResponse> adminActions(@RequestParam Long adminId,
            @RequestParam(required = false) Integer limit) {
        return adminOperationsService.adminActions(adminId, limit);
    }

    @GetMapping("/api/admin/settings/search")
    public List<SystemSettingResponse> searchSettings(@RequestParam(required = false) String query,
            @RequestParam(required = false) Integer limit) {
        return adminOperationsService.searchSettings(query, limit);
    }

    @GetMapping("/api/admin/settings/{key}/history")
    public List<SystemSettingHistoryResponse> settingHistory(@PathVariable String key,
            @RequestParam(required = false) Integer limit) {
        return adminOperationsService.settingHistory(key, limit);
    }

    @PutMapping("/api/admin/settings/bulk-update")
    public List<SystemSettingResponse> bulkUpdateSettings(Authentication authentication,
            @Valid @RequestBody SystemSettingsBulkUpdateRequest request) {
        return adminOperationsService.bulkUpdateSettings(CurrentUser.require(authentication).userId(), request);
    }

    @PostMapping("/api/admin/settings/backup")
    public List<SystemSettingResponse> backupSettings() {
        return adminOperationsService.backupSettings();
    }

    @PostMapping("/api/admin/settings/restore")
    public List<SystemSettingResponse> restoreSettings(Authentication authentication,
            @Valid @RequestBody SystemSettingsBulkUpdateRequest request) {
        return adminOperationsService.restoreSettings(CurrentUser.require(authentication).userId(), request);
    }

    @GetMapping("/api/admin/webhooks/sepay/status")
    public Map<String, Object> sepayStatus() {
        return adminOperationsService.sepayStatus();
    }

    @PostMapping("/api/admin/webhooks/sepay/retry")
    public Object retrySePayWebhook(Authentication authentication, @Valid @RequestBody WebhookRetryRequest request) {
        return adminOperationsService.retrySePayWebhook(CurrentUser.require(authentication).userId(), request);
    }

    @GetMapping("/api/admin/webhooks/sepay/logs")
    public List<com.example.KendyDigital.dto.AuditLogResponse> sepayLogs(
            @RequestParam(required = false) Integer limit) {
        return adminOperationsService.sepayLogs(limit);
    }

    @GetMapping("/api/admin/webhooks/sepay/config")
    public List<SystemSettingResponse> getSePayConfig() {
        return adminOperationsService.getSePayConfig();
    }

    @PutMapping("/api/admin/webhooks/sepay/config")
    public List<SystemSettingResponse> updateSePayConfig(Authentication authentication,
            @RequestBody WebhookConfigRequest request) {
        return adminOperationsService.updateSePayConfig(CurrentUser.require(authentication).userId(), request);
    }

    @GetMapping("/api/admin/admins")
    public List<AdminUserResponse> admins(@RequestParam(required = false) Integer limit) {
        return adminOperationsService.listAdmins(limit);
    }

    @PostMapping("/api/admin/admins/{id}/2fa/enable")
    public AdminUserResponse enableTwoFactor(Authentication authentication, @PathVariable Long id) {
        return adminOperationsService.setTwoFactor(CurrentUser.require(authentication).userId(), id, true,
                "ADMIN_2FA_ENABLED");
    }

    @PostMapping("/api/admin/admins/{id}/2fa/disable")
    public AdminUserResponse disableTwoFactor(Authentication authentication, @PathVariable Long id) {
        return adminOperationsService.setTwoFactor(CurrentUser.require(authentication).userId(), id, false,
                "ADMIN_2FA_DISABLED");
    }

    @PostMapping("/api/admin/admins/{id}/2fa/reset")
    public AdminUserResponse resetTwoFactor(Authentication authentication, @PathVariable Long id) {
        return adminOperationsService.setTwoFactor(CurrentUser.require(authentication).userId(), id, false,
                "ADMIN_2FA_RESET");
    }

    @GetMapping("/api/admin/admins/{id}/permissions")
    public Map<String, Object> getPermissions(@PathVariable Long id) {
        return adminOperationsService.getPermissions(id);
    }

    @PutMapping("/api/admin/admins/{id}/permissions")
    public Map<String, Object> updatePermissions(Authentication authentication, @PathVariable Long id,
            @RequestBody AdminPermissionsRequest request) {
        return adminOperationsService.updatePermissions(CurrentUser.require(authentication).userId(), id, request);
    }

    @GetMapping("/api/admin/admins/{id}/roles")
    public Map<String, Object> getRoles(@PathVariable Long id) {
        return adminOperationsService.getRoles(id);
    }

    @PutMapping("/api/admin/admins/{id}/roles")
    public Map<String, Object> updateRoles(Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody AdminRolesRequest request) {
        return adminOperationsService.updateRoles(CurrentUser.require(authentication).userId(), id, request);
    }

    @GetMapping("/api/admin/admins/{id}/sessions")
    public List<AuthSessionResponse> adminSessions(@PathVariable Long id,
            @RequestParam(required = false) Integer limit) {
        return adminOperationsService.listSessions(id, limit);
    }

    @DeleteMapping("/api/admin/admins/{id}/sessions/{sessionId}")
    public void revokeAdminSession(Authentication authentication, @PathVariable Long id,
            @PathVariable Long sessionId) {
        adminOperationsService.revokeSession(CurrentUser.require(authentication).userId(), id, sessionId);
    }

    @PostMapping("/api/admin/admins/bulk-lock")
    public List<AdminUserResponse> bulkLockAdmins(Authentication authentication,
            @Valid @RequestBody IdsRequest request) {
        return adminOperationsService.bulkUserStatus(CurrentUser.require(authentication).userId(), request.ids(),
                UserStatus.LOCKED, request.reason());
    }

    @PostMapping("/api/admin/admins/bulk-unlock")
    public List<AdminUserResponse> bulkUnlockAdmins(Authentication authentication,
            @Valid @RequestBody IdsRequest request) {
        return adminOperationsService.bulkUserStatus(CurrentUser.require(authentication).userId(), request.ids(),
                UserStatus.ACTIVE, request.reason());
    }

    @GetMapping("/api/admin/notifications")
    public List<AdminNotificationResponse> notifications(Authentication authentication,
            @RequestParam(required = false) Integer limit) {
        return adminOperationsService.notifications(CurrentUser.require(authentication).userId(), limit);
    }

    @PostMapping("/api/admin/notifications/{id}/read")
    public AdminNotificationResponse markNotificationRead(Authentication authentication, @PathVariable Long id) {
        return adminOperationsService.markNotificationRead(CurrentUser.require(authentication).userId(), id);
    }

    @PostMapping("/api/admin/notifications/bulk-read")
    public List<AdminNotificationResponse> bulkReadNotifications(Authentication authentication,
            @Valid @RequestBody BulkReadNotificationsRequest request) {
        return adminOperationsService.bulkReadNotifications(CurrentUser.require(authentication).userId(), request.ids());
    }

    @GetMapping("/api/admin/notifications/settings")
    public SystemSettingResponse notificationSettings() {
        return adminOperationsService.notificationSettings();
    }

    @PutMapping("/api/admin/notifications/settings")
    public SystemSettingResponse updateNotificationSettings(Authentication authentication,
            @Valid @RequestBody SystemSettingUpdateRequest request) {
        return adminOperationsService.updateNotificationSettings(CurrentUser.require(authentication).userId(),
                request.value());
    }

    @PostMapping(value = "/api/admin/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StoredFileResponse uploadFile(Authentication authentication, @RequestPart("file") MultipartFile file) {
        return adminOperationsService.uploadFile(CurrentUser.require(authentication).userId(), file);
    }

    @GetMapping("/api/admin/files/{fileId}/download")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long fileId) {
        StoredFile file = adminOperationsService.getFile(fileId);
        return fileResponse(file, file.getContent());
    }

    @DeleteMapping("/api/admin/files/{fileId}/delete")
    public void deleteFile(Authentication authentication, @PathVariable Long fileId) {
        adminOperationsService.deleteFile(CurrentUser.require(authentication).userId(), fileId);
    }

    @GetMapping("/api/admin/files/{fileId}/preview")
    public ResponseEntity<byte[]> previewFile(@PathVariable Long fileId) {
        StoredFile file = adminOperationsService.getFile(fileId);
        return fileResponse(file, adminOperationsService.previewBytes(file));
    }

    @GetMapping("/api/admin/jobs")
    public List<JobRecordResponse> jobs(@RequestParam(required = false) Integer limit) {
        return adminOperationsService.jobs(limit);
    }

    @GetMapping("/api/admin/jobs/{jobId}/status")
    public JobRecordResponse job(@PathVariable Long jobId) {
        return adminOperationsService.job(jobId);
    }

    @PostMapping("/api/admin/jobs/{jobId}/retry")
    public JobRecordResponse retryJob(Authentication authentication, @PathVariable Long jobId) {
        return adminOperationsService.retryJob(CurrentUser.require(authentication).userId(), jobId);
    }

    @PostMapping("/api/admin/jobs/{jobId}/cancel")
    public JobRecordResponse cancelJob(Authentication authentication, @PathVariable Long jobId) {
        return adminOperationsService.cancelJob(CurrentUser.require(authentication).userId(), jobId);
    }

    @GetMapping("/api/admin/jobs/logs")
    public List<JobRecordResponse> jobLogs(@RequestParam(required = false) Integer limit) {
        return adminOperationsService.jobs(limit);
    }

    private ResponseEntity<String> csv(String fileName, String body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(body);
    }

    private ResponseEntity<byte[]> fileResponse(StoredFile file, byte[] content) {
        MediaType mediaType = file.getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(file.getContentType());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.getFileName()).build().toString())
                .contentType(mediaType)
                .body(content);
    }
}
