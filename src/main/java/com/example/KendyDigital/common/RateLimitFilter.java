package com.example.KendyDigital.common;

import com.example.KendyDigital.common.error.ApiError;
import com.example.KendyDigital.common.error.ErrorCode;
import com.example.KendyDigital.config.RateLimitProperties;
import com.example.KendyDigital.repository.SystemSettingRepository;
import com.example.KendyDigital.security.AuthenticatedUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.Duration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final long WINDOW_SECONDS = 60;
    private static final int MAX_COUNTER_ENTRIES = 10_000;

    private final RateLimitProperties properties;
    private final SystemSettingRepository systemSettingRepository;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final MessageSource messageSource;
    private final ObjectMapper objectMapper;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final Map<String, CachedSetting> settingCache = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties properties, SystemSettingRepository systemSettingRepository,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            MessageSource messageSource, ObjectMapper objectMapper) {
        this.properties = properties;
        this.systemSettingRepository = systemSettingRepository;
        this.redisTemplateProvider = redisTemplateProvider;
        this.messageSource = messageSource;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 30000)
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

        String key = "rate-limit:" + rateLimitSubject(request) + ":" + request.getMethod() + ":" + request.getRequestURI();
        Integer distributedCount = incrementDistributed(key);
        if (distributedCount != null) {
            if (distributedCount > limit) {
                reject(request, response);
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        if (counters.size() >= MAX_COUNTER_ENTRIES) {
            long now = Instant.now().getEpochSecond();
            counters.entrySet().removeIf(entry -> now - entry.getValue().windowStartedAt() >= WINDOW_SECONDS);
            if (counters.size() >= MAX_COUNTER_ENTRIES) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        WindowCounter counter = counters.compute(key, (ignored, existing) -> {
            long now = Instant.now().getEpochSecond();
            if (existing == null || now - existing.windowStartedAt() >= WINDOW_SECONDS) {
                return new WindowCounter(now, new AtomicInteger(1));
            }
            existing.count().incrementAndGet();
            return existing;
        });

        if (counter.count().get() > limit) {
            reject(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private int limitFor(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if ("POST".equals(method) && ("/api/auth/login".equals(path) || "/api/auth/register".equals(path)
                || "/api/auth/forgot-password".equals(path) || "/api/auth/verify-password-reset".equals(path)
                || "/api/auth/reset-password".equals(path)
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
        return Boolean.parseBoolean(settingValue("rate_limit.enabled", String.valueOf(properties.isEnabled())));
    }

    private int configuredLimit(String key, int fallback) {
        return parsePositiveInt(settingValue(key, String.valueOf(fallback)), fallback);
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
        if (isTrustedProxy(request.getRemoteAddr()) && forwardedFor != null && !forwardedFor.isBlank()) {
            String[] parts = forwardedFor.split(",");
            return parts[parts.length - 1].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isLoopback(String address) {
        return "127.0.0.1".equals(address) || "0:0:0:0:0:0:0:1".equals(address) || "::1".equals(address);
    }

    private boolean isTrustedProxy(String address) {
        if (isLoopback(address)) return true;
        try {
            return java.net.InetAddress.getByName(address).isSiteLocalAddress();
        } catch (Exception exception) {
            return false;
        }
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

    private record CachedSetting(String value, long expiresAt) {
    }

    private String settingValue(String key, String fallback) {
        long now = System.currentTimeMillis();
        CachedSetting cached = settingCache.get(key);
        if (cached != null && cached.expiresAt() > now) {
            return cached.value();
        }
        String value = systemSettingRepository.findById(key)
                .map(setting -> setting.getValue())
                .orElse(fallback);
        settingCache.put(key, new CachedSetting(value, now + 30_000));
        return value;
    }

    private Integer incrementDistributed(String key) {
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) return null;
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, Duration.ofSeconds(WINDOW_SECONDS));
            }
            return count == null ? null : Math.toIntExact(Math.min(count, Integer.MAX_VALUE));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Locale locale = request.getLocale();
        String message = messageSource.getMessage(ErrorCode.TOO_MANY_REQUESTS.getMessageKey(), null, locale);
        ApiError apiError = ApiError.of(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                ErrorCode.TOO_MANY_REQUESTS.name(),
                message);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(WINDOW_SECONDS));
        response.getWriter().write(objectMapper.writeValueAsString(apiError));
    }
}
