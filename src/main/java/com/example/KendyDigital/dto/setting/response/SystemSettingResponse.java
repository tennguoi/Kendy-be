package com.example.KendyDigital.dto.setting.response;

import com.example.KendyDigital.model.setting.SystemSetting;
import java.time.Instant;

public record SystemSettingResponse(
        String key,
        String value,
        boolean publicSetting,
        Long updatedBy,
        Instant updatedAt) {
    public static SystemSettingResponse from(SystemSetting setting) {
        return new SystemSettingResponse(
                setting.getKey(),
                setting.getValue(),
                setting.isPublicSetting(),
                setting.getUpdatedBy(),
                setting.getUpdatedAt());
    }
}
