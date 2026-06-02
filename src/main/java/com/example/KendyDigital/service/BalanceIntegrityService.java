package com.example.KendyDigital.service;




import com.example.KendyDigital.dto.*;
import com.example.KendyDigital.repository.*;
import com.example.KendyDigital.model.*;
import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.KendyDigital.service.AuditService;
import com.example.KendyDigital.dto.BalanceIntegrityIssueResponse;

@Service
public class BalanceIntegrityService {
    private static final Logger log = LoggerFactory.getLogger(BalanceIntegrityService.class);

    private final JdbcTemplate jdbcTemplate;
    private final AuditService auditService;

    public BalanceIntegrityService(JdbcTemplate jdbcTemplate, AuditService auditService) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<BalanceIntegrityIssueResponse> findIssues() {
        String sql = """
                select
                    u.id as user_id,
                    u.email as email,
                    u.balance as stored_balance,
                    coalesce(sum(
                        case
                            when wt.direction = 'CREDIT' then wt.amount
                            when wt.direction = 'DEBIT' then -wt.amount
                            else 0
                        end
                    ), 0) as ledger_balance
                from users u
                left join wallet_transactions wt on wt.user_id = u.id
                group by u.id, u.email, u.balance
                having u.balance <> coalesce(sum(
                    case
                        when wt.direction = 'CREDIT' then wt.amount
                        when wt.direction = 'DEBIT' then -wt.amount
                        else 0
                    end
                ), 0)
                order by u.id
                limit 100
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            BigDecimal storedBalance = rs.getBigDecimal("stored_balance");
            BigDecimal ledgerBalance = rs.getBigDecimal("ledger_balance");
            return new BalanceIntegrityIssueResponse(
                    rs.getLong("user_id"),
                    rs.getString("email"),
                    storedBalance,
                    ledgerBalance,
                    storedBalance.subtract(ledgerBalance));
        });
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
