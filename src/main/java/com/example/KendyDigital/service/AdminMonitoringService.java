package com.example.KendyDigital.service;
import com.example.KendyDigital.dto.audit.response.AuditLogResponse;


import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.KendyDigital.dto.monitoring.response.JobRecordResponse;
import com.example.KendyDigital.dto.order.response.OrderResponse;
import com.example.KendyDigital.dto.finance.request.ReprocessBankTransactionRequest;
import com.example.KendyDigital.dto.ticket.response.TicketResponse;
import com.example.KendyDigital.dto.wallet.response.WalletTransactionResponse;
import com.example.KendyDigital.dto.setting.request.WebhookRetryRequest;
import com.example.KendyDigital.model.JobRecord;
import com.example.KendyDigital.repository.AuditLogRepository;
import com.example.KendyDigital.repository.JobRecordRepository;
import com.example.KendyDigital.repository.OrderRepository;
import com.example.KendyDigital.repository.TicketRepository;
import com.example.KendyDigital.repository.WalletTransactionRepository;

@Service
public class AdminMonitoringService {
    private final OrderRepository orderRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final TicketRepository ticketRepository;
    private final AuditLogRepository auditLogRepository;
    private final JobRecordRepository jobRecordRepository;
    private final AuditService auditService;
    private final AdminBankTxManagerService bankTxManagerService;

    public AdminMonitoringService(OrderRepository orderRepository,
            WalletTransactionRepository walletTransactionRepository,
            TicketRepository ticketRepository,
            AuditLogRepository auditLogRepository,
            JobRecordRepository jobRecordRepository,
            AuditService auditService,
            AdminBankTxManagerService bankTxManagerService) {
        this.orderRepository = orderRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.ticketRepository = ticketRepository;
        this.auditLogRepository = auditLogRepository;
        this.jobRecordRepository = jobRecordRepository;
        this.auditService = auditService;
        this.bankTxManagerService = bankTxManagerService;
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

    @Transactional
    public Object retrySePayWebhook(Long adminUserId, WebhookRetryRequest request) {
        return bankTxManagerService.reprocessBankTransaction(adminUserId, request.bankTransactionId(),
                new ReprocessBankTransactionRequest(request.depositCode(), request.reason()));
    }

    @Transactional(readOnly = true)
    public List<com.example.KendyDigital.dto.audit.response.AuditLogResponse> sepayLogs(Integer limit) {
        return auditLogRepository.searchAdmin(likePattern("SEPAY"), null, null, null, null, page(limit))
                .stream()
                .map(com.example.KendyDigital.dto.audit.response.AuditLogResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<com.example.KendyDigital.dto.audit.response.AuditLogResponse> searchAudit(String query, String action,
            Long actorUserId, String targetType, Long targetId, Integer limit) {
        return auditLogRepository.searchAdmin(likePattern(normalizeQuery(query)), blankToNull(action), actorUserId,
                        blankToNull(targetType), targetId, page(limit))
                .stream()
                .map(com.example.KendyDigital.dto.audit.response.AuditLogResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public com.example.KendyDigital.dto.audit.response.AuditLogResponse auditDetail(Long id) {
        return auditLogRepository.findById(id)
                .map(com.example.KendyDigital.dto.audit.response.AuditLogResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit log not found"));
    }

    @Transactional(readOnly = true)
    public List<com.example.KendyDigital.dto.audit.response.AuditLogResponse> adminActions(Long adminId, Integer limit) {
        return auditLogRepository.findAllByActorUserIdOrderByCreatedAtDesc(adminId, page(limit))
                .stream()
                .map(com.example.KendyDigital.dto.audit.response.AuditLogResponse::from)
                .toList();
    }

    private JobRecord requireJob(Long jobId) {
        return jobRecordRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
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

    private String likePattern(String value) {
        return value == null ? null : "%" + value.toLowerCase(java.util.Locale.ROOT) + "%";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
