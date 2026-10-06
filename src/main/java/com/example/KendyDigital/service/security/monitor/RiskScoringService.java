package com.example.KendyDigital.service.security.monitor;

import com.example.KendyDigital.config.SecurityMonitorProperties;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * Per-IP and per-user risk score. Scores accumulate as signals arrive and decay exponentially
 * (half-life configured by {@code app.security.monitor.risk-decay-percent-per-hour}).
 */
@Service
public class RiskScoringService {
    private static final Duration RISK_TTL = Duration.ofHours(24);

    private final SecurityCounters counters;
    private final SecurityMonitorProperties properties;

    public RiskScoringService(SecurityCounters counters, SecurityMonitorProperties properties) {
        this.counters = counters;
        this.properties = properties;
    }

    public double addIpRisk(String ip, double points) {
        if (ip == null || ip.isBlank() || points == 0) {
            return ip == null || ip.isBlank() ? 0 : ipRisk(ip);
        }
        return add("sec:risk:ip:" + ip, points);
    }

    public double addUserRisk(Long userId, double points) {
        if (userId == null || points == 0) {
            return 0;
        }
        return add("sec:risk:user:" + userId, points);
    }

    public double ipRisk(String ip) {
        if (ip == null || ip.isBlank()) {
            return 0;
        }
        return decayed("sec:risk:ip:" + ip);
    }

    public double userRisk(Long userId) {
        if (userId == null) {
            return 0;
        }
        return decayed("sec:risk:user:" + userId);
    }

    public void resetIp(String ip) {
        if (ip != null) {
            counters.delete("sec:risk:ip:" + ip);
        }
    }

    public void resetUser(Long userId) {
        if (userId != null) {
            counters.delete("sec:risk:user:" + userId);
        }
    }

    private double add(String key, double points) {
        double current = decayed(key);
        double next = Math.min(1000, current + points);
        counters.set(key, next + "|" + Instant.now().getEpochSecond(), RISK_TTL);
        return next;
    }

    private double decayed(String key) {
        String raw = counters.get(key);
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        double score;
        long updated;
        int separator = raw.indexOf('|');
        try {
            if (separator < 0) {
                score = Double.parseDouble(raw);
                updated = Instant.now().getEpochSecond();
            } else {
                score = Double.parseDouble(raw.substring(0, separator));
                updated = Long.parseLong(raw.substring(separator + 1));
            }
        } catch (NumberFormatException exception) {
            return 0;
        }
        long elapsedHours = Math.max(0, (Instant.now().getEpochSecond() - updated) / 3600);
        if (elapsedHours == 0) {
            return score;
        }
        double retention = Math.max(0.1, Math.min(1.0, properties.getRiskDecayPercentPerHour() / 100.0));
        double result = score * Math.pow(retention, elapsedHours);
        return result < 1 ? 0 : result;
    }
}
