package com.example.KendyDigital.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the security detection and response pipeline ({@code app.security.monitor.*}).
 */
@ConfigurationProperties(prefix = "app.security.monitor")
public class SecurityMonitorProperties {
    private boolean enabled = true;
    private boolean dryRun = true;
    private boolean blockEnabled = true;
    private boolean geoIpEnabled = false;
    private String geoLiteDatabasePath = "";
    private String geoLiteAsnDatabasePath = "";

    private int maxBanHours = 24;
    private int riskDecayPercentPerHour = 50;
    private int persistQueueCapacity = 5000;

    private boolean telegramEnabled = false;
    private String telegramBotToken = "";
    private String telegramChatId = "";

    private boolean emailAlertsEnabled = true;
    private String alertRecipient = "";

    private boolean turnstileEnabled = false;
    private String turnstileSecret = "";
    private String turnstileVerifyUrl = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    private int turnstileThresholdScore = 60;

    private List<String> autoBanAllowlist = new ArrayList<>();
    private List<String> webhookAllowlist = new ArrayList<>();
    private List<String> mobileAsns = new ArrayList<>();

    private final Thresholds thresholds = new Thresholds();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public boolean isBlockEnabled() {
        return blockEnabled;
    }

    public void setBlockEnabled(boolean blockEnabled) {
        this.blockEnabled = blockEnabled;
    }

    public boolean isGeoIpEnabled() {
        return geoIpEnabled;
    }

    public void setGeoIpEnabled(boolean geoIpEnabled) {
        this.geoIpEnabled = geoIpEnabled;
    }

    public String getGeoLiteDatabasePath() {
        return geoLiteDatabasePath;
    }

    public void setGeoLiteDatabasePath(String geoLiteDatabasePath) {
        this.geoLiteDatabasePath = geoLiteDatabasePath;
    }

    public String getGeoLiteAsnDatabasePath() {
        return geoLiteAsnDatabasePath;
    }

    public void setGeoLiteAsnDatabasePath(String geoLiteAsnDatabasePath) {
        this.geoLiteAsnDatabasePath = geoLiteAsnDatabasePath;
    }

    public int getMaxBanHours() {
        return maxBanHours;
    }

    public void setMaxBanHours(int maxBanHours) {
        this.maxBanHours = maxBanHours;
    }

    public int getRiskDecayPercentPerHour() {
        return riskDecayPercentPerHour;
    }

    public void setRiskDecayPercentPerHour(int riskDecayPercentPerHour) {
        this.riskDecayPercentPerHour = riskDecayPercentPerHour;
    }

    public int getPersistQueueCapacity() {
        return persistQueueCapacity;
    }

    public void setPersistQueueCapacity(int persistQueueCapacity) {
        this.persistQueueCapacity = persistQueueCapacity;
    }

    public boolean isTelegramEnabled() {
        return telegramEnabled;
    }

    public void setTelegramEnabled(boolean telegramEnabled) {
        this.telegramEnabled = telegramEnabled;
    }

    public String getTelegramBotToken() {
        return telegramBotToken;
    }

    public void setTelegramBotToken(String telegramBotToken) {
        this.telegramBotToken = telegramBotToken;
    }

    public String getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(String telegramChatId) {
        this.telegramChatId = telegramChatId;
    }

    public boolean isEmailAlertsEnabled() {
        return emailAlertsEnabled;
    }

    public void setEmailAlertsEnabled(boolean emailAlertsEnabled) {
        this.emailAlertsEnabled = emailAlertsEnabled;
    }

    public String getAlertRecipient() {
        return alertRecipient;
    }

    public void setAlertRecipient(String alertRecipient) {
        this.alertRecipient = alertRecipient;
    }

    public boolean isTurnstileEnabled() {
        return turnstileEnabled;
    }

    public void setTurnstileEnabled(boolean turnstileEnabled) {
        this.turnstileEnabled = turnstileEnabled;
    }

    public String getTurnstileSecret() {
        return turnstileSecret;
    }

    public void setTurnstileSecret(String turnstileSecret) {
        this.turnstileSecret = turnstileSecret;
    }

    public String getTurnstileVerifyUrl() {
        return turnstileVerifyUrl;
    }

    public void setTurnstileVerifyUrl(String turnstileVerifyUrl) {
        this.turnstileVerifyUrl = turnstileVerifyUrl;
    }

    public int getTurnstileThresholdScore() {
        return turnstileThresholdScore;
    }

