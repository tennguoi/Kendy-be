package com.example.KendyDigital.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oauth2")
public class AppOAuth2Properties {
    private String successRedirectUrl = "http://localhost:5173/oauth/callback";
    private String failureRedirectUrl = "http://localhost:5173/login";
    private final Client google = new Client();
    private final Client github = new Client();

    public String getSuccessRedirectUrl() {
        return successRedirectUrl;
    }

    public void setSuccessRedirectUrl(String successRedirectUrl) {
        this.successRedirectUrl = successRedirectUrl;
    }

    public String getFailureRedirectUrl() {
        return failureRedirectUrl;
    }

    public void setFailureRedirectUrl(String failureRedirectUrl) {
        this.failureRedirectUrl = failureRedirectUrl;
    }

    public Client getGoogle() {
        return google;
    }

    public Client getGithub() {
        return github;
    }

    public static class Client {
        private String clientId = "";
        private String clientSecret = "";

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }
    }
}
