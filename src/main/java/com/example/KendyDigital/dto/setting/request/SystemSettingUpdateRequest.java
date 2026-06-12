package com.example.KendyDigital.dto.setting.request;

import jakarta.validation.constraints.NotNull;

public record SystemSettingUpdateRequest(
        @NotNull String value,
        Boolean publicSetting) {
}