    public void setTurnstileThresholdScore(int turnstileThresholdScore) {
        this.turnstileThresholdScore = turnstileThresholdScore;
    }

    public List<String> getAutoBanAllowlist() {
        return autoBanAllowlist;
    }

    public void setAutoBanAllowlist(List<String> autoBanAllowlist) {
        this.autoBanAllowlist = autoBanAllowlist;
    }

    public List<String> getWebhookAllowlist() {
        return webhookAllowlist;
    }

    public void setWebhookAllowlist(List<String> webhookAllowlist) {
        this.webhookAllowlist = webhookAllowlist;
    }

    public List<String> getMobileAsns() {
        return mobileAsns;
    }

    public void setMobileAsns(List<String> mobileAsns) {
        this.mobileAsns = mobileAsns;
    }

    public Thresholds getThresholds() {
        return thresholds;
    }

    public static class Thresholds {
        private int loginFailPerIp = 20;
        private int loginFailIpWindowMinutes = 10;
        private int sprayDistinctEmails = 10;
        private int sprayWindowMinutes = 15;
        private int stuffingDistinctIps = 5;
        private int stuffingWindowMinutes = 60;
        private int unknownEmailPerIp = 15;
        private int unknownEmailWindowMinutes = 10;
        private int twoFactorFailPerUser = 5;
        private int twoFactorWindowMinutes = 15;
        private int passwordOk2faFail = 3;
        private int rateLimitedPerIp = 5;
        private int rateLimitedWindowMinutes = 10;
        private int idEnumerationPerSubject = 30;
        private int idEnumerationWindowMinutes = 10;
        private int scannerRequests = 3;
        private int scannerWindowMinutes = 5;
        private int webhookRejectPerIp = 5;
        private int webhookRejectWindowMinutes = 5;
        private int registerBurstPerIp = 5;
        private int registerBurstWindowMinutes = 60;
        private int credentialRevealPerAdmin = 20;
        private int credentialRevealWindowMinutes = 60;
        private int privilegeChangeWindowMinutes = 60;
        private int couponMultiAccount = 3;
        private int warrantyRepeat = 3;
        private int warrantyWindowDays = 7;

        public int getLoginFailPerIp() {
            return loginFailPerIp;
        }

        public void setLoginFailPerIp(int loginFailPerIp) {
            this.loginFailPerIp = loginFailPerIp;
        }

        public int getLoginFailIpWindowMinutes() {
            return loginFailIpWindowMinutes;
        }

        public void setLoginFailIpWindowMinutes(int loginFailIpWindowMinutes) {
            this.loginFailIpWindowMinutes = loginFailIpWindowMinutes;
        }

        public int getSprayDistinctEmails() {
            return sprayDistinctEmails;
        }

        public void setSprayDistinctEmails(int sprayDistinctEmails) {
            this.sprayDistinctEmails = sprayDistinctEmails;
        }

        public int getSprayWindowMinutes() {
            return sprayWindowMinutes;
        }

        public void setSprayWindowMinutes(int sprayWindowMinutes) {
            this.sprayWindowMinutes = sprayWindowMinutes;
        }

        public int getStuffingDistinctIps() {
            return stuffingDistinctIps;
        }

        public void setStuffingDistinctIps(int stuffingDistinctIps) {
            this.stuffingDistinctIps = stuffingDistinctIps;
        }

        public int getStuffingWindowMinutes() {
            return stuffingWindowMinutes;
        }

        public void setStuffingWindowMinutes(int stuffingWindowMinutes) {
            this.stuffingWindowMinutes = stuffingWindowMinutes;
        }

        public int getUnknownEmailPerIp() {
            return unknownEmailPerIp;
        }

        public void setUnknownEmailPerIp(int unknownEmailPerIp) {
            this.unknownEmailPerIp = unknownEmailPerIp;
        }

        public int getUnknownEmailWindowMinutes() {
            return unknownEmailWindowMinutes;
        }

        public void setUnknownEmailWindowMinutes(int unknownEmailWindowMinutes) {
            this.unknownEmailWindowMinutes = unknownEmailWindowMinutes;
        }

        public int getTwoFactorFailPerUser() {
            return twoFactorFailPerUser;
        }

        public void setTwoFactorFailPerUser(int twoFactorFailPerUser) {
            this.twoFactorFailPerUser = twoFactorFailPerUser;
        }

