package com.example.KendyDigital.dto.setting.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SystemSettingsBulkUpdateRequest(
        @NotEmpty @Valid List<Item> settings) {
    public record Item(
            @NotBlank String key,
            @NotNull String value,
            Boolean publicSetting) {
    }
}
