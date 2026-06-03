package com.example.KendyDigital.dto;

import java.util.Map;

public record WebhookConfigRequest(
        Map<String, String> config) {
}
