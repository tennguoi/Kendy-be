package com.example.KendyDigital.service.analytics_finance.export;

import com.example.KendyDigital.dto.finance.response.RevenueReportResponse;
import com.example.KendyDigital.model.audit.AuditLog;
import com.example.KendyDigital.model.bank.BankTransaction;
import com.example.KendyDigital.model.order.OrderRecord;
import com.example.KendyDigital.model.ticket.Ticket;
import com.example.KendyDigital.model.user.UserAccount;
import java.util.List;

public interface ReportExportService {
    String exportRevenueCsv(RevenueReportResponse report);
    byte[] exportRevenueXlsx(RevenueReportResponse report);

    String exportUsersCsv(List<UserAccount> users);
    byte[] exportUsersXlsx(List<UserAccount> users);

    String exportOrdersCsv(List<OrderRecord> orders);
    byte[] exportOrdersXlsx(List<OrderRecord> orders);

    String exportBankTransactionsCsv(List<BankTransaction> transactions);
    byte[] exportBankTransactionsXlsx(List<BankTransaction> transactions);

    String exportTicketsCsv(List<Ticket> tickets);
    byte[] exportTicketsXlsx(List<Ticket> tickets);

    String exportAuditLogsCsv(List<AuditLog> auditLogs);
    byte[] exportAuditLogsXlsx(List<AuditLog> auditLogs);
}
