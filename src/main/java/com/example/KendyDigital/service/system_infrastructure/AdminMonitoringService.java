package com.example.KendyDigital.service.system_infrastructure;

import com.example.KendyDigital.dto.audit.response.AuditLogResponse;
import com.example.KendyDigital.dto.monitoring.response.JobRecordResponse;
import com.example.KendyDigital.dto.order.response.OrderResponse;
import com.example.KendyDigital.dto.setting.request.WebhookRetryRequest;
import com.example.KendyDigital.dto.ticket.response.TicketResponse;
import com.example.KendyDigital.dto.wallet.response.WalletTransactionResponse;
import java.util.List;
import java.util.UUID;

public interface AdminMonitoringService {
    List<OrderResponse> listUserOrders(UUID userId, int page, int size);
    List<OrderResponse> listUserOrders(UUID userId, Integer limit);
    List<WalletTransactionResponse> listUserWalletTransactions(UUID userId, int page, int size);
    List<WalletTransactionResponse> listUserWalletTransactions(UUID userId, Integer limit);
    List<TicketResponse> listUserTickets(UUID userId, int page, int size);
    List<TicketResponse> listUserTickets(UUID userId, Integer limit);
    List<JobRecordResponse> jobs(Integer limit);
    JobRecordResponse job(UUID jobId);
    JobRecordResponse retryJob(Long adminUserId, UUID jobId);
    JobRecordResponse cancelJob(Long adminUserId, UUID jobId);
    Object retrySePayWebhook(Long adminUserId, WebhookRetryRequest request);
    List<com.example.KendyDigital.dto.audit.response.AuditLogResponse> sepayLogs(Integer limit);
    List<com.example.KendyDigital.dto.audit.response.AuditLogResponse> searchAudit(String query, String action, UUID actorUserId, String targetType, Long targetId, Integer limit);
    com.example.KendyDigital.dto.audit.response.AuditLogResponse auditDetail(UUID id);
    List<com.example.KendyDigital.dto.audit.response.AuditLogResponse> adminActions(UUID adminId, Integer limit);
}
