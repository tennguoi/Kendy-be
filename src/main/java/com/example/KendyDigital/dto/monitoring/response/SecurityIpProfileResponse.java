package com.example.KendyDigital.dto.monitoring.response;

import java.time.Instant;
import java.util.List;

public record SecurityIpProfileResponse(
        String ip,
        double riskScore,
        boolean banned,
        boolean allowlisted,
        Instant firstSeen,
        Instant lastSeen,
        long eventCount,
        List<SecurityEventResponse> recentEvents) {
}
