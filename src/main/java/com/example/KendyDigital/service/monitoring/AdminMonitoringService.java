package com.example.KendyDigital.service.monitoring;

import com.example.KendyDigital.dto.audit.response.AuditLogResponse;
import com.example.KendyDigital.dto.monitoring.response.JobRecordResponse;
import com.example.KendyDigital.dto.order.response.OrderResponse;
import com.example.KendyDigital.dto.setting.request.WebhookRetryRequest;
import com.example.KendyDigital.dto.ticket.response.TicketResponse;
import com.example.KendyDigital.dto.wallet.response.WalletTransactionResponse;
import java.util.List;

public interface AdminMonitoringService {
    List<OrderResponse> listUserOrders(Long userId, int page, int size);
    List<OrderResponse> listUserOrders(Long userId, Integer limit);
    List<WalletTransactionResponse> listUserWalletTransactions(Long userId, int page, int size);
    List<WalletTransactionResponse> listUserWalletTransactions(Long userId, Integer limit);
    List<TicketResponse> listUserTickets(Long userId, int page, int size);
    List<TicketResponse> listUserTickets(Long userId, Integer limit);
    List<JobRecordResponse> jobs(Integer limit);
    JobRecordResponse job(Long jobId);
    JobRecordResponse retryJob(Long adminUserId, Long jobId);
    JobRecordResponse cancelJob(Long adminUserId, Long jobId);
    Object retrySePayWebhook(Long adminUserId, WebhookRetryRequest request);
    List<com.example.KendyDigital.dto.audit.response.AuditLogResponse> sepayLogs(Integer limit);
    List<com.example.KendyDigital.dto.audit.response.AuditLogResponse> searchAudit(String query, String action, Long actorUserId, String targetType, Long targetId, Integer limit);
    com.example.KendyDigital.dto.audit.response.AuditLogResponse auditDetail(Long id);
    List<com.example.KendyDigital.dto.audit.response.AuditLogResponse> adminActions(Long adminId, Integer limit);
}
