package com.example.KendyDigital.dto;


import com.example.KendyDigital.model.*;
import java.time.Instant;

public record AuthTokenResponse(
        String accessToken,
        Instant expiresAt,
        AuthUserResponse user) {
}
