package com.example.KendyDigital.dto.setting.request;

import java.util.Map;

public record WebhookConfigRequest(
        Map<String, String> config) {
}
