package com.example.KendyDigital.common;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.KendyDigital.config.RateLimitProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final long WINDOW_SECONDS = 60;

    private final RateLimitProperties properties;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        int limit = limitFor(request);
        if (!properties.isEnabled() || limit <= 0) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = clientIp(request) + ":" + request.getMethod() + ":" + request.getRequestURI();
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
        if ("POST".equals(method) && ("/api/auth/login".equals(path) || "/api/auth/register".equals(path))) {
            return properties.getAuthPerMinute();
        }
        if ("POST".equals(method) && "/api/webhooks/sepay".equals(path)) {
            return properties.getWebhookPerMinute();
        }
        if ("POST".equals(method) && ("/api/deposits".equals(path) || "/api/orders".equals(path))) {
            return properties.getFinancePerMinute();
        }
        return 0;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record WindowCounter(long windowStartedAt, AtomicInteger count) {
    }
}
