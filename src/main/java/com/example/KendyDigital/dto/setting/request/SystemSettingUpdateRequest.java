package com.example.KendyDigital.dto.setting.request;


import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

public record SystemSettingUpdateRequest(
        @NotNull @Size(max = 10000) String value,
        Boolean publicSetting) {
}
