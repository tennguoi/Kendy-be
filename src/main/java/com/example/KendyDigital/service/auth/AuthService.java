package com.example.KendyDigital.service.auth;

import com.example.KendyDigital.dto.auth.request.AuthLoginRequest;
import com.example.KendyDigital.dto.auth.request.AuthRegisterRequest;
import com.example.KendyDigital.dto.auth.request.AuthTwoFactorEmailRequest;
import com.example.KendyDigital.dto.auth.response.AuthTokenResponse;
import com.example.KendyDigital.dto.auth.response.AuthUserResponse;
import com.example.KendyDigital.dto.auth.response.SecurityTokenResponse;

public interface AuthService {
    AuthUserResponse register(AuthRegisterRequest request, String acceptLanguage);
    AuthTokenResponse login(AuthLoginRequest request, String acceptLanguage);
    SecurityTokenResponse sendLoginTwoFactorEmailCode(AuthTwoFactorEmailRequest request);
    void logout(String token);
}
