package com.example.KendyDigital.dto.monitoring.response;

import java.time.Instant;

public record SecurityTimelinePointResponse(
        Instant bucket,
        long loginFailures,
        long rateLimited,
        long wafHits,
        long webhookRejections,
        long total) {
}
