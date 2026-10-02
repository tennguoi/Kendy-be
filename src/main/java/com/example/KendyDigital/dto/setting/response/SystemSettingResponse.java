package com.example.KendyDigital.dto.setting.response;

import com.example.KendyDigital.model.setting.SystemSetting;
import java.time.Instant;

public record SystemSettingResponse(
        String key,
        String value,
        boolean publicSetting,
        Long updatedBy,
        Instant updatedAt,
        Long version) {

    public SystemSettingResponse(String key, String value, boolean publicSetting, Long updatedBy, Instant updatedAt) {
        this(key, value, publicSetting, updatedBy, updatedAt, 0L);
    }

    public static SystemSettingResponse from(SystemSetting setting) {
        return new SystemSettingResponse(
                setting.getKey(),
                setting.getValue(),
                setting.isPublicSetting(),
                setting.getUpdatedBy(),
                setting.getUpdatedAt(),
                setting.getVersion());
    }
}

