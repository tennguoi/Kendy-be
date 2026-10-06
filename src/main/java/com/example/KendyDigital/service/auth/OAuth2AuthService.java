package com.example.KendyDigital.service.auth;

import com.example.KendyDigital.dto.auth.response.AuthTokenResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

public interface OAuth2AuthService {
    AuthTokenResponse login(String registrationId, Map<String, Object> attributes, String accessToken, String acceptLanguage);

    AuthTokenResponse login(String registrationId, Map<String, Object> attributes, String accessToken, String acceptLanguage, HttpServletResponse response);
}
