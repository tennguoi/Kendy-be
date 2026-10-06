package com.example.KendyDigital.service.security.monitor;

import com.example.KendyDigital.config.SecurityMonitorProperties;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Optional step-up CAPTCHA (Cloudflare Turnstile). When disabled or when the caller's risk is
 * below the threshold, verification is a no-op. Enforced at the authentication entry points.
 */
@Service
public class CaptchaService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaptchaService.class);

    private final SecurityMonitorProperties properties;
    private final RiskScoringService riskScoringService;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    public CaptchaService(SecurityMonitorProperties properties, RiskScoringService riskScoringService) {
        this.properties = properties;
        this.riskScoringService = riskScoringService;
    }

    public boolean isRequired(String ip) {
        return properties.isTurnstileEnabled() && !properties.getTurnstileSecret().isBlank()
                && riskScoringService.ipRisk(ip) >= properties.getTurnstileThresholdScore();
    }

    public boolean verify(String token, String ip) {
        if (!properties.isTurnstileEnabled() || properties.getTurnstileSecret().isBlank()) {
            return true;
        }
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            String body = "secret=" + URLEncoder.encode(properties.getTurnstileSecret(), StandardCharsets.UTF_8)
                    + "&response=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                    + (ip == null ? "" : "&remoteip=" + URLEncoder.encode(ip, StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getTurnstileVerifyUrl()))
                    .timeout(Duration.ofSeconds(6))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 && response.body() != null
                    && response.body().contains("\"success\":true");
        } catch (RuntimeException exception) {
            LOGGER.warn("Turnstile verification failed", exception);
            return false;
        } catch (Exception exception) {
            LOGGER.warn("Turnstile verification request failed", exception);
            return false;
        }
    }
}
