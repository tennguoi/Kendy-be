package com.example.KendyDigital.repository;


import com.example.KendyDigital.model.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.KendyDigital.dto.webhook.request.SePayWebhookPayload;

@Repository
public class BankTransactionInserter {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public BankTransactionInserter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Long> insertNew(SePayWebhookPayload payload, String rawPayload, Instant receivedAt,
            Instant transactionDate) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("sepayId", payload.id())
                .addValue("gateway", blankToNull(payload.gateway()))
                .addValue("bankName", blankToNull(payload.gateway()))
                .addValue("accountNumber", blankToNull(payload.accountNumber()))
                .addValue("subAccount", blankToNull(payload.subAccount()))
                .addValue("transactionDate", transactionDate == null ? null : Timestamp.from(transactionDate))
                .addValue("transferType", blankToNull(payload.transferType()))
                .addValue("transferAmount", payload.transferAmount())
                .addValue("accumulated", payload.accumulated())
                .addValue("code", blankToNull(payload.code()))
                .addValue("content", blankToNull(payload.content()))
                .addValue("referenceCode", blankToNull(payload.referenceCode()))
                .addValue("status", BankTransactionStatus.NEW.name())
                .addValue("rawPayload", rawPayload)
                .addValue("receivedAt", Timestamp.from(receivedAt))
                .addValue("createdAt", Timestamp.from(receivedAt))
                .addValue("updatedAt", Timestamp.from(receivedAt));

        String sql = """
                insert into bank_transactions (
                    sepay_id, gateway, bank_name, account_number, sub_account, transaction_date,
                    transfer_type, transfer_amount, accumulated, code, content, reference_code,
                    status, raw_payload, received_at, created_at, updated_at
                )
                values (
                    :sepayId, :gateway, :bankName, :accountNumber, :subAccount, :transactionDate,
                    :transferType, :transferAmount, :accumulated, :code, :content, :referenceCode,
                    :status, :rawPayload, :receivedAt, :createdAt, :updatedAt
                )
                on conflict do nothing
                returning id
                """;

        List<Long> ids = jdbcTemplate.query(sql, params, (rs, rowNum) -> rs.getLong("id"));
        return ids.stream().findFirst();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
