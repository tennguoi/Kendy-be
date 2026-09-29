package com.example.KendyDigital.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.email")
public class AppEmailProperties {
    private boolean enabled = false;
    private String from = "no-reply@kendydigital.local";
    private String frontendBaseUrl = "http://localhost:5173";

    private String consultRecipient;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getFrontendBaseUrl() {
        return frontendBaseUrl;
    }

    public void setFrontendBaseUrl(String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public String getConsultRecipient() {
        return consultRecipient;
    }

    public void setConsultRecipient(String consultRecipient) {
        this.consultRecipient = consultRecipient;
    }
}
