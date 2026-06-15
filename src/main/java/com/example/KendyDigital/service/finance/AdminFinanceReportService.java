package com.example.KendyDigital.service.finance;

import com.example.KendyDigital.dto.finance.response.AdminDashboardResponse;
import com.example.KendyDigital.dto.finance.response.RevenueReportResponse;
import java.time.Instant;

public interface AdminFinanceReportService {
    AdminDashboardResponse dashboard();
    RevenueReportResponse getRevenueReport(Instant fromDate, Instant toDate);
    RevenueReportResponse getRevenueReport();
}
