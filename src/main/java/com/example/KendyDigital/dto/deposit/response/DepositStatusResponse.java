package com.example.KendyDigital.dto.deposit.response;

import com.example.KendyDigital.model.deposit.DepositStatus;
import java.time.Instant;

public record DepositStatusResponse(
        String depositCode,
        DepositStatus status,
        Instant expiredAt,
        Instant completedAt,
        Instant serverTime) {
}
