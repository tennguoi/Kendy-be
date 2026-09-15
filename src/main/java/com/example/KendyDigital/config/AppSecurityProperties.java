package com.example.KendyDigital.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {
    private List<String> corsAllowedOrigins = new ArrayList<>(List.of(
            "http://127.0.0.1:5173",
            "http://localhost:5173"));

    private List<String> trustedProxies = new ArrayList<>(List.of(
            "127.0.0.1",
            "::1"));

    private boolean enableHttpOnlyCookie = false;

    public List<String> getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(List<String> corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    public List<String> getTrustedProxies() {
        return trustedProxies;
    }

    public void setTrustedProxies(List<String> trustedProxies) {
        this.trustedProxies = trustedProxies;
    }

    public boolean isEnableHttpOnlyCookie() {
        return enableHttpOnlyCookie;
    }

    public void setEnableHttpOnlyCookie(boolean enableHttpOnlyCookie) {
        this.enableHttpOnlyCookie = enableHttpOnlyCookie;
    }
}
