package com.example.KendyDigital.dto;

import jakarta.validation.constraints.NotNull;

public record SystemSettingUpdateRequest(
        @NotNull String value,
        Boolean publicSetting) {
}
