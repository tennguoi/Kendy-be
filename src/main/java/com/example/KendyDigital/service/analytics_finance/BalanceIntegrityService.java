package com.example.KendyDigital.service.analytics_finance;

import com.example.KendyDigital.dto.finance.response.BalanceIntegrityIssueResponse;
import com.example.KendyDigital.repository.*;
import java.util.List;

public interface BalanceIntegrityService {
    List<BalanceIntegrityIssueResponse> findIssues();
    void scheduledCheck();
}
