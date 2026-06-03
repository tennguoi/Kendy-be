package com.example.KendyDigital.dto;

public record UserApiKeyCreatedResponse(
        UserApiKeyResponse apiKey,
        String token) {
}
