package com.example.KendyDigital.controller;






import com.example.KendyDigital.service.*;
import com.example.KendyDigital.security.*;
import com.example.KendyDigital.repository.*;
import com.example.KendyDigital.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.KendyDigital.config.SePayWebhookProperties;
import com.example.KendyDigital.dto.webhook.request.SePayWebhookPayload;
import com.example.KendyDigital.dto.webhook.response.SePayWebhookResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/webhooks/sepay")
public class SePayWebhookController {
    private final SePayWebhookService sePayWebhookService;
    private final SePayWebhookProperties properties;
    private final ObjectMapper objectMapper;

    public SePayWebhookController(SePayWebhookService sePayWebhookService,
            SePayWebhookProperties properties,
            ObjectMapper objectMapper) {
        this.sePayWebhookService = sePayWebhookService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<SePayWebhookResponse> receive(
            @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestBody String rawPayload) {
        verifyApiKey(headers, queryParams);
        verifyHmac(headers, rawPayload);
        SePayWebhookPayload payload = parsePayload(rawPayload);
        sePayWebhookService.process(payload, rawPayload);
        return ResponseEntity.ok(new SePayWebhookResponse(true));
    }

    private void verifyApiKey(HttpHeaders headers, MultiValueMap<String, String> queryParams) {
        if (!properties.isRequireApiKey()) {
            return;
        }
        String receivedApiKey = resolveWebhookSecret(headers, queryParams);
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()
                || !properties.getApiKey().equals(receivedApiKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid SePay webhook API key");
        }
    }

    private String resolveWebhookSecret(HttpHeaders headers, MultiValueMap<String, String> queryParams) {
        String configuredHeader = normalizeHeaderValue(headers.getFirst(properties.getApiKeyHeader()));
        if (!configuredHeader.isBlank()) {
            return configuredHeader;
        }

        String sepaySecret = normalizeHeaderValue(headers.getFirst("X-SePay-Webhook-Secret"));
        if (!sepaySecret.isBlank()) {
            return sepaySecret;
        }

        String genericSecret = normalizeHeaderValue(headers.getFirst("X-Webhook-Secret"));
        if (!genericSecret.isBlank()) {
            return genericSecret;
        }

        String authorization = normalizeHeaderValue(headers.getFirst(HttpHeaders.AUTHORIZATION));
        if (authorization.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            return authorization.substring("Bearer ".length()).trim();
        }
        if (authorization.regionMatches(true, 0, "Apikey ", 0, "Apikey ".length())) {
            return authorization.substring("Apikey ".length()).trim();
        }

        String querySecret = firstQueryParam(queryParams, "secret", "webhookSecret", "webhook-secret", "apiKey", "token");
        if (!querySecret.isBlank()) {
            return querySecret;
        }

        return "";
    }

    private String firstQueryParam(MultiValueMap<String, String> queryParams, String... keys) {
        if (queryParams == null) {
            return "";
        }
        for (String key : keys) {
            String value = normalizeHeaderValue(queryParams.getFirst(key));
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private void verifyHmac(HttpHeaders headers, String rawPayload) {
        if (!properties.isRequireHmac()) {
            return;
        }
        if (properties.getHmacSecret() == null || properties.getHmacSecret().isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "SePay HMAC secret is not configured");
        }

        String receivedSignature = normalizeSignature(headers.getFirst(properties.getSignatureHeader()));
        if (receivedSignature.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing SePay webhook signature");
        }

        String expectedSignature = hmacSha256(rawPayload, properties.getHmacSecret());
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                receivedSignature.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid SePay webhook signature");
        }
    }

    private SePayWebhookPayload parsePayload(String rawPayload) {
        try {
            return objectMapper.readValue(rawPayload, SePayWebhookPayload.class);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid SePay payload: " + exception.getMessage());
        }
    }

    private String hmacSha256(String rawPayload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot verify SePay HMAC");
        }
    }

    private String normalizeSignature(String signature) {
        if (signature == null) {
            return "";
        }
        String normalized = signature.trim();
        if (normalized.startsWith("sha256=")) {
            normalized = normalized.substring("sha256=".length());
        }
        return normalized.toLowerCase();
    }

    private String normalizeHeaderValue(String value) {
        return value == null ? "" : value.trim();
    }
}