        public int getTwoFactorWindowMinutes() {
            return twoFactorWindowMinutes;
        }

        public void setTwoFactorWindowMinutes(int twoFactorWindowMinutes) {
            this.twoFactorWindowMinutes = twoFactorWindowMinutes;
        }

        public int getPasswordOk2faFail() {
            return passwordOk2faFail;
        }

        public void setPasswordOk2faFail(int passwordOk2faFail) {
            this.passwordOk2faFail = passwordOk2faFail;
        }

        public int getRateLimitedPerIp() {
            return rateLimitedPerIp;
        }

        public void setRateLimitedPerIp(int rateLimitedPerIp) {
            this.rateLimitedPerIp = rateLimitedPerIp;
        }

        public int getRateLimitedWindowMinutes() {
            return rateLimitedWindowMinutes;
        }

        public void setRateLimitedWindowMinutes(int rateLimitedWindowMinutes) {
            this.rateLimitedWindowMinutes = rateLimitedWindowMinutes;
        }

        public int getIdEnumerationPerSubject() {
            return idEnumerationPerSubject;
        }

        public void setIdEnumerationPerSubject(int idEnumerationPerSubject) {
            this.idEnumerationPerSubject = idEnumerationPerSubject;
        }

        public int getIdEnumerationWindowMinutes() {
            return idEnumerationWindowMinutes;
        }

        public void setIdEnumerationWindowMinutes(int idEnumerationWindowMinutes) {
            this.idEnumerationWindowMinutes = idEnumerationWindowMinutes;
        }

        public int getScannerRequests() {
            return scannerRequests;
        }

        public void setScannerRequests(int scannerRequests) {
            this.scannerRequests = scannerRequests;
        }

        public int getScannerWindowMinutes() {
            return scannerWindowMinutes;
        }

        public void setScannerWindowMinutes(int scannerWindowMinutes) {
            this.scannerWindowMinutes = scannerWindowMinutes;
        }

        public int getWebhookRejectPerIp() {
            return webhookRejectPerIp;
        }

        public void setWebhookRejectPerIp(int webhookRejectPerIp) {
            this.webhookRejectPerIp = webhookRejectPerIp;
        }

        public int getWebhookRejectWindowMinutes() {
            return webhookRejectWindowMinutes;
        }

        public void setWebhookRejectWindowMinutes(int webhookRejectWindowMinutes) {
            this.webhookRejectWindowMinutes = webhookRejectWindowMinutes;
        }

        public int getRegisterBurstPerIp() {
            return registerBurstPerIp;
        }

        public void setRegisterBurstPerIp(int registerBurstPerIp) {
            this.registerBurstPerIp = registerBurstPerIp;
        }

        public int getRegisterBurstWindowMinutes() {
            return registerBurstWindowMinutes;
        }

        public void setRegisterBurstWindowMinutes(int registerBurstWindowMinutes) {
            this.registerBurstWindowMinutes = registerBurstWindowMinutes;
        }

        public int getCredentialRevealPerAdmin() {
            return credentialRevealPerAdmin;
        }

        public void setCredentialRevealPerAdmin(int credentialRevealPerAdmin) {
            this.credentialRevealPerAdmin = credentialRevealPerAdmin;
        }

        public int getCredentialRevealWindowMinutes() {
            return credentialRevealWindowMinutes;
        }

        public void setCredentialRevealWindowMinutes(int credentialRevealWindowMinutes) {
            this.credentialRevealWindowMinutes = credentialRevealWindowMinutes;
        }

        public int getPrivilegeChangeWindowMinutes() {
            return privilegeChangeWindowMinutes;
        }

        public void setPrivilegeChangeWindowMinutes(int privilegeChangeWindowMinutes) {
            this.privilegeChangeWindowMinutes = privilegeChangeWindowMinutes;
        }

        public int getCouponMultiAccount() {
            return couponMultiAccount;
        }

        public void setCouponMultiAccount(int couponMultiAccount) {
            this.couponMultiAccount = couponMultiAccount;
        }

        public int getWarrantyRepeat() {
            return warrantyRepeat;
        }

        public void setWarrantyRepeat(int warrantyRepeat) {
            this.warrantyRepeat = warrantyRepeat;
        }

        public int getWarrantyWindowDays() {
            return warrantyWindowDays;
        }

        public void setWarrantyWindowDays(int warrantyWindowDays) {
            this.warrantyWindowDays = warrantyWindowDays;
        }
    }
}
