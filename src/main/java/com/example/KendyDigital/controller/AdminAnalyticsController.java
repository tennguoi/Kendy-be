package com.example.KendyDigital.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.KendyDigital.service.AdminExportAnalyticsService;

@RestController
public class AdminAnalyticsController {
    private final AdminExportAnalyticsService exportAnalyticsService;

    public AdminAnalyticsController(AdminExportAnalyticsService exportAnalyticsService) {
        this.exportAnalyticsService = exportAnalyticsService;
    }

    @GetMapping("/api/admin/dashboard/summary")
    public Map<String, Object> dashboardSummary() {
        return exportAnalyticsService.dashboardSummary();
    }

    @GetMapping("/api/admin/dashboard/revenue-chart")
    public List<Map<String, Object>> revenueChart(@RequestParam(required = false) Integer days) {
        return exportAnalyticsService.revenueChart(days);
    }

    @GetMapping("/api/admin/dashboard/user-activity")
    public Map<String, Object> userActivity() {
        return exportAnalyticsService.userActivity();
    }

    @GetMapping("/api/admin/dashboard/service-performance")
    public List<Map<String, Object>> servicePerformance(@RequestParam(required = false) Integer limit) {
        return exportAnalyticsService.servicePerformance(limit);
    }

    @GetMapping("/api/admin/reports/revenue/export")
    public ResponseEntity<?> exportRevenue(@RequestParam(required = false) String format) {
        if ("xlsx".equalsIgnoreCase(format)) {
            byte[] data = exportAnalyticsService.exportRevenueXlsx();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"revenue.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(data);
        }
        return csv("revenue.csv", exportAnalyticsService.exportRevenue());
    }

    @GetMapping("/api/admin/reports/users/export")
    public ResponseEntity<?> exportUsers(@RequestParam(required = false) String format) {
        if ("xlsx".equalsIgnoreCase(format)) {
            byte[] data = exportAnalyticsService.exportUsersXlsx();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"users.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(data);
        }
        return csv("users.csv", exportAnalyticsService.exportUsers());
    }

    @GetMapping("/api/admin/reports/orders/export")
    public ResponseEntity<?> exportOrders(@RequestParam(required = false) String format) {
        if ("xlsx".equalsIgnoreCase(format)) {
            byte[] data = exportAnalyticsService.exportOrdersXlsx();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"orders.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(data);
        }
        return csv("orders.csv", exportAnalyticsService.exportOrders());
    }

    @GetMapping("/api/admin/reports/bank/export")
    public ResponseEntity<?> exportBank(@RequestParam(required = false) String format) {
        if ("xlsx".equalsIgnoreCase(format)) {
            byte[] data = exportAnalyticsService.exportBankXlsx();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bank-transactions.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(data);
        }
        return csv("bank-transactions.csv", exportAnalyticsService.exportBank());
    }

    @GetMapping("/api/admin/reports/tickets/export")
    public ResponseEntity<?> exportTickets(@RequestParam(required = false) String format) {
        if ("xlsx".equalsIgnoreCase(format)) {
            byte[] data = exportAnalyticsService.exportTicketsXlsx();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tickets.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(data);
        }
        return csv("tickets.csv", exportAnalyticsService.exportTickets());
    }

    @GetMapping("/api/admin/audit-logs/export")
    public ResponseEntity<?> exportAudit(@RequestParam(required = false) String format) {
        if ("xlsx".equalsIgnoreCase(format)) {
            byte[] data = exportAnalyticsService.exportAuditXlsx();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-logs.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(data);
        }
        return csv("audit-logs.csv", exportAnalyticsService.exportAudit());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ResponseEntity<String> csv(String fileName, String body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(body);
    }
}
