package com.example.KendyDigital.service.analytics_finance.impl;

import com.example.KendyDigital.service.analytics_finance.*;
import com.example.KendyDigital.service.analytics_finance.export.ReportExportService;

import com.example.KendyDigital.model.order.OrderStatus;
import com.example.KendyDigital.model.user.UserStatus;
import com.example.KendyDigital.repository.AuditLogRepository;
import com.example.KendyDigital.repository.BankTransactionRepository;
import com.example.KendyDigital.repository.OrderRepository;
import com.example.KendyDigital.repository.TicketRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.repository.WalletTransactionRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminExportAnalyticsServiceImpl implements AdminExportAnalyticsService {
    private final UserAccountRepository userAccountRepository;
    private final OrderRepository orderRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final TicketRepository ticketRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final AdminFinanceReportService financeReportService;
    private final ReportExportService reportExportService;

    public AdminExportAnalyticsServiceImpl(UserAccountRepository userAccountRepository,
            OrderRepository orderRepository,
            WalletTransactionRepository walletTransactionRepository,
            TicketRepository ticketRepository,
            BankTransactionRepository bankTransactionRepository,
            AuditLogRepository auditLogRepository,
            AdminFinanceReportService financeReportService,
            ReportExportService reportExportService) {
        this.userAccountRepository = userAccountRepository;
        this.orderRepository = orderRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.ticketRepository = ticketRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.auditLogRepository = auditLogRepository;
        this.financeReportService = financeReportService;
        this.reportExportService = reportExportService;
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
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate firstDay = today.minusDays(normalizedDays - 1L);
        Instant from = firstDay.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        Map<LocalDate, java.math.BigDecimal> depositsByDay = walletTransactionRepository.sumDepositsByDay(from, to)
                .stream()
                .collect(Collectors.toMap(
                        row -> toLocalDate(row[0]),
                        row -> toBigDecimal(row[1])));
        Map<LocalDate, Map<String, java.math.BigDecimal>> ordersByDay = new LinkedHashMap<>();
        for (Object[] row : orderRepository.sumRevenueByDay(from, to)) {
            ordersByDay.computeIfAbsent(toLocalDate(row[0]), ignored -> new LinkedHashMap<>())
                    .put(String.valueOf(row[1]), toBigDecimal(row[2]));
        }

        List<Map<String, Object>> rows = new ArrayList<>(normalizedDays);
        for (int i = normalizedDays - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Map<String, java.math.BigDecimal> orderTotals = ordersByDay.getOrDefault(date, Map.of());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", date.toString());
            row.put("depositVolume", depositsByDay.getOrDefault(date, java.math.BigDecimal.ZERO));
            row.put("grossRevenue", orderTotals.getOrDefault(OrderStatus.COMPLETED.name(), java.math.BigDecimal.ZERO));
            row.put("refunds", orderTotals.getOrDefault(OrderStatus.REFUNDED.name(), java.math.BigDecimal.ZERO));
            rows.add(row);
        }
        return rows;
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof java.sql.Date date) return date.toLocalDate();
        if (value instanceof LocalDate date) return date;
        return LocalDate.parse(String.valueOf(value));
    }

    private java.math.BigDecimal toBigDecimal(Object value) {
        if (value instanceof java.math.BigDecimal decimal) return decimal;
        return new java.math.BigDecimal(String.valueOf(value));
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
        var report = financeReportService.getRevenueReport();
        return reportExportService.exportRevenueCsv(report);
    }

    @Transactional(readOnly = true)
    public byte[] exportRevenueXlsx() {
        var report = financeReportService.getRevenueReport();
        return reportExportService.exportRevenueXlsx(report);
    }

    @Transactional(readOnly = true)
    public String exportUsers() {
        return reportExportService.exportUsersCsv(loadAll(userAccountRepository::findAllByOrderByCreatedAtDesc));
    }

    @Transactional(readOnly = true)
    public byte[] exportUsersXlsx() {
        return reportExportService.exportUsersXlsx(loadAll(userAccountRepository::findAllByOrderByCreatedAtDesc));
    }

    @Transactional(readOnly = true)
    public String exportOrders() {
        return reportExportService.exportOrdersCsv(loadAll(orderRepository::findAllByOrderByCreatedAtDesc));
    }

    @Transactional(readOnly = true)
    public byte[] exportOrdersXlsx() {
        return reportExportService.exportOrdersXlsx(loadAll(orderRepository::findAllByOrderByCreatedAtDesc));
    }

    @Transactional(readOnly = true)
    public String exportBank() {
        return reportExportService.exportBankTransactionsCsv(loadAll(bankTransactionRepository::findAllByOrderByReceivedAtDesc));
    }

    @Transactional(readOnly = true)
    public byte[] exportBankXlsx() {
        return reportExportService.exportBankTransactionsXlsx(loadAll(bankTransactionRepository::findAllByOrderByReceivedAtDesc));
    }

    @Transactional(readOnly = true)
    public String exportTickets() {
        return reportExportService.exportTicketsCsv(loadAll(ticketRepository::findAllByOrderByCreatedAtDesc));
    }

    @Transactional(readOnly = true)
    public byte[] exportTicketsXlsx() {
        return reportExportService.exportTicketsXlsx(loadAll(ticketRepository::findAllByOrderByCreatedAtDesc));
    }

    @Transactional(readOnly = true)
    public String exportAudit() {
        return reportExportService.exportAuditLogsCsv(loadAll(auditLogRepository::findAllByOrderByCreatedAtDesc));
    }

    @Transactional(readOnly = true)
    public byte[] exportAuditXlsx() {
        return reportExportService.exportAuditLogsXlsx(loadAll(auditLogRepository::findAllByOrderByCreatedAtDesc));
    }

    private PageRequest page(Integer limit) {
        int normalizedLimit = limit == null ? 100 : Math.max(1, Math.min(limit, 500));
        return PageRequest.of(0, normalizedLimit);
    }

    private <T> List<T> loadAll(Function<Pageable, List<T>> loader) {
        final int batchSize = 500;
        List<T> results = new ArrayList<>();
        for (int pageNumber = 0; ; pageNumber++) {
            List<T> batch = loader.apply(PageRequest.of(pageNumber, batchSize));
            results.addAll(batch);
            if (batch.size() < batchSize) {
                return results;
            }
        }
    }
}
