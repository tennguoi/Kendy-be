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

    private boolean enableHttpOnlyCookie = true;

    private String cookieSameSite = "Lax";

    private Boolean cookieSecure = null;

    private boolean trustCloudflare = false;

    private List<String> ipAllowlist = new ArrayList<>();

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

    public String getCookieSameSite() {
        return cookieSameSite;
    }

    public void setCookieSameSite(String cookieSameSite) {
        this.cookieSameSite = cookieSameSite;
    }

    public Boolean getCookieSecure() {
        return cookieSecure;
    }

    public void setCookieSecure(Boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    public boolean isTrustCloudflare() {
        return trustCloudflare;
    }

    public void setTrustCloudflare(boolean trustCloudflare) {
        this.trustCloudflare = trustCloudflare;
    }

    public List<String> getIpAllowlist() {
        return ipAllowlist;
    }

    public void setIpAllowlist(List<String> ipAllowlist) {
        this.ipAllowlist = ipAllowlist;
    }
}
