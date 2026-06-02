package com.example.KendyDigital.dto;


import com.example.KendyDigital.model.*;
import java.math.BigDecimal;

public record SePayWebhookPayload(
        Long id,
        String gateway,
        String transactionDate,
        String accountNumber,
        String subAccount,
        String code,
        String content,
        String transferType,
        BigDecimal transferAmount,
        BigDecimal accumulated,
        String referenceCode) {
}
