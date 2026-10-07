package com.example.KendyDigital;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.KendyDigital.model.security.AlertStatus;
import com.example.KendyDigital.model.security.SecurityEventType;
import com.example.KendyDigital.model.security.SecuritySeverity;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserStatus;
import com.example.KendyDigital.repository.IpBanRepository;
import com.example.KendyDigital.repository.SecurityAlertRepository;
import com.example.KendyDigital.repository.SecurityEventRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.service.security.monitor.SecuritySignal;
import com.example.KendyDigital.service.security.monitor.SecuritySignalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Simulates real attack scenarios end to end against the running application (random local port):
 * detection, response (ban/alert/freeze) AND enforcement (subsequent HTTP 403). No external host or
 * VPS is required: distinct attacker IPs are simulated with the {@code X-Forwarded-For} header,
 * which is honoured because the test client connects from a trusted loopback proxy.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.security.monitor.enabled=true",
        "app.security.monitor.dry-run=false",
        "app.security.monitor.block-enabled=true",
        "app.security.monitor.thresholds.spray-distinct-emails=3",
        "app.security.monitor.thresholds.scanner-requests=2",
        "app.security.monitor.thresholds.webhook-reject-per-ip=3",
        "app.security.monitor.thresholds.two-factor-fail-per-user=3",
        "app.security.monitor.thresholds.password-ok-2fa-fail=2",
        "app.security.monitor.thresholds.rate-limited-per-ip=3",
        "app.security.monitor.auto-ban-allowlist=198.51.100.42",
        "app.rate-limit.enabled=true",
        "app.rate-limit.auth-per-minute=3"
})
@ActiveProfiles("test")
class SecurityAttackSimulationIntegrationTest {

    private static final String TEST_PASSWORD = "CorrectHorse123!";

    @LocalServerPort
    private int port;

    @Autowired
    private IpBanRepository ipBanRepository;
    @Autowired
    private SecurityAlertRepository securityAlertRepository;
    @Autowired
    private SecurityEventRepository securityEventRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private SecuritySignalService securitySignalService;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @BeforeEach
    void reset() {
        ipBanRepository.deleteAll();
        securityAlertRepository.deleteAll();
    }

    @Test
    @DisplayName("Password spraying: banned and blocked with HTTP 403 afterwards")
    void passwordSprayingIsBanned() throws Exception {
        String attackerIp = "203.0.113.10";
        for (int i = 0; i < 3; i++) {
            String email = createActiveUser("spray-" + UUID.randomUUID() + "@example.com").getEmail();
            postJson("/api/auth/login", attackerIp,
                    "{\"email\":\"" + email + "\",\"password\":\"WrongPassword1!\"}");
        }

        assertTrue(isBanned(attackerIp), "IP should be banned after password spraying");
        assertTrue(hasOpenAlert("AUTH-02"), "Password spraying should raise an AUTH-02 alert");
        assertBlocked(attackerIp);
    }

    @Test
    @DisplayName("Scanner probes: banned and blocked with HTTP 403 afterwards")
    void scannerProbesAreBanned() throws Exception {
        String attackerIp = "203.0.113.20";
        get("/.env", attackerIp);
        get("/wp-admin", attackerIp);

        assertTrue(isBanned(attackerIp), "IP should be banned after repeated scanner probes");
        assertTrue(hasOpenAlert("WAF-BAN"), "Scanner activity should raise a WAF-BAN alert");
        assertTrue(awaitEvent(SecurityEventType.WAF_SCANNER, attackerIp), "Scanner event should be persisted");
        assertBlocked(attackerIp);
    }

    @Test
    @DisplayName("Honeytoken: CRITICAL alert, 24h ban and HTTP 403 afterwards")
    void honeytokenTriggersCriticalResponse() throws Exception {
        String attackerIp = "203.0.113.30";
        get("/api/honeytoken", attackerIp);

        assertTrue(isBanned(attackerIp), "Honeytoken hit should ban the IP");
        assertTrue(hasOpenAlert("HONEY-01"), "Honeytoken hit should raise a CRITICAL alert");
        assertBlocked(attackerIp);
    }

