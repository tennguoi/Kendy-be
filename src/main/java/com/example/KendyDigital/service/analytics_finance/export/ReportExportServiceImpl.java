package com.example.KendyDigital.service.analytics_finance.export;

import com.example.KendyDigital.dto.finance.response.RevenueReportResponse;
import com.example.KendyDigital.model.audit.AuditLog;
import com.example.KendyDigital.model.bank.BankTransaction;
import com.example.KendyDigital.model.order.OrderRecord;
import com.example.KendyDigital.model.ticket.Ticket;
import com.example.KendyDigital.model.user.UserAccount;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ReportExportServiceImpl implements ReportExportService {

    @Override
    public String exportRevenueCsv(RevenueReportResponse report) {
        return csv(List.of("depositVolume,grossRevenue,totalRefunds,netRevenue,walletLiability,totalCost,profit",
                csvRow(report.depositVolume(), report.grossRevenue(), report.totalRefunds(), report.netRevenue(),
                        report.walletLiability(), report.totalCost(), report.profit())));
    }

    @Override
    public byte[] exportRevenueXlsx(RevenueReportResponse report) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles styles = createStyles(wb);
            XSSFSheet sheet = wb.createSheet("Revenue");
            String[] headers = new String[]{"depositVolume", "grossRevenue", "totalRefunds", "netRevenue", "walletLiability", "totalCost", "profit"};
            createHeaderRow(sheet, headers, styles.headerStyle);

            Row row = sheet.createRow(1);
            int cidx = 0;
            addCell(row, cidx++, report.depositVolume(), styles.bodyStyle);
            addCell(row, cidx++, report.grossRevenue(), styles.bodyStyle);
            addCell(row, cidx++, report.totalRefunds(), styles.bodyStyle);
            addCell(row, cidx++, report.netRevenue(), styles.bodyStyle);
            addCell(row, cidx++, report.walletLiability(), styles.bodyStyle);
            addCell(row, cidx++, report.totalCost(), styles.bodyStyle);
            addCell(row, cidx++, report.profit(), styles.bodyStyle);

            autoSizeColumns(sheet, headers.length);
            return toByteArray(wb);
        } catch (IOException e) {
            throw new RuntimeException("Cannot generate XLSX", e);
        }
    }

    @Override
    public String exportUsersCsv(List<UserAccount> users) {
        List<String> rows = new ArrayList<>();
        rows.add("id,email,name,phone,role,status,balance,createdAt");
        users.forEach(user -> rows.add(csvRow(user.getId(), user.getEmail(), user.getName(), user.getPhone(),
                user.getRole(), user.getStatus(), user.getBalance(), user.getCreatedAt())));
        return csv(rows);
    }

    @Override
    public byte[] exportUsersXlsx(List<UserAccount> users) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles styles = createStyles(wb);
            XSSFSheet sheet = wb.createSheet("Users");
            String[] headers = new String[]{"id", "email", "name", "phone", "role", "status", "balance", "createdAt"};
            createHeaderRow(sheet, headers, styles.headerStyle);

            int r = 1;
            for (UserAccount u : users) {
                Row row = sheet.createRow(r++);
                int cidx = 0;
                addCell(row, cidx++, u.getId(), styles.bodyStyle);
                addCell(row, cidx++, u.getEmail(), styles.bodyStyle);
                addCell(row, cidx++, u.getName(), styles.bodyStyle);
                addCell(row, cidx++, u.getPhone(), styles.bodyStyle);
                addCell(row, cidx++, u.getRole(), styles.bodyStyle);
                addCell(row, cidx++, u.getStatus(), styles.bodyStyle);
                addCell(row, cidx++, u.getBalance(), styles.bodyStyle);
                addCell(row, cidx++, u.getCreatedAt(), styles.bodyStyle);
            }
            autoSizeColumns(sheet, headers.length);
            return toByteArray(wb);
        } catch (IOException e) {
            throw new RuntimeException("Cannot generate XLSX", e);
        }
    }

    @Override
    public String exportOrdersCsv(List<OrderRecord> orders) {
        List<String> rows = new ArrayList<>();
        rows.add("id,orderCode,userId,serviceId,amount,status,createdAt");
        orders.forEach(order -> rows.add(csvRow(order.getId(), order.getOrderCode(),
                order.getUser() == null ? null : order.getUser().getId(),
                order.getService() == null ? null : order.getService().getId(),
                order.getAmount(), order.getStatus(), order.getCreatedAt())));
        return csv(rows);
    }

    @Override
    public byte[] exportOrdersXlsx(List<OrderRecord> orders) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles styles = createStyles(wb);
            XSSFSheet sheet = wb.createSheet("Orders");
            String[] headers = new String[]{"id", "orderCode", "userId", "serviceId", "amount", "status", "createdAt"};
            createHeaderRow(sheet, headers, styles.headerStyle);

            int r = 1;
            for (OrderRecord o : orders) {
                Row row = sheet.createRow(r++);
                int cidx = 0;
                addCell(row, cidx++, o.getId(), styles.bodyStyle);
                addCell(row, cidx++, o.getOrderCode(), styles.bodyStyle);
                addCell(row, cidx++, o.getUser() == null ? null : o.getUser().getId(), styles.bodyStyle);
                addCell(row, cidx++, o.getService() == null ? null : o.getService().getId(), styles.bodyStyle);
                addCell(row, cidx++, o.getAmount(), styles.bodyStyle);
                addCell(row, cidx++, o.getStatus(), styles.bodyStyle);
                addCell(row, cidx++, o.getCreatedAt(), styles.bodyStyle);
            }
            autoSizeColumns(sheet, headers.length);
            return toByteArray(wb);
        } catch (IOException e) {
            throw new RuntimeException("Cannot generate XLSX", e);
        }
    }

    @Override
    public String exportBankTransactionsCsv(List<BankTransaction> transactions) {
        List<String> rows = new ArrayList<>();
        rows.add("id,sepayId,referenceCode,transferType,transferAmount,status,receivedAt");
        transactions.forEach(tx -> rows.add(csvRow(tx.getId(), tx.getSepayId(), tx.getReferenceCode(),
                tx.getTransferType(), tx.getTransferAmount(), tx.getStatus(), tx.getReceivedAt())));
        return csv(rows);
    }

    @Override
    public byte[] exportBankTransactionsXlsx(List<BankTransaction> transactions) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles styles = createStyles(wb);
            XSSFSheet sheet = wb.createSheet("BankTransactions");
            String[] headers = new String[]{"id", "sepayId", "referenceCode", "transferType", "transferAmount", "status", "receivedAt"};
            createHeaderRow(sheet, headers, styles.headerStyle);

            int r = 1;
            for (BankTransaction tx : transactions) {
                Row row = sheet.createRow(r++);
                int cidx = 0;
                addCell(row, cidx++, tx.getId(), styles.bodyStyle);
                addCell(row, cidx++, tx.getSepayId(), styles.bodyStyle);
                addCell(row, cidx++, tx.getReferenceCode(), styles.bodyStyle);
                addCell(row, cidx++, tx.getTransferType(), styles.bodyStyle);
                addCell(row, cidx++, tx.getTransferAmount(), styles.bodyStyle);
                addCell(row, cidx++, tx.getStatus(), styles.bodyStyle);
                addCell(row, cidx++, tx.getReceivedAt(), styles.bodyStyle);
            }
            autoSizeColumns(sheet, headers.length);
            return toByteArray(wb);
        } catch (IOException e) {
            throw new RuntimeException("Cannot generate XLSX", e);
        }
    }

    @Override
    public String exportTicketsCsv(List<Ticket> tickets) {
        List<String> rows = new ArrayList<>();
        rows.add("id,ticketCode,userId,category,priority,status,createdAt,closedAt");
        tickets.forEach(ticket -> rows.add(csvRow(ticket.getId(), ticket.getTicketCode(),
                ticket.getUser() == null ? null : ticket.getUser().getId(),
                ticket.getCategory(), ticket.getPriority(), ticket.getStatus(), ticket.getCreatedAt(), ticket.getClosedAt())));
        return csv(rows);
    }

    @Override
    public byte[] exportTicketsXlsx(List<Ticket> tickets) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles styles = createStyles(wb);
            XSSFSheet sheet = wb.createSheet("Tickets");
            String[] headers = new String[]{"id", "ticketCode", "userId", "category", "priority", "status", "createdAt", "closedAt"};
            createHeaderRow(sheet, headers, styles.headerStyle);

            int r = 1;
            for (Ticket t : tickets) {
                Row row = sheet.createRow(r++);
                int cidx = 0;
                addCell(row, cidx++, t.getId(), styles.bodyStyle);
                addCell(row, cidx++, t.getTicketCode(), styles.bodyStyle);
                addCell(row, cidx++, t.getUser() == null ? null : t.getUser().getId(), styles.bodyStyle);
                addCell(row, cidx++, t.getCategory(), styles.bodyStyle);
                addCell(row, cidx++, t.getPriority(), styles.bodyStyle);
                addCell(row, cidx++, t.getStatus(), styles.bodyStyle);
                addCell(row, cidx++, t.getCreatedAt(), styles.bodyStyle);
                addCell(row, cidx++, t.getClosedAt(), styles.bodyStyle);
            }
            autoSizeColumns(sheet, headers.length);
            return toByteArray(wb);
        } catch (IOException e) {
            throw new RuntimeException("Cannot generate XLSX", e);
        }
    }

    @Override
    public String exportAuditLogsCsv(List<AuditLog> auditLogs) {
        List<String> rows = new ArrayList<>();
        rows.add("id,actorUserId,actorRole,action,targetType,targetId,metadata,createdAt");
        auditLogs.forEach(log -> rows.add(csvRow(log.getId(), log.getActorUserId(), log.getActorRole(),
                log.getAction(), log.getTargetType(), log.getTargetId(), log.getMetadata(), log.getCreatedAt())));
        return csv(rows);
    }

    @Override
    public byte[] exportAuditLogsXlsx(List<AuditLog> auditLogs) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles styles = createStyles(wb);
            XSSFSheet sheet = wb.createSheet("AuditLogs");
            String[] headers = new String[]{"id", "actorUserId", "actorRole", "action", "targetType", "targetId", "metadata", "createdAt"};
            createHeaderRow(sheet, headers, styles.headerStyle);

            int r = 1;
            for (AuditLog log : auditLogs) {
                Row row = sheet.createRow(r++);
                int cidx = 0;
                addCell(row, cidx++, log.getId(), styles.bodyStyle);
                addCell(row, cidx++, log.getActorUserId(), styles.bodyStyle);
                addCell(row, cidx++, log.getActorRole(), styles.bodyStyle);
                addCell(row, cidx++, log.getAction(), styles.bodyStyle);
                addCell(row, cidx++, log.getTargetType(), styles.bodyStyle);
                addCell(row, cidx++, log.getTargetId(), styles.bodyStyle);
                addCell(row, cidx++, log.getMetadata(), styles.bodyStyle);
                addCell(row, cidx++, log.getCreatedAt(), styles.bodyStyle);
            }
            autoSizeColumns(sheet, headers.length);
            return toByteArray(wb);
        } catch (IOException e) {
            throw new RuntimeException("Cannot generate XLSX", e);
        }
    }

    private record Styles(XSSFCellStyle headerStyle, XSSFCellStyle bodyStyle) {}

    private Styles createStyles(XSSFWorkbook wb) {
        XSSFFont font = wb.createFont();
        font.setFontName("Noto Sans");

        XSSFCellStyle headerStyle = wb.createCellStyle();
        headerStyle.setFont(font);
        headerStyle.setWrapText(false);

        XSSFCellStyle bodyStyle = wb.createCellStyle();
        bodyStyle.setFont(font);

        return new Styles(headerStyle, bodyStyle);
    }

    private void createHeaderRow(XSSFSheet sheet, String[] headers, XSSFCellStyle headerStyle) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell c = header.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }
    }

    private void addCell(Row row, int index, Object value, XSSFCellStyle bodyStyle) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value == null ? "" : value.toString());
        cell.setCellStyle(bodyStyle);
    }

    private void autoSizeColumns(XSSFSheet sheet, int count) {
        for (int i = 0; i < count; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private byte[] toByteArray(XSSFWorkbook wb) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            wb.write(out);
            return out.toByteArray();
        }
    }

    private String csv(List<String> rows) {
        String content = String.join("\n", rows) + "\n";
        return "\uFEFF" + "sep=,\n" + content;
    }

    private String csvRow(Object... values) {
        return Arrays.stream(values)
                .map(value -> value == null ? "" : value.toString())
                .map(value -> "\"" + value.replace("\"", "\"\"") + "\"")
                .collect(Collectors.joining(","));
    }
}
