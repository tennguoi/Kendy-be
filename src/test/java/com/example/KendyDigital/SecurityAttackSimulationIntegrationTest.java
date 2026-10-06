package com.example.KendyDigital;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.KendyDigital.model.security.AlertStatus;
import com.example.KendyDigital.model.security.SecurityEventType;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.repository.IpBanRepository;
import com.example.KendyDigital.repository.SecurityAlertRepository;
import com.example.KendyDigital.repository.SecurityEventRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
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
 * Simulates real attack scenarios against the running application (random local port) and asserts
 * the detection/response pipeline reacts. No external host or VPS required: distinct attacker IPs
 * are simulated with the {@code X-Forwarded-For} header, which is honoured because the test client
 * connects from a trusted loopback proxy.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.security.monitor.enabled=true",
        "app.security.monitor.dry-run=false",
        "app.security.monitor.block-enabled=true",
        "app.security.monitor.thresholds.spray-distinct-emails=3",
        "app.security.monitor.thresholds.scanner-requests=2",
        "app.security.monitor.thresholds.webhook-reject-per-ip=3"
})
@ActiveProfiles("test")
class SecurityAttackSimulationIntegrationTest {

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

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @BeforeEach
    void reset() {
        ipBanRepository.deleteAll();
        securityAlertRepository.deleteAll();
    }

    @Test
    @DisplayName("Password spraying from one IP triggers an automatic ban and HIGH alert")
    void passwordSprayingIsBanned() throws Exception {
        String attackerIp = "203.0.113.10";
        String password = "CorrectHorse123!";
        for (int i = 0; i < 3; i++) {
            String email = "spray-" + UUID.randomUUID() + "@example.com";
            userAccountRepository.save(new UserAccount("Spray " + i, email, null,
                    passwordEncoder.encode(password)));
            postJson("/api/auth/login", attackerIp,
                    "{\"email\":\"" + email + "\",\"password\":\"WrongPassword1!\"}");
        }

        assertTrue(isBanned(attackerIp), "IP should be banned after password spraying");
        assertTrue(hasOpenAlert("AUTH-02"), "Password spraying should raise an AUTH-02 alert");
    }

    @Test
    @DisplayName("Vulnerability scanner probes trigger a WAF ban")
    void scannerProbesAreBanned() throws Exception {
        String attackerIp = "203.0.113.20";
        get("/.env", attackerIp);
        get("/wp-admin", attackerIp);

        assertTrue(isBanned(attackerIp), "IP should be banned after repeated scanner probes");
        assertTrue(hasOpenAlert("WAF-BAN"), "Scanner activity should raise a WAF-BAN alert");
        assertTrue(awaitEvent(SecurityEventType.WAF_SCANNER, attackerIp), "Scanner event should be persisted");
    }

    @Test
    @DisplayName("Honeytoken hit triggers a CRITICAL alert and a 24h ban")
    void honeytokenTriggersCriticalResponse() throws Exception {
        String attackerIp = "203.0.113.30";
        get("/api/honeytoken", attackerIp);

        assertTrue(isBanned(attackerIp), "Honeytoken hit should ban the IP");
        assertTrue(hasOpenAlert("HONEY-01"), "Honeytoken hit should raise a CRITICAL alert");
    }

    @Test
    @DisplayName("Forged SePay webhook signatures trigger a ban")
    void forgedWebhookSignaturesAreBanned() throws Exception {
        String attackerIp = "203.0.113.40";
        for (int i = 0; i < 3; i++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(base() + "/api/webhooks/sepay"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .header("X-Forwarded-For", attackerIp)
                    .header("X-SePay-Api-Key", "test-api-key")
                    .header("X-SePay-Signature", "deadbeef")
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        }

        assertTrue(isBanned(attackerIp), "IP should be banned after repeated forged webhook signatures");
        assertTrue(hasOpenAlert("BIZ-04"), "Forged webhooks should raise a BIZ-04 alert");
    }

    private String base() {
        return "http://localhost:" + port;
    }

    private HttpResponse<String> postJson(String path, String ip, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base() + path))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", ip)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path, String ip) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base() + path))
                .timeout(Duration.ofSeconds(5))
                .header("X-Forwarded-For", ip)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
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
            boolean found = !securityEventRepository
                    .searchSecurityEvents(type.name(), null, ip, null, null, null, PageRequest.of(0, 10)).isEmpty();
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
