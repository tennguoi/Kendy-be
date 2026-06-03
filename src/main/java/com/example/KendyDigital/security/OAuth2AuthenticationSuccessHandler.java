package com.example.KendyDigital.security;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.example.KendyDigital.config.AppOAuth2Properties;
import com.example.KendyDigital.dto.AuthTokenResponse;
import com.example.KendyDigital.service.OAuth2AuthService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final OAuth2AuthService oAuth2AuthService;
    private final AppOAuth2Properties properties;
    private final ObjectProvider<OAuth2AuthorizedClientService> authorizedClientServiceProvider;

    public OAuth2AuthenticationSuccessHandler(OAuth2AuthService oAuth2AuthService,
            AppOAuth2Properties properties,
            ObjectProvider<OAuth2AuthorizedClientService> authorizedClientServiceProvider) {
        this.oAuth2AuthService = oAuth2AuthService;
        this.properties = properties;
        this.authorizedClientServiceProvider = authorizedClientServiceProvider;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken token)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "OAuth2 authentication required");
            return;
        }
        AuthTokenResponse authToken = oAuth2AuthService.login(
                token.getAuthorizedClientRegistrationId(),
                token.getPrincipal().getAttributes(),
                accessToken(token));
        response.sendRedirect(successUrl(authToken));
    }

    private String accessToken(OAuth2AuthenticationToken token) {
        OAuth2AuthorizedClientService authorizedClientService = authorizedClientServiceProvider.getIfAvailable();
        if (authorizedClientService == null) {
            return null;
        }
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                token.getAuthorizedClientRegistrationId(),
                token.getName());
        return client == null || client.getAccessToken() == null ? null : client.getAccessToken().getTokenValue();
    }

    private String successUrl(AuthTokenResponse token) {
        String separator = properties.getSuccessRedirectUrl().contains("?") ? "&" : "?";
        return properties.getSuccessRedirectUrl()
                + separator
                + "token=" + encode(token.accessToken())
                + "&expiresAt=" + encode(token.expiresAt().toString());
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
