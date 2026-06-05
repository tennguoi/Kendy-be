package com.example.KendyDigital.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.KendyDigital.model.OrderStatus;
import com.example.KendyDigital.model.UserStatus;
import com.example.KendyDigital.model.WalletTransactionDirection;
import com.example.KendyDigital.model.WalletTransactionType;
import com.example.KendyDigital.repository.AuditLogRepository;
import com.example.KendyDigital.repository.BankTransactionRepository;
import com.example.KendyDigital.repository.OrderRepository;
import com.example.KendyDigital.repository.TicketRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.repository.WalletTransactionRepository;

@Service
public class AdminExportAnalyticsService {
    private final UserAccountRepository userAccountRepository;
    private final OrderRepository orderRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final TicketRepository ticketRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final AdminFinanceReportService financeReportService;

    public AdminExportAnalyticsService(UserAccountRepository userAccountRepository,
            OrderRepository orderRepository,
            WalletTransactionRepository walletTransactionRepository,
            TicketRepository ticketRepository,
            BankTransactionRepository bankTransactionRepository,
            AuditLogRepository auditLogRepository,
            AdminFinanceReportService financeReportService) {
        this.userAccountRepository = userAccountRepository;
        this.orderRepository = orderRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.ticketRepository = ticketRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.auditLogRepository = auditLogRepository;
        this.financeReportService = financeReportService;
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
        var report = financeReportService.getRevenueReport();
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

    private String csv(List<String> rows) {
        return String.join("\n", rows) + "\n";
    }

    private String csvRow(Object... values) {
        return Arrays.stream(values)
                .map(value -> value == null ? "" : value.toString())
                .map(value -> "\"" + value.replace("\"", "\"\"") + "\"")
                .collect(Collectors.joining(","));
    }

    private PageRequest page(Integer limit) {
        int normalizedLimit = limit == null ? 100 : Math.max(1, Math.min(limit, 500));
        return PageRequest.of(0, normalizedLimit);
    }
}
