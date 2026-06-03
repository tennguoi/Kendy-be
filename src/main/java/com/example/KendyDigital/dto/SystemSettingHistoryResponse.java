package com.example.KendyDigital.dto;

import java.time.Instant;

import com.example.KendyDigital.model.SystemSettingHistory;

public record SystemSettingHistoryResponse(
        Long id,
        String key,
        String oldValue,
        String newValue,
        Long changedBy,
        Instant createdAt) {
    public static SystemSettingHistoryResponse from(SystemSettingHistory history) {
        return new SystemSettingHistoryResponse(
                history.getId(),
                history.getKey(),
                history.getOldValue(),
                history.getNewValue(),
                history.getChangedBy(),
                history.getCreatedAt());
    }
}
