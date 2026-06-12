package com.example.KendyDigital.dto.user.response;

public record UserApiKeyCreatedResponse(
        UserApiKeyResponse apiKey,
        String token) {
}
