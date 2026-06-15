package com.example.KendyDigital.dto.webhook.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SePayWebhookPayload(
        Long id,
        String gateway,
        String transactionDate,
        String accountNumber,
        String subAccount,
        String code,
        String content,
        String transferType,
        String description,
        BigDecimal transferAmount,
        BigDecimal accumulated,
        String referenceCode) {
}
