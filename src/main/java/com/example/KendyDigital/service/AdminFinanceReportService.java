package com.example.KendyDigital.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.KendyDigital.dto.AdminDashboardResponse;
import com.example.KendyDigital.dto.RevenueReportResponse;
import com.example.KendyDigital.model.BankTransactionStatus;
import com.example.KendyDigital.model.DepositStatus;
import com.example.KendyDigital.model.OrderStatus;
import com.example.KendyDigital.model.TicketStatus;
import com.example.KendyDigital.model.UserStatus;
import com.example.KendyDigital.model.WalletTransactionDirection;
import com.example.KendyDigital.model.WalletTransactionType;
import com.example.KendyDigital.repository.BankTransactionRepository;
import com.example.KendyDigital.repository.DepositRequestRepository;
import com.example.KendyDigital.repository.OrderRepository;
import com.example.KendyDigital.repository.TicketRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.repository.WalletTransactionRepository;

@Service
public class AdminFinanceReportService {
    private final UserAccountRepository userAccountRepository;
    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final DepositRequestRepository depositRequestRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final BankTransactionRepository bankTransactionRepository;

    public AdminFinanceReportService(UserAccountRepository userAccountRepository,
            OrderRepository orderRepository,
            TicketRepository ticketRepository,
            DepositRequestRepository depositRequestRepository,
            WalletTransactionRepository walletTransactionRepository,
            BankTransactionRepository bankTransactionRepository) {
        this.userAccountRepository = userAccountRepository;
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
        this.depositRequestRepository = depositRequestRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.bankTransactionRepository = bankTransactionRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard() {
        Instant todayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        return new AdminDashboardResponse(
                userAccountRepository.count(),
                userAccountRepository.countByStatus(UserStatus.ACTIVE),
                userAccountRepository.countByStatus(UserStatus.LOCKED),
                userAccountRepository.countByStatus(UserStatus.PENDING_VERIFY),
                userAccountRepository.sumAllBalances(),
                orderRepository.count(),
                orderRepository.countByStatus(OrderStatus.PROCESSING),
                orderRepository.countByStatus(OrderStatus.COMPLETED),
                orderRepository.countByStatus(OrderStatus.CANCELLED),
                orderRepository.countByStatus(OrderStatus.FAILED),
                orderRepository.countByStatus(OrderStatus.REFUNDED),
                depositRequestRepository.countByStatus(DepositStatus.PENDING),
                depositRequestRepository.countByStatus(DepositStatus.COMPLETED),
                depositRequestRepository.countByStatus(DepositStatus.MANUAL_REVIEW),
                depositRequestRepository.sumAmountByStatus(DepositStatus.COMPLETED),
                ticketRepository.countByStatus(TicketStatus.PENDING_ADMIN),
                ticketRepository.countByStatus(TicketStatus.PENDING_USER),
                ticketRepository.countByStatus(TicketStatus.RESOLVED),
                ticketRepository.countByStatus(TicketStatus.CLOSED),
                depositRequestRepository.countByCreatedAtGreaterThanEqual(todayStart),
                orderRepository.sumAmountByStatusBetween(OrderStatus.COMPLETED, todayStart, Instant.now()),
                bankTransactionRepository.countByStatus(BankTransactionStatus.NEW)
                        + bankTransactionRepository.countByStatus(BankTransactionStatus.MANUAL_REVIEW));
    }

    @Transactional(readOnly = true)
    public RevenueReportResponse getRevenueReport(Instant fromDate, Instant toDate) {
        Instant from = fromDate != null ? fromDate : Instant.EPOCH;
        Instant to = toDate != null ? toDate : Instant.now();
        BigDecimal depositVolume = walletTransactionRepository.sumAmountByTypeAndDirectionBetween(
                WalletTransactionType.DEPOSIT, WalletTransactionDirection.CREDIT, from, to);
        BigDecimal grossRevenue = orderRepository.sumAmountByStatusBetween(OrderStatus.COMPLETED, from, to);
        BigDecimal totalRefunds = orderRepository.sumAmountByStatusBetween(OrderStatus.REFUNDED, from, to);
        BigDecimal totalCost = orderRepository.sumCostPriceByStatusBetween(OrderStatus.COMPLETED, from, to);
        BigDecimal netRevenue = grossRevenue.subtract(totalRefunds);
        BigDecimal profit = grossRevenue.subtract(totalCost);
        BigDecimal walletLiability = userAccountRepository.sumAllBalances();

        return new RevenueReportResponse(depositVolume, grossRevenue, totalRefunds, netRevenue, walletLiability, totalCost, profit);
    }

    @Transactional(readOnly = true)
    public RevenueReportResponse getRevenueReport() {
        return getRevenueReport(null, null);
    }
}