    @Test
    @DisplayName("Forged SePay webhook signatures: banned and blocked with HTTP 403 afterwards")
    void forgedWebhookSignaturesAreBanned() throws Exception {
        String attackerIp = "203.0.113.40";
        for (int i = 0; i < 3; i++) {
            postSePayWebhook(attackerIp);
        }

        assertTrue(isBanned(attackerIp), "IP should be banned after repeated forged webhook signatures");
        assertTrue(hasOpenAlert("BIZ-04"), "Forged webhooks should raise a BIZ-04 alert");
        assertBlocked(attackerIp);
    }

    @Test
    @DisplayName("SQL injection and path traversal are detected (WAF-01 / WAF-03)")
    void webAttackPatternsAreDetected() throws Exception {
        String sqlIp = "203.0.113.50";
        get("/api/services?search=1%27%20UNION%20SELECT", sqlIp);
        assertTrue(awaitEvent(SecurityEventType.WAF_SQLI, sqlIp), "SQL injection should persist a WAF_SQLI event");

        String traversalIp = "203.0.113.51";
        get("/api/files?path=../../etc/passwd", traversalIp);
        assertTrue(awaitEvent(SecurityEventType.WAF_TRAVERSAL, traversalIp),
                "Path traversal should persist a WAF_TRAVERSAL event");
    }

    @Test
    @DisplayName("Reusing a revoked session token is rejected (401) and raises AUTH-11")
    void revokedTokenReuseRaisesAlert() throws Exception {
        String ip = "203.0.113.60";
        UserAccount user = createActiveUser("revoke-" + UUID.randomUUID() + "@example.com");
        String token = login(user.getEmail(), TEST_PASSWORD, ip);

        postWithBearer("/api/auth/logout", ip, token);

        HttpResponse<String> reuse = getWithBearer("/api/me", ip, token);
        int status = reuse.statusCode();
        assertTrue(status == 401 || status == 403, "A revoked token must be rejected (got " + status + ")");
        assertTrue(hasOpenAlert("AUTH-11"), "Reusing a revoked token should raise AUTH-11");
    }

    @Test
    @DisplayName("Brute forcing the 2FA code raises AUTH-05 / AUTH-06")
    void twoFactorBruteForceRaisesAlert() throws Exception {
        String ip = "203.0.113.70";
        UserAccount user = createActiveUser("twofa-" + UUID.randomUUID() + "@example.com");
        user.enableTwoFactor("JBSWY3DPEHPK3PXP", null);
        userAccountRepository.save(user);

        for (int i = 0; i < 3; i++) {
            postJson("/api/auth/login", ip,
                    "{\"email\":\"" + user.getEmail() + "\",\"password\":\"" + TEST_PASSWORD
                            + "\",\"twoFactorCode\":\"000000\"}");
        }

        assertTrue(hasOpenAlert("AUTH-05"), "2FA brute force should raise AUTH-05");
    }

    @Test
    @DisplayName("Account-takeover chain (AUTH-13) auto-freezes the wallet with a CRITICAL alert")
    void accountTakeoverChainFreezesWallet() {
        UserAccount user = createActiveUser("takeover-" + UUID.randomUUID() + "@example.com");

        securitySignalService.record(SecuritySignal
                .of(SecurityEventType.PASSWORD_RESET, SecuritySeverity.HIGH, "203.0.113.80")
                .user(user.getId()).metadata("admin=false").build());
        securitySignalService.record(SecuritySignal
                .of(SecurityEventType.TWO_FACTOR_DISABLED, SecuritySeverity.HIGH, "203.0.113.80")
                .user(user.getId()).metadata("admin=false").build());

        assertTrue(userAccountRepository.findById(user.getId()).orElseThrow().isWalletFrozen(),
                "Wallet should be frozen after the takeover chain");
        assertTrue(hasOpenAlert("AUTH-13"), "Takeover chain should raise a CRITICAL AUTH-13 alert");
    }

    @Test
    @DisplayName("False positives: a normal user with one failed login is never banned")
    void normalUserIsNotFalselyBanned() throws Exception {
        String normalIp = "198.51.100.5";
        postJson("/api/auth/login", normalIp, "{\"email\":\"nobody@example.com\",\"password\":\"whatever\"}");

        assertFalse(isBanned(normalIp), "A single failed login must not ban a normal user");
        assertEquals(200, get("/api/services", normalIp).statusCode(),
                "Normal user must still reach permitted endpoints");
    }

