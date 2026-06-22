package com.example.KendyDigital.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {
    private boolean enabled = true;
    private int authPerMinute = 10;
    private int webhookPerMinute = 120;
    private int financePerMinute = 60;
    private int depositPerMinute = 5;
    private int renewalPerMinute = 3;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getAuthPerMinute() {
        return authPerMinute;
    }

    public void setAuthPerMinute(int authPerMinute) {
        this.authPerMinute = authPerMinute;
    }

    public int getWebhookPerMinute() {
        return webhookPerMinute;
    }

    public void setWebhookPerMinute(int webhookPerMinute) {
        this.webhookPerMinute = webhookPerMinute;
    }

    public int getFinancePerMinute() {
        return financePerMinute;
    }

    public void setFinancePerMinute(int financePerMinute) {
        this.financePerMinute = financePerMinute;
    }

    public int getDepositPerMinute() {
        return depositPerMinute;
    }

    public void setDepositPerMinute(int depositPerMinute) {
        this.depositPerMinute = depositPerMinute;
    }

    public int getRenewalPerMinute() {
        return renewalPerMinute;
    }

    public void setRenewalPerMinute(int renewalPerMinute) {
        this.renewalPerMinute = renewalPerMinute;
    }
}
