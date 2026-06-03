package com.example.KendyDigital.dto;

import java.time.Instant;

import com.example.KendyDigital.model.DepositStatus;

public record DepositStatusResponse(
        String depositCode,
        DepositStatus status,
        Instant expiredAt,
        Instant completedAt,
        Instant serverTime) {
}
