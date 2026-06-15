package com.example.KendyDigital.dto.setting.response;

import com.example.KendyDigital.model.setting.SystemSettingHistory;
import java.time.Instant;

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
