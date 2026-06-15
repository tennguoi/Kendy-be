package com.example.KendyDigital.service.analytics;

import java.util.List;
import java.util.Map;

public interface AdminExportAnalyticsService {
    Map<String, Object> dashboardSummary();
    List<Map<String, Object>> revenueChart(Integer days);
    Map<String, Object> userActivity();
    List<Map<String, Object>> servicePerformance(Integer limit);
    String exportRevenue();
    byte[] exportRevenueXlsx();
    String exportUsers();
    byte[] exportUsersXlsx();
    String exportOrders();
    byte[] exportOrdersXlsx();
    String exportBank();
    byte[] exportBankXlsx();
    String exportTickets();
    byte[] exportTicketsXlsx();
    String exportAudit();
    byte[] exportAuditXlsx();
}
