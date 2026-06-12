package com.example.KendyDigital.dto.auth.response;


import com.example.KendyDigital.model.*;
import java.time.Instant;

public record AuthTokenResponse(
        String accessToken,
        Instant expiresAt,
        AuthUserResponse user) {
}
