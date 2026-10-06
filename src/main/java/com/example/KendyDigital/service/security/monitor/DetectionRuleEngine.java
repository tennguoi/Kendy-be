package com.example.KendyDigital.service.security.monitor;

import com.example.KendyDigital.config.SecurityMonitorProperties;
import com.example.KendyDigital.config.SecurityMonitorProperties.Thresholds;
import com.example.KendyDigital.model.auth.AuthSession;
import com.example.KendyDigital.model.security.AlertSubjectType;
import com.example.KendyDigital.model.security.SecurityEventType;
import com.example.KendyDigital.model.security.SecuritySeverity;
import com.example.KendyDigital.repository.AuthSessionRepository;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Converts raw signals into alerts and responses. Thresholds live in
 * {@link SecurityMonitorProperties.Thresholds}; blocking honours the global dry-run flag.
 */
@Service
public class DetectionRuleEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(DetectionRuleEngine.class);
    private static final int ACCOUNT_TAKEOVER_CHAIN_THRESHOLD = 2;

    private final SecurityCounters counters;
    private final RiskScoringService riskScoringService;
    private final AlertService alertService;
    private final IpBanService ipBanService;
    private final AuthSessionRepository authSessionRepository;
    private final SecurityMonitorProperties properties;
    private final WalletFreezeService walletFreezeService;

    public DetectionRuleEngine(SecurityCounters counters, RiskScoringService riskScoringService,
            AlertService alertService, IpBanService ipBanService, AuthSessionRepository authSessionRepository,
            SecurityMonitorProperties properties, WalletFreezeService walletFreezeService) {
        this.counters = counters;
        this.riskScoringService = riskScoringService;
        this.alertService = alertService;
        this.ipBanService = ipBanService;
        this.authSessionRepository = authSessionRepository;
        this.properties = properties;
        this.walletFreezeService = walletFreezeService;
    }

    /** Tiered response driven purely by accumulated risk. */
    public void onRiskChanged(SecuritySignal signal, double ipRisk, double userRisk) {
        String ip = signal.ip();
        if (ip == null || ip.isBlank()) {
            return;
        }
        if (ipRisk >= 150) {
            if (!ipBanService.isBanned(ip)) {
                ipBanService.ban(ip, "risk score " + Math.round(ipRisk), com.example.KendyDigital.model.security.IpBanSource.AUTO_RULE,
                        "RISK-150", null, Duration.ofHours(24));
                alertService.raise("RISK-150", SecuritySeverity.CRITICAL, "IP auto-banned (attack score)",
                        AlertSubjectType.IP, ip, "risk=" + Math.round(ipRisk) + " type=" + signal.type());
            }
        } else if (ipRisk >= 90) {
            if (!ipBanService.isBanned(ip)) {
                ipBanService.ban(ip, "risk score " + Math.round(ipRisk), com.example.KendyDigital.model.security.IpBanSource.AUTO_RULE,
                        "RISK-90", null, Duration.ofHours(1));
                alertService.raise("RISK-90", SecuritySeverity.HIGH, "IP temporarily banned (high risk)",
                        AlertSubjectType.IP, ip, "risk=" + Math.round(ipRisk) + " type=" + signal.type());
            }
        } else if (ipRisk >= 60) {
            counters.set("sec:captcha:" + ip, "1", Duration.ofMinutes(30));
        }
    }

    public void evaluate(SecuritySignal signal) {
        Thresholds t = properties.getThresholds();
        try {
            switch (signal.type()) {
                case LOGIN_FAILED -> evaluateLoginFailed(signal, t);
                case LOGIN_SUCCESS -> evaluateLoginSuccess(signal, t);
                case LOGIN_UNKNOWN_EMAIL -> evaluateUnknownEmail(signal, t);
                case ACCOUNT_LOCKED -> alertService.raise("AUTH-LOCK", SecuritySeverity.LOW, "Account locked after failed logins",
                        AlertSubjectType.USER, subject(signal), detail(signal));
                case TWO_FACTOR_FAILED -> evaluateTwoFactor(signal, t);
                case REVOKED_TOKEN_USED -> evaluateRevokedToken(signal);
                case SESSION_HIJACK_SUSPECTED -> evaluateSessionHijack(signal);
                case IMPOSSIBLE_TRAVEL -> alertService.raise("AUTH-10", SecuritySeverity.MEDIUM, "Impossible travel detected",
                        AlertSubjectType.USER, subject(signal), detail(signal));
                case RATE_LIMITED -> evaluateRateLimited(signal, t);
                case ID_ENUMERATION -> evaluateIdEnumeration(signal, t);
                case WAF_SQLI, WAF_XSS, WAF_TRAVERSAL, WAF_SCANNER, WAF_TOOL_USER_AGENT -> evaluateWaf(signal, t);
                case HONEYTOKEN_HIT -> evaluateHoneytoken(signal);
                case REGISTER_BURST -> evaluateRegisterBurst(signal, t);
                case WEBHOOK_REJECTED -> evaluateWebhookRejected(signal, t);
                case WEBHOOK_IP_NOT_ALLOWED -> alertService.raise("BIZ-06", SecuritySeverity.HIGH,
                        "Webhook from disallowed IP", AlertSubjectType.IP, signal.ip(), detail(signal));
                case PRIVILEGE_CHANGE, TWO_FACTOR_DISABLED, PASSWORD_RESET -> evaluateAccountChange(signal, t);
                case ACCOUNT_TAKEOVER_SUSPECTED -> evaluateAccountTakeover(signal);
                case CREDENTIAL_REVEAL_BURST -> evaluateCredentialReveal(signal, t);
                case COUPON_ABUSE -> evaluateCouponAbuse(signal, t);
                case WARRANTY_ABUSE -> evaluateWarrantyAbuse(signal, t);
                case BALANCE_MISMATCH -> alertService.raise("BIZ-03", SecuritySeverity.CRITICAL,
                        "Wallet balance mismatch detected", AlertSubjectType.SYSTEM, "ledger", detail(signal));
                case AUDIT_INTEGRITY_BROKEN -> alertService.raise("ADM-04", SecuritySeverity.CRITICAL,
                        "Audit log hash chain broken", AlertSubjectType.SYSTEM, "audit_logs", detail(signal));
                case IP_BANNED, IP_UNBANNED -> LOGGER.debug("Ban lifecycle event {}", signal.type());
                default -> LOGGER.trace("No rule for signal {}", signal.type());
            }
        } catch (RuntimeException exception) {
            LOGGER.debug("Rule evaluation failed for {}", signal.type(), exception);
        }
    }

    private void evaluateLoginFailed(SecuritySignal signal, Thresholds t) {
        String ip = signal.ip();
        if (ip != null && !ip.isBlank()) {
            long fails = counters.increment("sec:loginfail:ip:" + ip,
                    Duration.ofMinutes(t.getLoginFailIpWindowMinutes()));
            if (fails == t.getLoginFailPerIp()) {
                alertService.raise("AUTH-01", SecuritySeverity.MEDIUM, "Brute force from IP",
                        AlertSubjectType.IP, ip, "fails=" + fails + " window=" + t.getLoginFailIpWindowMinutes() + "m");
            }
            String emailHash = meta(signal, "emailHash");
            if (emailHash != null) {
                long distinctEmails = counters.distinct("sec:loginfail:ipemails:" + ip, emailHash,
                        Duration.ofMinutes(t.getSprayWindowMinutes()));
                if (distinctEmails >= t.getSprayDistinctEmails()) {
                    ipBanService.ban(ip, "password spraying", com.example.KendyDigital.model.security.IpBanSource.AUTO_RULE,
                            "AUTH-02", null, Duration.ofHours(1));
                    alertService.raise("AUTH-02", SecuritySeverity.HIGH, "Password spraying detected",
                            AlertSubjectType.IP, ip, "distinctEmails=" + distinctEmails);
                }
            }
        }
        String emailHash = meta(signal, "emailHash");
        if (emailHash != null) {
            long distinctIps = counters.distinct("sec:loginfail:email:" + emailHash + ":ips", ip,
                    Duration.ofMinutes(t.getStuffingWindowMinutes()));
            if (distinctIps >= t.getStuffingDistinctIps() && !ipBanService.isAllowlisted(ip)) {
                alertService.raise("AUTH-03", SecuritySeverity.HIGH, "Distributed credential stuffing",
                        AlertSubjectType.USER, subject(signal), "distinctIps=" + distinctIps);
            }
        }
    }

    private void evaluateLoginSuccess(SecuritySignal signal, Thresholds t) {
        String ip = signal.ip();
        if (ip == null || ip.isBlank()) {
            return;
        }
        String raw = counters.get("sec:loginfail:ip:" + ip);
        long fails = parse(raw);
        if (fails >= 5) {
            alertService.raise("AUTH-07", SecuritySeverity.HIGH, "Successful login after repeated failures",
                    AlertSubjectType.IP, ip, "priorFails=" + fails);
        }
    }

    private void evaluateUnknownEmail(SecuritySignal signal, Thresholds t) {
        String ip = signal.ip();
        if (ip == null || ip.isBlank()) {
            return;
        }
        long attempts = counters.increment("sec:unknownemail:ip:" + ip,
                Duration.ofMinutes(t.getUnknownEmailWindowMinutes()));
        if (attempts == t.getUnknownEmailPerIp()) {
            alertService.raise("AUTH-04", SecuritySeverity.MEDIUM, "Account enumeration (unknown emails)",
                    AlertSubjectType.IP, ip, "attempts=" + attempts);
        }
    }

    private void evaluateTwoFactor(SecuritySignal signal, Thresholds t) {
        Long userId = signal.userId();
        if (userId == null) {
            return;
        }
        long fails = counters.increment("sec:2fafail:user:" + userId,
                Duration.ofMinutes(t.getTwoFactorWindowMinutes()));
        if (fails == t.getTwoFactorFailPerUser()) {
            alertService.raise("AUTH-05", SecuritySeverity.HIGH, "2FA brute force",
                    AlertSubjectType.USER, String.valueOf(userId), "fails=" + fails);
        }
        if ("true".equalsIgnoreCase(meta(signal, "passwordOk"))) {
            long passwordOkFails = counters.increment("sec:2fafail:pwok:user:" + userId,
                    Duration.ofMinutes(t.getTwoFactorWindowMinutes()));
            if (passwordOkFails == t.getPasswordOk2faFail()) {
                alertService.raise("AUTH-06", SecuritySeverity.HIGH,
                        "Correct password with repeated invalid 2FA (password may be compromised)",
                        AlertSubjectType.USER, String.valueOf(userId), "fails=" + passwordOkFails);
            }
        }
    }

    private void evaluateRevokedToken(SecuritySignal signal) {
        alertService.raise("AUTH-11", SecuritySeverity.HIGH, "Revoked session token reused",
                AlertSubjectType.USER, subject(signal), detail(signal));
        Long userId = signal.userId();
        if (userId != null) {
            try {
                java.util.List<AuthSession> sessions = authSessionRepository.findAllByUser_IdAndRevokedAtIsNull(userId);
                for (AuthSession session : sessions) {
                    session.revoke();
                }
                authSessionRepository.saveAll(sessions);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private void evaluateSessionHijack(SecuritySignal signal) {
        alertService.raise("AUTH-12", SecuritySeverity.HIGH, "Session IP/user-agent changed",
                AlertSubjectType.USER, subject(signal), detail(signal));
        Long sessionId = signal.sessionId();
        if (sessionId != null) {
            authSessionRepository.findById(sessionId).ifPresent(AuthSession::revoke);
        }
    }

    private void evaluateRateLimited(SecuritySignal signal, Thresholds t) {
        String ip = signal.ip();
        if (ip == null || ip.isBlank()) {
            return;
        }
        long hits = counters.increment("sec:ratelimited:ip:" + ip,
                Duration.ofMinutes(t.getRateLimitedWindowMinutes()));
        if (hits == t.getRateLimitedPerIp()) {
            alertService.raise("ABUSE-01", SecuritySeverity.MEDIUM, "Repeated rate-limit violations",
                    AlertSubjectType.IP, ip, "hits=" + hits + " path=" + signal.path());
        }
    }

    private void evaluateIdEnumeration(SecuritySignal signal, Thresholds t) {
        String ip = signal.ip();
        String key = ip != null && !ip.isBlank() ? ip : subject(signal);
        long hits = counters.increment("sec:idenum:" + key,
                Duration.ofMinutes(t.getIdEnumerationWindowMinutes()));
        if (hits == t.getIdEnumerationPerSubject()) {
            alertService.raise("ABUSE-02", SecuritySeverity.HIGH, "ID enumeration (403/404 probing)",
                    AlertSubjectType.IP, key, "hits=" + hits + " path=" + signal.path());
            if (ip != null && !ip.isBlank()) {
                ipBanService.ban(ip, "id enumeration", com.example.KendyDigital.model.security.IpBanSource.AUTO_RULE,
                        "ABUSE-02", null, Duration.ofHours(1));
            }
        }
    }

    private void evaluateWaf(SecuritySignal signal, Thresholds t) {
        String ip = signal.ip();
        SecuritySeverity severity = signal.severity() == null ? SecuritySeverity.MEDIUM : signal.severity();
        alertService.raise("WAF-" + signal.type().name(), severity, "Attack pattern detected",
                AlertSubjectType.IP, ip, detail(signal));
        if (ip == null || ip.isBlank()) {
            return;
        }
        long hits = counters.increment("sec:waf:ip:" + ip,
                Duration.ofMinutes(t.getScannerWindowMinutes()));
        if (hits >= t.getScannerRequests()) {
            ipBanService.ban(ip, "vulnerability scanning", com.example.KendyDigital.model.security.IpBanSource.AUTO_RULE,
                    "WAF-BAN", null, Duration.ofHours(24));
            alertService.raise("WAF-BAN", SecuritySeverity.HIGH, "IP banned for vulnerability scanning",
                    AlertSubjectType.IP, ip, "hits=" + hits);
        }
    }

    private void evaluateHoneytoken(SecuritySignal signal) {
        alertService.raise("HONEY-01", SecuritySeverity.CRITICAL, "Honeytoken triggered",
                AlertSubjectType.IP, signal.ip(), detail(signal));
        if (signal.ip() != null && !signal.ip().isBlank()) {
            ipBanService.ban(signal.ip(), "honeytoken", com.example.KendyDigital.model.security.IpBanSource.AUTO_RULE,
                    "HONEY-01", null, Duration.ofHours(24));
        }
    }

    private void evaluateRegisterBurst(SecuritySignal signal, Thresholds t) {
        String ip = signal.ip();
        if (ip == null || ip.isBlank()) {
            return;
        }
        long count = counters.increment("sec:register:ip:" + ip,
                Duration.ofMinutes(t.getRegisterBurstWindowMinutes()));
        if (count == t.getRegisterBurstPerIp()) {
            alertService.raise("AUTH-14", SecuritySeverity.MEDIUM, "Registration burst from IP",
                    AlertSubjectType.IP, ip, "count=" + count);
        }
    }

    private void evaluateWebhookRejected(SecuritySignal signal, Thresholds t) {
        String ip = signal.ip();
        if (ip == null || ip.isBlank()) {
            return;
        }
        long count = counters.increment("sec:webhook:ip:" + ip,
                Duration.ofMinutes(t.getWebhookRejectWindowMinutes()));
        if (count >= t.getWebhookRejectPerIp()) {
            ipBanService.ban(ip, "invalid webhook signatures", com.example.KendyDigital.model.security.IpBanSource.AUTO_RULE,
                    "BIZ-04", null, Duration.ofHours(1));
            alertService.raise("BIZ-04", SecuritySeverity.HIGH, "Repeated invalid SePay webhook signatures",
                    AlertSubjectType.IP, ip, "count=" + count + " reason=" + meta(signal, "reason"));
        }
    }

    private void evaluateAccountChange(SecuritySignal signal, Thresholds t) {
        Long userId = signal.userId();
        if (userId == null) {
            return;
        }
        boolean admin = "true".equalsIgnoreCase(meta(signal, "admin"));
        switch (signal.type()) {
            case PRIVILEGE_CHANGE -> {
                if (admin) {
                    alertService.raise("ADM-01", SecuritySeverity.HIGH, "Admin role/permission change",
                            AlertSubjectType.USER, String.valueOf(userId), detail(signal));
                }
            }
            case TWO_FACTOR_DISABLED -> {
                if (admin) {
                    alertService.raise("ADM-02", SecuritySeverity.HIGH, "Admin 2FA disabled/reset",
                            AlertSubjectType.USER, String.valueOf(userId), detail(signal));
                }
            }
            default -> LOGGER.debug("Account change signal {}", signal.type());
        }
        long changes = counters.increment("sec:takeover:user:" + userId,
                Duration.ofMinutes(t.getPrivilegeChangeWindowMinutes()));
        if (changes >= ACCOUNT_TAKEOVER_CHAIN_THRESHOLD) {
            evaluateAccountTakeover(signal);
        }
    }

    private void evaluateAccountTakeover(SecuritySignal signal) {
        Long userId = signal.userId();
        if (userId == null) {
            return;
        }
        alertService.raise("AUTH-13", SecuritySeverity.CRITICAL, "Account takeover chain detected",
                AlertSubjectType.USER, String.valueOf(userId), detail(signal));
        boolean alreadyFrozen = false;
        try {
            alreadyFrozen = walletFreezeService.isFrozen(userId);
        } catch (RuntimeException ignored) {
        }
        if (properties.isBlockEnabled() && !properties.isDryRun() && !alreadyFrozen) {
            try {
                walletFreezeService.freeze(userId, "AUTH-13 account takeover suspected", null);
            } catch (RuntimeException exception) {
                LOGGER.debug("Automatic wallet freeze failed for user {}", userId, exception);
            }
        }
    }

    private void evaluateCouponAbuse(SecuritySignal signal, Thresholds t) {
        String ip = signal.ip();
        Long userId = signal.userId();
        String couponCode = meta(signal, "couponCode");
        if (userId == null || couponCode == null) {
            alertService.raise("BIZ-08", SecuritySeverity.MEDIUM, "Coupon used across multiple accounts",
                    AlertSubjectType.USER, subject(signal), detail(signal));
            return;
        }
        String key = "sec:coupon:" + couponCode + ":ip:" + (ip == null || ip.isBlank() ? "unknown" : ip);
        long distinctUsers = counters.distinct(key, String.valueOf(userId), Duration.ofDays(7));
        if (distinctUsers >= t.getCouponMultiAccount()) {
            alertService.raise("BIZ-08", SecuritySeverity.MEDIUM, "Coupon used across multiple accounts",
                    AlertSubjectType.IP, ip, "coupon=" + couponCode + ";distinctUsers=" + distinctUsers);
        }
    }

    private void evaluateWarrantyAbuse(SecuritySignal signal, Thresholds t) {
        Long userId = signal.userId();
        if (userId == null) {
            alertService.raise("BIZ-09", SecuritySeverity.MEDIUM, "Repeated warranty/refund requests",
                    AlertSubjectType.USER, subject(signal), detail(signal));
            return;
        }
        long count = counters.increment("sec:warranty:user:" + userId,
                Duration.ofDays(t.getWarrantyWindowDays()));
        if (count >= t.getWarrantyRepeat()) {
            alertService.raise("BIZ-09", SecuritySeverity.MEDIUM, "Repeated warranty/refund requests",
                    AlertSubjectType.USER, String.valueOf(userId), "count=" + count);
        }
    }

    private void evaluateCredentialReveal(SecuritySignal signal, Thresholds t) {
        Long userId = signal.userId();
        if (userId == null) {
            return;
        }
        long count = counters.increment("sec:reveal:admin:" + userId,
                Duration.ofMinutes(t.getCredentialRevealWindowMinutes()));
        if (count == t.getCredentialRevealPerAdmin()) {
            alertService.raise("BIZ-01", SecuritySeverity.HIGH, "Excessive credential reveals by admin",
                    AlertSubjectType.USER, String.valueOf(userId), "count=" + count);
        }
    }

    private String subject(SecuritySignal signal) {
        if (signal.userId() != null) {
            return String.valueOf(signal.userId());
        }
        if (signal.ip() != null) {
            return signal.ip();
        }
        return "unknown";
    }

    private String detail(SecuritySignal signal) {
        return (signal.type() + " ip=" + signal.ip() + " path=" + signal.path() + " "
                + (signal.metadata() == null ? "" : signal.metadata())).trim();
    }

    private static String meta(SecuritySignal signal, String key) {
        String metadata = signal.metadata();
        if (metadata == null || metadata.isBlank()) {
            return null;
        }
        for (String part : metadata.split(";")) {
            int idx = part.indexOf('=');
            if (idx > 0 && part.substring(0, idx).trim().equals(key)) {
                return part.substring(idx + 1).trim();
            }
        }
        return null;
    }

    private long parse(String raw) {
        try {
            return raw == null ? 0 : (long) Double.parseDouble(raw);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
