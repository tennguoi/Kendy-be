package com.example.KendyDigital.common;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final long WINDOW_SECONDS = 60;

    private final RateLimitProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final Map<String, WindowCounter> fallbackCounters = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties properties,
            @Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
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
        boolean allowed;

        if (redisTemplate != null) {
            allowed = checkRedis(key, limit);
        } else {
            allowed = checkInMemory(key, limit);
        }

        if (!allowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Too many requests\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Redis-backed rate limiting using INCR + EXPIRE.
     * Falls back to in-memory if Redis is unavailable.
     */
    private boolean checkRedis(String key, int limit) {
        try {
            String redisKey = "rate_limit:" + key;
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1) {
                redisTemplate.expire(redisKey, WINDOW_SECONDS, TimeUnit.SECONDS);
            }
            return count != null && count <= limit;
        } catch (Exception ex) {
            log.warn("Redis rate limit check failed, falling back to in-memory: {}", ex.getMessage());
            return checkInMemory(key, limit);
        }
    }

    /**
     * In-memory fallback rate limiting using ConcurrentHashMap.
     */
    private boolean checkInMemory(String key, int limit) {
        WindowCounter counter = fallbackCounters.compute(key, (ignored, existing) -> {
            long now = Instant.now().getEpochSecond();
            if (existing == null || now - existing.windowStartedAt() >= WINDOW_SECONDS) {
                return new WindowCounter(now, new AtomicInteger(1));
            }
            existing.count().incrementAndGet();
            return existing;
        });
        return counter.count().get() <= limit;
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
