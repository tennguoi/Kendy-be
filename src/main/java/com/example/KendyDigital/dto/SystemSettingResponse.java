package com.example.KendyDigital.dto;

import java.time.Instant;

import com.example.KendyDigital.model.SystemSetting;

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
