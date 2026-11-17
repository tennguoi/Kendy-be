package com.example.KendyDigital.dto.monitoring.response;

import com.example.KendyDigital.model.security.SecurityAlert;
import java.time.Instant;
import java.util.UUID;

public record SecurityAlertResponse(
        UUID id,
        String ruleCode,
        String severity,
        String title,
        String subjectType,
        String subjectValue,
        Instant firstSeen,
        Instant lastSeen,
        int eventCount,
        String status,
        String assignee,
        String note,
        String metadata) {
    public static SecurityAlertResponse from(SecurityAlert alert) {
        return new SecurityAlertResponse(
                alert.getPublicId(),
                alert.getRuleCode(),
                alert.getSeverity() == null ? null : alert.getSeverity().name(),
                alert.getTitle(),
                alert.getSubjectType() == null ? null : alert.getSubjectType().name(),
                alert.getSubjectValue(),
                alert.getFirstSeen(),
                alert.getLastSeen(),
                alert.getEventCount(),
                alert.getStatus() == null ? null : alert.getStatus().name(),
                alert.getAssignee(),
                alert.getNote(),
                alert.getMetadata());
    }
}
