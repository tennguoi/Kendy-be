package com.example.KendyDigital.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap")
public class BootstrapProperties {
    private boolean seedServices = true;
    private String adminEmail = "";
    private String adminPassword = "";
    private String adminName = "Kendy Admin";

    public boolean isSeedServices() {
        return seedServices;
    }

    public void setSeedServices(boolean seedServices) {
        this.seedServices = seedServices;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }
}
