package com.example.KendyDigital.service.auth;

import com.example.KendyDigital.dto.auth.response.AuthTokenResponse;
import java.util.Map;

public interface OAuth2AuthService {
    AuthTokenResponse login(String registrationId, Map<String, Object> attributes, String accessToken);
}
