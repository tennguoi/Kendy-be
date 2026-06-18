package com.example.KendyDigital.dto.setting.request;

import java.util.Map;
import jakarta.validation.constraints.NotNull;

public record WebhookConfigRequest(
        @NotNull Map<String, String> config) {
}
