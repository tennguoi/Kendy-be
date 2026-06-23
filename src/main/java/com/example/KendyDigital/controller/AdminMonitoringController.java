package com.example.KendyDigital.controller;

import com.example.KendyDigital.dto.audit.response.AuditLogResponse;
import com.example.KendyDigital.dto.monitoring.response.JobRecordResponse;
import com.example.KendyDigital.dto.order.response.OrderResponse;
import com.example.KendyDigital.dto.setting.request.WebhookRetryRequest;
import com.example.KendyDigital.dto.ticket.response.TicketResponse;
import com.example.KendyDigital.dto.wallet.response.WalletTransactionResponse;
import com.example.KendyDigital.security.CurrentUser;
import com.example.KendyDigital.service.monitoring.AdminMonitoringService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminMonitoringController {
    private final AdminMonitoringService monitoringService;

    public AdminMonitoringController(AdminMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping("/api/admin/users/{id}/orders")
    public List<OrderResponse> userOrders(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return monitoringService.listUserOrders(id, page, size);
    }

    @GetMapping("/api/admin/users/{id}/wallet-transactions")
    public List<WalletTransactionResponse> userWalletTransactions(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return monitoringService.listUserWalletTransactions(id, page, size);
    }

    @GetMapping("/api/admin/users/{id}/tickets")
    public List<TicketResponse> userTickets(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return monitoringService.listUserTickets(id, page, size);
    }

    @GetMapping("/api/admin/audit-logs/search")
    public List<AuditLogResponse> searchAudit(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long actorUserId,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) Integer limit) {
        return monitoringService.searchAudit(query, action, actorUserId, targetType, targetId, limit);
    }

    @GetMapping("/api/admin/audit-logs/{id}/details")
    public AuditLogResponse auditDetail(@PathVariable Long id) {
        return monitoringService.auditDetail(id);
    }

    @GetMapping("/api/admin/audit-logs/admin-actions")
    public List<AuditLogResponse> adminActions(@RequestParam Long adminId,
            @RequestParam(required = false) Integer limit) {
        return monitoringService.adminActions(adminId, limit);
    }

    @PostMapping("/api/admin/webhooks/sepay/retry")
    public Object retrySePayWebhook(Authentication authentication, @Valid @RequestBody WebhookRetryRequest request) {
        return monitoringService.retrySePayWebhook(CurrentUser.require(authentication).userId(), request);
    }

    @GetMapping("/api/admin/webhooks/sepay/logs")
    public List<AuditLogResponse> sepayLogs(@RequestParam(required = false) Integer limit) {
        return monitoringService.sepayLogs(limit);
    }

    @GetMapping("/api/admin/jobs")
    public List<JobRecordResponse> jobs(@RequestParam(required = false) Integer limit) {
        return monitoringService.jobs(limit);
    }

    @GetMapping("/api/admin/jobs/{jobId}/status")
    public JobRecordResponse job(@PathVariable Long jobId) {
        return monitoringService.job(jobId);
    }

    @PostMapping("/api/admin/jobs/{jobId}/retry")
    public JobRecordResponse retryJob(Authentication authentication, @PathVariable Long jobId) {
        return monitoringService.retryJob(CurrentUser.require(authentication).userId(), jobId);
    }

    @PostMapping("/api/admin/jobs/{jobId}/cancel")
    public JobRecordResponse cancelJob(Authentication authentication, @PathVariable Long jobId) {
        return monitoringService.cancelJob(CurrentUser.require(authentication).userId(), jobId);
    }

    @GetMapping("/api/admin/jobs/logs")
    public List<JobRecordResponse> jobLogs(@RequestParam(required = false) Integer limit) {
        return monitoringService.jobs(limit);
    }
}
