package com.example.KendyDigital.service.finance;

import com.example.KendyDigital.dto.finance.response.AdminDashboardResponse;
import com.example.KendyDigital.dto.finance.response.RevenueReportResponse;
import com.example.KendyDigital.model.bank.BankTransactionStatus;
import com.example.KendyDigital.model.catalog.ServiceType;
import com.example.KendyDigital.model.deposit.DepositStatus;
import com.example.KendyDigital.model.order.OrderStatus;
import com.example.KendyDigital.model.ticket.TicketStatus;
import com.example.KendyDigital.model.user.UserStatus;
import com.example.KendyDigital.model.warranty.WarrantyRequestStatus;
import com.example.KendyDigital.model.wallet.WalletTransactionDirection;
import com.example.KendyDigital.model.wallet.WalletTransactionType;
import com.example.KendyDigital.repository.AccountCredentialRepository;
import com.example.KendyDigital.repository.BankTransactionRepository;
import com.example.KendyDigital.repository.DepositRequestRepository;
import com.example.KendyDigital.repository.OrderRepository;
import com.example.KendyDigital.repository.TicketRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.repository.WarrantyRequestRepository;
import com.example.KendyDigital.repository.WalletTransactionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminFinanceReportServiceImpl  implements AdminFinanceReportService{
    private final UserAccountRepository userAccountRepository;
    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final DepositRequestRepository depositRequestRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final AccountCredentialRepository accountCredentialRepository;
    private final WarrantyRequestRepository warrantyRequestRepository;

    public AdminFinanceReportServiceImpl(UserAccountRepository userAccountRepository,
            OrderRepository orderRepository,
            TicketRepository ticketRepository,
            DepositRequestRepository depositRequestRepository,
            WalletTransactionRepository walletTransactionRepository,
            BankTransactionRepository bankTransactionRepository,
            AccountCredentialRepository accountCredentialRepository,
            WarrantyRequestRepository warrantyRequestRepository) {
        this.userAccountRepository = userAccountRepository;
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
        this.depositRequestRepository = depositRequestRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.accountCredentialRepository = accountCredentialRepository;
        this.warrantyRequestRepository = warrantyRequestRepository;
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
                        + bankTransactionRepository.countByStatus(BankTransactionStatus.MANUAL_REVIEW),
                orderRepository.countByService_TypeAndStatus(ServiceType.MANUAL, OrderStatus.PROCESSING),
                warrantyRequestRepository.countByStatusIn(List.of(
                        WarrantyRequestStatus.OPEN,
                        WarrantyRequestStatus.REVIEWING)),
                accountCredentialRepository.countLowStockServices(2),
                accountCredentialRepository.countExpiringCredentials(Instant.now(), Instant.now().plus(7, ChronoUnit.DAYS)));
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
