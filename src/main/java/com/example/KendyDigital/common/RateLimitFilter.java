package com.example.KendyDigital.common;

import com.example.KendyDigital.config.RateLimitProperties;
import com.example.KendyDigital.repository.SystemSettingRepository;
import com.example.KendyDigital.security.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final long WINDOW_SECONDS = 60;

    private final RateLimitProperties properties;
    private final SystemSettingRepository systemSettingRepository;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties properties, SystemSettingRepository systemSettingRepository) {
        this.properties = properties;
        this.systemSettingRepository = systemSettingRepository;
    }

    @Scheduled(fixedDelay = 60000)
    public void cleanExpired() {
        long now = Instant.now().getEpochSecond();
        counters.entrySet().removeIf(entry -> now - entry.getValue().windowStartedAt() >= WINDOW_SECONDS);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        int limit = limitFor(request);
        if (!isEnabled() || limit <= 0) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = rateLimitSubject(request) + ":" + request.getMethod() + ":" + request.getRequestURI();
        WindowCounter counter = counters.compute(key, (ignored, existing) -> {
            long now = Instant.now().getEpochSecond();
            if (existing == null || now - existing.windowStartedAt() >= WINDOW_SECONDS) {
                return new WindowCounter(now, new AtomicInteger(1));
            }
            existing.count().incrementAndGet();
            return existing;
        });

        if (counter.count().get() > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Too many requests\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private int limitFor(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if ("POST".equals(method) && ("/api/auth/login".equals(path) || "/api/auth/register".equals(path)
                || "/api/auth/forgot-password".equals(path) || "/api/auth/reset-password".equals(path)
                || "/api/auth/resend-verification".equals(path))) {
            return configuredLimit("rate_limit.auth_per_minute", properties.getAuthPerMinute());
        }
        if ("POST".equals(method) && "/api/webhooks/sepay".equals(path)) {
            return configuredLimit("rate_limit.webhook_per_minute", properties.getWebhookPerMinute());
        }
        if ("POST".equals(method) && "/api/deposits".equals(path)) {
            return configuredLimit("rate_limit.deposit_per_minute", properties.getDepositPerMinute());
        }
        if ("POST".equals(method) && ("/api/orders".equals(path)
                || "/api/auth/2fa/email-code".equals(path))) {
            return configuredLimit("rate_limit.finance_per_minute", properties.getFinancePerMinute());
        }
        if (path.startsWith("/api/tickets") || path.startsWith("/api/warranty-requests")) {
            return configuredLimit("rate_limit.auth_per_minute", properties.getAuthPerMinute());
        }
        if ("POST".equals(method) && path.matches("/api/me/entitlements/\\d+/renew")) {
            return configuredLimit("rate_limit.renewal_per_minute", properties.getRenewalPerMinute());
        }
        if ("POST".equals(method) && path.startsWith("/api/admin/credentials/") && path.endsWith("/reveal")) {
            return configuredLimit("rate_limit.finance_per_minute", properties.getFinancePerMinute());
        }
        return 0;
    }

    private boolean isEnabled() {
        return systemSettingRepository.findById("rate_limit.enabled")
                .map(setting -> Boolean.parseBoolean(setting.getValue()))
                .orElse(properties.isEnabled());
    }

    private int configuredLimit(String key, int fallback) {
        return systemSettingRepository.findById(key)
                .map(setting -> parsePositiveInt(setting.getValue(), fallback))
                .orElse(fallback);
    }

    private int parsePositiveInt(String value, int fallback) {
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (isLoopback(request.getRemoteAddr()) && forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isLoopback(String address) {
        return "127.0.0.1".equals(address) || "0:0:0:0:0:0:0:1".equals(address) || "::1".equals(address);
    }

    private String rateLimitSubject(HttpServletRequest request) {
        if ("POST".equals(request.getMethod())
                && ("/api/deposits".equals(request.getRequestURI())
                    || request.getRequestURI().matches("/api/me/entitlements/\\d+/renew"))) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
                return "user:" + user.userId();
            }
        }
        return "ip:" + clientIp(request);
    }

    private record WindowCounter(long windowStartedAt, AtomicInteger count) {
    }
}
