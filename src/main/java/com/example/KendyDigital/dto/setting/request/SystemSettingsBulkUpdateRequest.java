package com.example.KendyDigital.dto.setting.request;


import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SystemSettingsBulkUpdateRequest(
        @NotEmpty @Valid List<Item> settings) {
    public record Item(
            @NotBlank @Size(max = 100) String key,
            @NotNull @Size(max = 10000) String value,
            Boolean publicSetting) {
    }
}
