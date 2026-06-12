package com.example.KendyDigital.service;




import com.example.KendyDigital.repository.*;
import com.example.KendyDigital.model.*;
import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.KendyDigital.service.AuditService;
import com.example.KendyDigital.dto.finance.response.BalanceIntegrityIssueResponse;

@Service
public class BalanceIntegrityService {
    private static final Logger log = LoggerFactory.getLogger(BalanceIntegrityService.class);

    private final BalanceIntegrityRepository balanceIntegrityRepository;
    private final AuditService auditService;

    public BalanceIntegrityService(BalanceIntegrityRepository balanceIntegrityRepository, AuditService auditService) {
        this.balanceIntegrityRepository = balanceIntegrityRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<BalanceIntegrityIssueResponse> findIssues() {
        return balanceIntegrityRepository.findBalanceDiscrepancies();
    }

    @Scheduled(cron = "${app.finance.integrity-check-cron:0 15 * * * *}")
    @Transactional
    public void scheduledCheck() {
        List<BalanceIntegrityIssueResponse> issues = findIssues();
        if (issues.isEmpty()) {
            return;
        }

        log.warn("Wallet balance integrity check found {} issue(s)", issues.size());
        auditService.recordSystem(
                "BALANCE_INTEGRITY_ISSUES_FOUND",
                "WALLET",
                null,
                "issueCount=" + issues.size());
    }
}
