package com.example.KendyDigital.service.system_infrastructure.impl;

import com.example.KendyDigital.dto.audit.response.AuditLogResponse;
import com.example.KendyDigital.dto.finance.request.ReprocessBankTransactionRequest;
import com.example.KendyDigital.dto.monitoring.response.JobRecordResponse;
import com.example.KendyDigital.dto.order.response.OrderResponse;
import com.example.KendyDigital.dto.setting.request.WebhookRetryRequest;
import com.example.KendyDigital.dto.ticket.response.TicketResponse;
import com.example.KendyDigital.dto.wallet.response.WalletTransactionResponse;
import com.example.KendyDigital.service.audit.AdminAuditSearchService;
import com.example.KendyDigital.service.bank.AdminBankTxManagerService;
import com.example.KendyDigital.service.system_infrastructure.AdminMonitoringService;
import com.example.KendyDigital.service.system_infrastructure.JobRecordMonitoringService;
import com.example.KendyDigital.service.system_infrastructure.UserActivityMonitoringService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminMonitoringServiceImpl implements AdminMonitoringService {
    private final UserActivityMonitoringService userActivityMonitoringService;
    private final JobRecordMonitoringService jobRecordMonitoringService;
    private final AdminAuditSearchService adminAuditSearchService;
    private final AdminBankTxManagerService bankTxManagerService;

    public AdminMonitoringServiceImpl(
            UserActivityMonitoringService userActivityMonitoringService,
            JobRecordMonitoringService jobRecordMonitoringService,
            AdminAuditSearchService adminAuditSearchService,
            AdminBankTxManagerService bankTxManagerService) {
        this.userActivityMonitoringService = userActivityMonitoringService;
        this.jobRecordMonitoringService = jobRecordMonitoringService;
        this.adminAuditSearchService = adminAuditSearchService;
        this.bankTxManagerService = bankTxManagerService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> listUserOrders(Long userId, int page, int size) {
        return userActivityMonitoringService.listUserOrders(userId, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> listUserOrders(Long userId, Integer limit) {
        return userActivityMonitoringService.listUserOrders(userId, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> listUserWalletTransactions(Long userId, int page, int size) {
        return userActivityMonitoringService.listUserWalletTransactions(userId, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> listUserWalletTransactions(Long userId, Integer limit) {
        return userActivityMonitoringService.listUserWalletTransactions(userId, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> listUserTickets(Long userId, int page, int size) {
        return userActivityMonitoringService.listUserTickets(userId, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> listUserTickets(Long userId, Integer limit) {
        return userActivityMonitoringService.listUserTickets(userId, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobRecordResponse> jobs(Integer limit) {
        return jobRecordMonitoringService.jobs(limit);
    }

    @Override
    @Transactional(readOnly = true)
    public JobRecordResponse job(Long jobId) {
        return jobRecordMonitoringService.job(jobId);
    }

    @Override
    @Transactional
    public JobRecordResponse retryJob(Long adminUserId, Long jobId) {
        return jobRecordMonitoringService.retryJob(adminUserId, jobId);
    }

    @Override
    @Transactional
    public JobRecordResponse cancelJob(Long adminUserId, Long jobId) {
        return jobRecordMonitoringService.cancelJob(adminUserId, jobId);
    }

    @Override
    @Transactional
    public Object retrySePayWebhook(Long adminUserId, WebhookRetryRequest request) {
        return bankTxManagerService.reprocessBankTransaction(adminUserId, request.bankTransactionId(),
                new ReprocessBankTransactionRequest(request.depositCode(), request.reason()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> sepayLogs(Integer limit) {
        return adminAuditSearchService.sepayLogs(limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> searchAudit(String query, String action, Long actorUserId, String targetType,
            Long targetId, Integer limit) {
        return adminAuditSearchService.searchAudit(query, action, actorUserId, targetType, targetId, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogResponse auditDetail(Long id) {
        return adminAuditSearchService.auditDetail(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> adminActions(Long adminId, Integer limit) {
        return adminAuditSearchService.adminActions(adminId, limit);
    }
}
