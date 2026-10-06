package com.example.KendyDigital.dto.monitoring.response;

import java.time.Instant;

public record SecurityTopIpResponse(
        String ip,
        long eventCount,
        String maxSeverity,
        double riskScore,
        String country,
        String asn,
        boolean banned) {
}
