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
    public byte[] exportRevenueXlsx() {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.xssf.usermodel.XSSFFont font = wb.createFont();
            font.setFontName("Noto Sans");
            org.apache.poi.xssf.usermodel.XSSFCellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(font);
            headerStyle.setWrapText(false);
            org.apache.poi.xssf.usermodel.XSSFCellStyle bodyStyle = wb.createCellStyle();
            bodyStyle.setFont(font);
            org.apache.poi.xssf.usermodel.XSSFSheet sheet = wb.createSheet("Revenue");
            String[] headers = new String[]{"depositVolume","grossRevenue","totalRefunds","netRevenue","walletLiability","totalCost","profit"};
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell c = header.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }
            var report = financeReportService.getRevenueReport();
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(1);
            int cidx = 0;
            org.apache.poi.ss.usermodel.Cell c0 = row.createCell(cidx++);
            c0.setCellValue(report.depositVolume() == null ? "" : report.depositVolume().toString()); c0.setCellStyle(bodyStyle);
            org.apache.poi.ss.usermodel.Cell c1 = row.createCell(cidx++);
            c1.setCellValue(report.grossRevenue() == null ? "" : report.grossRevenue().toString()); c1.setCellStyle(bodyStyle);
            org.apache.poi.ss.usermodel.Cell c2 = row.createCell(cidx++);
            c2.setCellValue(report.totalRefunds() == null ? "" : report.totalRefunds().toString()); c2.setCellStyle(bodyStyle);
            org.apache.poi.ss.usermodel.Cell c3 = row.createCell(cidx++);
            c3.setCellValue(report.netRevenue() == null ? "" : report.netRevenue().toString()); c3.setCellStyle(bodyStyle);
            org.apache.poi.ss.usermodel.Cell c4 = row.createCell(cidx++);
            c4.setCellValue(report.walletLiability() == null ? "" : report.walletLiability().toString()); c4.setCellStyle(bodyStyle);
            org.apache.poi.ss.usermodel.Cell c5 = row.createCell(cidx++);
            c5.setCellValue(report.totalCost() == null ? "" : report.totalCost().toString()); c5.setCellStyle(bodyStyle);
            org.apache.poi.ss.usermodel.Cell c6 = row.createCell(cidx++);
            c6.setCellValue(report.profit() == null ? "" : report.profit().toString()); c6.setCellStyle(bodyStyle);
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
                wb.write(out);
                return out.toByteArray();
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("Cannot generate XLSX", e);
        }
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
    public byte[] exportUsersXlsx() {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.xssf.usermodel.XSSFFont font = wb.createFont();
            // Use Noto Sans for Vietnamese; ensure the font is installed on the system
            font.setFontName("Noto Sans");

            org.apache.poi.xssf.usermodel.XSSFCellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(font);
            headerStyle.setWrapText(false);

            org.apache.poi.xssf.usermodel.XSSFCellStyle bodyStyle = wb.createCellStyle();
            bodyStyle.setFont(font);

            org.apache.poi.xssf.usermodel.XSSFSheet sheet = wb.createSheet("Users");

            // Header
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            String[] headers = new String[]{"id","email","name","phone","role","status","balance","createdAt"};
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell c = header.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            // Rows
            java.util.List<com.example.KendyDigital.model.UserAccount> users =
                    userAccountRepository.findAllByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, 5000));
            int r = 1;
            for (com.example.KendyDigital.model.UserAccount u : users) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(r++);
                int cidx = 0;
                org.apache.poi.ss.usermodel.Cell c0 = row.createCell(cidx++);
                c0.setCellValue(u.getId() == null ? "" : u.getId().toString());
                c0.setCellStyle(bodyStyle);

                org.apache.poi.ss.usermodel.Cell c1 = row.createCell(cidx++);
                c1.setCellValue(u.getEmail() == null ? "" : u.getEmail());
                c1.setCellStyle(bodyStyle);

                org.apache.poi.ss.usermodel.Cell c2 = row.createCell(cidx++);
                c2.setCellValue(u.getName() == null ? "" : u.getName());
                c2.setCellStyle(bodyStyle);

                org.apache.poi.ss.usermodel.Cell c3 = row.createCell(cidx++);
                c3.setCellValue(u.getPhone() == null ? "" : u.getPhone());
                c3.setCellStyle(bodyStyle);

                org.apache.poi.ss.usermodel.Cell c4 = row.createCell(cidx++);
                c4.setCellValue(u.getRole() == null ? "" : u.getRole().toString());
                c4.setCellStyle(bodyStyle);

                org.apache.poi.ss.usermodel.Cell c5 = row.createCell(cidx++);
                c5.setCellValue(u.getStatus() == null ? "" : u.getStatus().toString());
                c5.setCellStyle(bodyStyle);

                org.apache.poi.ss.usermodel.Cell c6 = row.createCell(cidx++);
                c6.setCellValue(u.getBalance() == null ? "" : u.getBalance().toString());
                c6.setCellStyle(bodyStyle);

                org.apache.poi.ss.usermodel.Cell c7 = row.createCell(cidx++);
                c7.setCellValue(u.getCreatedAt() == null ? "" : u.getCreatedAt().toString());
                c7.setCellStyle(bodyStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
                wb.write(out);
                return out.toByteArray();
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("Cannot generate XLSX", e);
        }
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
    public byte[] exportOrdersXlsx() {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.xssf.usermodel.XSSFFont font = wb.createFont();
            font.setFontName("Noto Sans");
            org.apache.poi.xssf.usermodel.XSSFCellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(font);
            headerStyle.setWrapText(false);
            org.apache.poi.xssf.usermodel.XSSFCellStyle bodyStyle = wb.createCellStyle();
            bodyStyle.setFont(font);
            org.apache.poi.xssf.usermodel.XSSFSheet sheet = wb.createSheet("Orders");
            String[] headers = new String[]{"id","orderCode","userId","serviceId","amount","status","createdAt"};
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell c = header.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }
            java.util.List<com.example.KendyDigital.model.OrderRecord> orders =
                    orderRepository.findAllByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, 5000));
            int r = 1;
            for (com.example.KendyDigital.model.OrderRecord o : orders) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(r++);
                int cidx = 0;
                org.apache.poi.ss.usermodel.Cell c0 = row.createCell(cidx++);
                c0.setCellValue(o.getId() == null ? "" : o.getId().toString()); c0.setCellStyle(bodyStyle);
                org.apache.poi.ss.usermodel.Cell c1 = row.createCell(cidx++);
                c1.setCellValue(o.getOrderCode() == null ? "" : o.getOrderCode()); c1.setCellStyle(bodyStyle);
                org.apache.poi.ss.usermodel.Cell c2 = row.createCell(cidx++);
                c2.setCellValue(o.getUser() == null || o.getUser().getId() == null ? "" : o.getUser().getId().toString()); c2.setCellStyle(bodyStyle);
                org.apache.poi.ss.usermodel.Cell c3 = row.createCell(cidx++);
                c3.setCellValue(o.getService() == null || o.getService().getId() == null ? "" : o.getService().getId().toString()); c3.setCellStyle(bodyStyle);
                org.apache.poi.ss.usermodel.Cell c4 = row.createCell(cidx++);
                c4.setCellValue(o.getAmount() == null ? "" : o.getAmount().toString()); c4.setCellStyle(bodyStyle);
                org.apache.poi.ss.usermodel.Cell c5 = row.createCell(cidx++);
                c5.setCellValue(o.getStatus() == null ? "" : o.getStatus().toString()); c5.setCellStyle(bodyStyle);
                org.apache.poi.ss.usermodel.Cell c6 = row.createCell(cidx++);
                c6.setCellValue(o.getCreatedAt() == null ? "" : o.getCreatedAt().toString()); c6.setCellStyle(bodyStyle);
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
                wb.write(out);
                return out.toByteArray();
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("Cannot generate XLSX", e);
        }
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
    public byte[] exportBankXlsx() {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.xssf.usermodel.XSSFFont font = wb.createFont();
            font.setFontName("Noto Sans");
            org.apache.poi.xssf.usermodel.XSSFCellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(font);
            headerStyle.setWrapText(false);
            org.apache.poi.xssf.usermodel.XSSFCellStyle bodyStyle = wb.createCellStyle();
            bodyStyle.setFont(font);
            org.apache.poi.xssf.usermodel.XSSFSheet sheet = wb.createSheet("BankTransactions");
            String[] headers = new String[]{"id","sepayId","referenceCode","transferType","transferAmount","status","receivedAt"};
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell c = header.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }
            java.util.List<com.example.KendyDigital.model.BankTransaction> txs =
                    bankTransactionRepository.findAllByOrderByReceivedAtDesc(org.springframework.data.domain.PageRequest.of(0, 5000));
            int r = 1;
            for (com.example.KendyDigital.model.BankTransaction tx : txs) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(r++);
                int cidx = 0;
                org.apache.poi.ss.usermodel.Cell c0 = row.createCell(cidx++);
                c0.setCellValue(tx.getId() == null ? "" : tx.getId().toString()); c0.setCellStyle(bodyStyle);
                org.apache.poi.ss.usermodel.Cell c1 = row.createCell(cidx++);
                c1.setCellValue(tx.getSepayId() == null ? "" : tx.getSepayId().toString()); c1.setCellStyle(bodyStyle);
                org.apache.poi.ss.usermodel.Cell c2 = row.createCell(cidx++);
                c2.setCellValue(tx.getReferenceCode() == null ? "" : tx.getReferenceCode()); c2.setCellStyle(bodyStyle);
                org.apache.poi.ss.usermodel.Cell c3 = row.createCell(cidx++);
                c3.setCellValue(tx.getTransferType() == null ? "" : tx.getTransferType()); c3.setCellStyle(bodyStyle);
                org.apache.poi.ss.usermodel.Cell c4 = row.createCell(cidx++);
                c4.setCellValue(tx.getTransferAmount() == null ? "" : tx.getTransferAmount().toString()); c4.setCellStyle(bodyStyle);
                org.apache.poi.ss.usermodel.Cell c5 = row.createCell(cidx++);
                c5.setCellValue(tx.getStatus() == null ? "" : tx.getStatus().toString()); c5.setCellStyle(bodyStyle);
                org.apache.poi.ss.usermodel.Cell c6 = row.createCell(cidx++);
                c6.setCellValue(tx.getReceivedAt() == null ? "" : tx.getReceivedAt().toString()); c6.setCellStyle(bodyStyle);
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
                wb.write(out);
                return out.toByteArray();
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("Cannot generate XLSX", e);
        }
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
    public byte[] exportTicketsXlsx() {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.xssf.usermodel.XSSFFont font = wb.createFont();
            font.setFontName("Noto Sans");
            org.apache.poi.xssf.usermodel.XSSFCellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(font);
            headerStyle.setWrapText(false);
            org.apache.poi.xssf.usermodel.XSSFCellStyle bodyStyle = wb.createCellStyle();
            bodyStyle.setFont(font);
            org.apache.poi.xssf.usermodel.XSSFSheet sheet = wb.createSheet("Tickets");
            String[] headers = new String[]{"id","ticketCode","userId","category","priority","status","createdAt","closedAt"};
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell c = header.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }
            java.util.List<com.example.KendyDigital.model.Ticket> tickets =
                    ticketRepository.findAllByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, 5000));
            int r = 1;
            for (com.example.KendyDigital.model.Ticket t : tickets) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(r++);
                int cidx = 0;
                org.apache.poi.ss.usermodel.Cell c0 = row.createCell(cidx++);
                c0.setCellValue(t.getId() == null ? "" : t.getId().toString()); c0.setCellStyle(bodyStyle);
                org.apache.poi.ss.usermodel.Cell c1 = row.createCell(cidx++);
                c1.setCellValue(t.getTicketCode() == null ? "" : t.getTicketCode()); c1.setCellStyle(bodyStyle);
                org.apache.poi.ss.usermodel.Cell c2 = row.createCell(cidx++);
                c2.setCellValue(t.getUser() == null || t.getUser().getId() == null ? "" : t.getUser().getId().toString()); c2.setCellStyle(bodyStyle);
                org.apache.poi.ss.usermodel.Cell c3 = row.createCell(cidx++);
                c3.setCellValue(t.getCategory() == null ? "" : t.getCategory().toString()); c3.setCellStyle(bodyStyle);
                org.apache.poi.ss.usermodel.Cell c4 = row.createCell(cidx++);
                c4.setCellValue(t.getPriority() == null ? "" : t.getPriority().toString()); c4.setCellStyle(bodyStyle);
                org.apache.poi.ss.usermodel.Cell c5 = row.createCell(cidx++);
                c5.setCellValue(t.getStatus() == null ? "" : t.getStatus().toString()); c5.setCellStyle(bodyStyle);
                org.apache.poi.ss.usermodel.Cell c6 = row.createCell(cidx++);
                c6.setCellValue(t.getCreatedAt() == null ? "" : t.getCreatedAt().toString()); c6.setCellStyle(bodyStyle);
                org.apache.poi.ss.usermodel.Cell c7 = row.createCell(cidx++);
                c7.setCellValue(t.getClosedAt() == null ? "" : t.getClosedAt().toString()); c7.setCellStyle(bodyStyle);
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
                wb.write(out);
                return out.toByteArray();
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("Cannot generate XLSX", e);
        }
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

    @Transactional(readOnly = true)
    public byte[] exportAuditXlsx() {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.xssf.usermodel.XSSFFont font = wb.createFont();
            font.setFontName("Noto Sans");
            org.apache.poi.xssf.usermodel.XSSFCellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(font);
            headerStyle.setWrapText(false);
            org.apache.poi.xssf.usermodel.XSSFCellStyle bodyStyle = wb.createCellStyle();
            bodyStyle.setFont(font);
            org.apache.poi.xssf.usermodel.XSSFSheet sheet = wb.createSheet("AuditLogs");
            String[] headers = new String[]{"id","actorUserId","actorRole","action","targetType","targetId","metadata","createdAt"};
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell c = header.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }
            java.util.List<com.example.KendyDigital.model.AuditLog> logs =
                    auditLogRepository.findAllByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, 5000));
            int r = 1;
            for (com.example.KendyDigital.model.AuditLog log : logs) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(r++);
                int cidx = 0;
                org.apache.poi.ss.usermodel.Cell c0 = row.createCell(cidx++);
                c0.setCellValue(log.getId() == null ? "" : log.getId().toString()); c0.setCellStyle(bodyStyle);
                org.apache.poi.ss.usermodel.Cell c1 = row.createCell(cidx++);
                c1.setCellValue(log.getActorUserId() == null ? "" : log.getActorUserId().toString()); c1.setCellStyle(bodyStyle);
                org.apache.poi.ss.usermodel.Cell c2 = row.createCell(cidx++);
                c2.setCellValue(log.getActorRole() == null ? "" : log.getActorRole()); c2.setCellStyle(bodyStyle);
                org.apache.poi.ss.usermodel.Cell c3 = row.createCell(cidx++);
                c3.setCellValue(log.getAction() == null ? "" : log.getAction()); c3.setCellStyle(bodyStyle);
                org.apache.poi.ss.usermodel.Cell c4 = row.createCell(cidx++);
                c4.setCellValue(log.getTargetType() == null ? "" : log.getTargetType()); c4.setCellStyle(bodyStyle);
                org.apache.poi.ss.usermodel.Cell c5 = row.createCell(cidx++);
                c5.setCellValue(log.getTargetId() == null ? "" : log.getTargetId().toString()); c5.setCellStyle(bodyStyle);
                org.apache.poi.ss.usermodel.Cell c6 = row.createCell(cidx++);
                c6.setCellValue(log.getMetadata() == null ? "" : log.getMetadata()); c6.setCellStyle(bodyStyle);
                org.apache.poi.ss.usermodel.Cell c7 = row.createCell(cidx++);
                c7.setCellValue(log.getCreatedAt() == null ? "" : log.getCreatedAt().toString()); c7.setCellStyle(bodyStyle);
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
                wb.write(out);
                return out.toByteArray();
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("Cannot generate XLSX", e);
        }
    }

    private String csv(List<String> rows) {
        String content = String.join("\n", rows) + "\n";
        // Prepend UTF-8 BOM and Excel separator hint so Excel opens CSV with correct encoding and delimiter
        return "\uFEFF" + "sep=,\n" + content;
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
