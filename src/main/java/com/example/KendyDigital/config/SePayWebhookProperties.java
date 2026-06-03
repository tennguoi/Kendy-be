package com.example.KendyDigital.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sepay.webhook")
public class SePayWebhookProperties {
    private boolean requireApiKey = true;
    private String apiKey = "";
    private String apiKeyHeader = "X-SePay-Api-Key";
    private boolean requireHmac = true;
    private String hmacSecret = "";
    private String signatureHeader = "X-SePay-Signature";

    public boolean isRequireApiKey() {
        return requireApiKey;
    }

    public void setRequireApiKey(boolean requireApiKey) {
        this.requireApiKey = requireApiKey;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKeyHeader() {
        return apiKeyHeader;
    }

    public void setApiKeyHeader(String apiKeyHeader) {
        this.apiKeyHeader = apiKeyHeader;
    }

    public boolean isRequireHmac() {
        return requireHmac;
    }

    public void setRequireHmac(boolean requireHmac) {
        this.requireHmac = requireHmac;
    }

    public String getHmacSecret() {
        return hmacSecret;
    }

    public void setHmacSecret(String hmacSecret) {
        this.hmacSecret = hmacSecret;
    }

    public String getSignatureHeader() {
        return signatureHeader;
    }

    public void setSignatureHeader(String signatureHeader) {
        this.signatureHeader = signatureHeader;
    }
}