    @Test
    @DisplayName("False positives: an allowlisted (admin) IP is detected but never auto-banned")
    void allowlistedIpIsNeverBanned() throws Exception {
        String allowlistedIp = "198.51.100.42";
        get("/.env", allowlistedIp);
        get("/wp-admin", allowlistedIp);
        get("/phpmyadmin", allowlistedIp);

        assertFalse(isBanned(allowlistedIp), "An allowlisted IP must never be auto-banned");
        assertEquals(200, get("/api/services", allowlistedIp).statusCode(),
                "Allowlisted IP must remain able to reach the app");
    }

    @Test
    @DisplayName("Rate-limit abuse returns HTTP 429 and raises ABUSE-01")
    void rateLimitAbuseIsThrottled() throws Exception {
        String attackerIp = "203.0.113.90";
        int throttled = 0;
        for (int i = 0; i < 8; i++) {
            HttpResponse<String> response = postJson("/api/auth/forgot-password", attackerIp,
                    "{\"email\":\"nobody@example.com\"}");
            if (response.statusCode() == 429) {
                throttled++;
            }
        }

        assertTrue(throttled > 0, "Rate limit must return HTTP 429");
        assertTrue(hasOpenAlert("ABUSE-01"), "Repeated 429s should raise ABUSE-01");
    }

    // --- helpers ------------------------------------------------------------------------------

    private String base() {
        return "http://localhost:" + port;
    }

    private UserAccount createActiveUser(String email) {
        UserAccount user = new UserAccount("Test User", email, null, passwordEncoder.encode(TEST_PASSWORD));
        user.setStatus(UserStatus.ACTIVE);
        return userAccountRepository.save(user);
    }

    private String login(String email, String password, String ip) throws Exception {
        HttpResponse<String> response = postJson("/api/auth/login", ip,
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
        assertEquals(200, response.statusCode(), "Login should succeed: " + response.body());
        return objectMapper.readTree(response.body()).path("accessToken").asText();
    }

    private HttpResponse<String> postJson(String path, String ip, String json) throws Exception {
        return httpClient.send(baseRequest(path, ip)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postWithBearer(String path, String ip, String token) throws Exception {
        return httpClient.send(baseRequest(path, ip)
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path, String ip) throws Exception {
        return httpClient.send(baseRequest(path, ip).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getWithBearer(String path, String ip, String token) throws Exception {
        return httpClient.send(baseRequest(path, ip)
                .header("Authorization", "Bearer " + token)
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private void postSePayWebhook(String ip) throws Exception {
        httpClient.send(baseRequest("/api/webhooks/sepay", ip)
                .header("Content-Type", "application/json")
                .header("X-SePay-Api-Key", "test-api-key")
                .header("X-SePay-Signature", "deadbeef")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder baseRequest(String path, String ip) {
        return HttpRequest.newBuilder(URI.create(base() + path))
                .timeout(Duration.ofSeconds(5))
                .header("X-Forwarded-For", ip);
    }

    private void assertBlocked(String ip) throws Exception {
        assertEquals(403, get("/api/services", ip).statusCode(),
                "A banned IP must be rejected with HTTP 403 on subsequent requests");
    }

    private boolean isBanned(String ip) {
        return !ipBanRepository.findActiveByIpOrCidr(ip, Instant.now()).isEmpty();
    }

    private boolean hasOpenAlert(String ruleCode) {
        return securityAlertRepository.findAllByStatusOrderByLastSeenDesc(AlertStatus.OPEN, PageRequest.of(0, 200))
                .stream().anyMatch(alert -> ruleCode.equals(alert.getRuleCode()));
    }

    private boolean awaitEvent(SecurityEventType type, String ip) {
        for (int attempt = 0; attempt < 20; attempt++) {
            var spec = com.example.KendyDigital.repository.specification.SecurityEventSpecifications.searchEvents(type, null, ip, null, null, null);
            boolean found = !securityEventRepository.findAll(spec, PageRequest.of(0, 10)).isEmpty();
            if (found) {
                return true;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
