package com.example.KendyDigital.repository;

import com.example.KendyDigital.dto.finance.response.BalanceIntegrityIssueResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BalanceIntegrityRepository {

    private final JdbcTemplate jdbcTemplate;

    public BalanceIntegrityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<BalanceIntegrityIssueResponse> findBalanceDiscrepancies() {
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
}
