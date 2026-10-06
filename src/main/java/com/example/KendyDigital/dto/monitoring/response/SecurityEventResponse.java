package com.example.KendyDigital.dto.monitoring.response;

import com.example.KendyDigital.model.security.SecurityEvent;
import java.time.Instant;

public record SecurityEventResponse(
        Long id,
        Instant occurredAt,
        String type,
        String severity,
        String ip,
        String asn,
        String country,
        Long userId,
        Long sessionId,
        String method,
        String path,
        String userAgent,
        int eventCount,
        String metadata) {
    public static SecurityEventResponse from(SecurityEvent event) {
        return new SecurityEventResponse(
                event.getId(),
                event.getOccurredAt(),
                event.getType() == null ? null : event.getType().name(),
                event.getSeverity() == null ? null : event.getSeverity().name(),
                event.getIp(),
                event.getAsn(),
                event.getCountry(),
                event.getUserId(),
                event.getSessionId(),
                event.getMethod(),
                event.getPath(),
                event.getUserAgent(),
                event.getEventCount(),
                event.getMetadata());
    }
}
