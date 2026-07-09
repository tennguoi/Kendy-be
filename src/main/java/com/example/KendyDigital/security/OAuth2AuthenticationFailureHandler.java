package com.example.KendyDigital.security;

import com.example.KendyDigital.config.AppOAuth2Properties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {
    private final AppOAuth2Properties properties;

    public OAuth2AuthenticationFailureHandler(AppOAuth2Properties properties) {
        this.properties = properties;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = "OAuth login failed";
        }
        message = message.replaceAll("[\\r\\n\\t]", "_");
        String separator = properties.getFailureRedirectUrl().contains("?") ? "&" : "?";
        response.sendRedirect(properties.getFailureRedirectUrl()
                + separator
                + "oauthError=" + URLEncoder.encode(message, StandardCharsets.UTF_8));
    }
}
