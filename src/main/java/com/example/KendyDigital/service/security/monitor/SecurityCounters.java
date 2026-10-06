package com.example.KendyDigital.service.security.monitor;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Windowed counters used by the detection rules. Prefers Redis (shared across instances) and
 * degrades to an in-process map when Redis is unavailable.
 */
@Component
public class SecurityCounters {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityCounters.class);

    private final ObjectProvider<StringRedisTemplate> redisProvider;
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, StringEntry> values = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> distinctFallback =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> distinctExpiry = new ConcurrentHashMap<>();

    public SecurityCounters(ObjectProvider<StringRedisTemplate> redisProvider) {
        this.redisProvider = redisProvider;
    }

    public long increment(String key, Duration ttl) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis != null) {
            try {
                Long count = redis.opsForValue().increment(key);
                if (count != null && count == 1L) {
                    redis.expire(key, ttl);
                }
                if (count != null) {
                    return count;
                }
            } catch (RuntimeException exception) {
                LOGGER.debug("Redis increment failed for {}; using in-memory fallback", key, exception);
            }
        }
        long now = System.currentTimeMillis();
        WindowCounter counter = counters.compute(key, (ignored, existing) -> {
            if (existing == null || now - existing.startedAt() > ttl.toMillis()) {
                return new WindowCounter(now, new AtomicLong(1));
            }
            existing.count().incrementAndGet();
            return existing;
        });
        return counter.count().get();
    }

    /** Approximate distinct count (HyperLogLog in Redis, exact set in fallback). Returns cardinality. */
    public long distinct(String key, String member, Duration ttl) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis != null) {
            try {
                redis.opsForHyperLogLog().add(key, member);
                redis.expire(key, ttl);
                Long size = redis.opsForHyperLogLog().size(key);
                if (size != null) {
                    return size;
                }
            } catch (RuntimeException exception) {
                LOGGER.debug("Redis HLL failed for {}; using in-memory fallback", key, exception);
            }
        }
        long now = System.currentTimeMillis();
        ConcurrentHashMap<String, Long> set = distinctFallback.compute(key, (ignored, existing) -> {
            Long expiry = distinctExpiry.get(key);
            if (existing == null || expiry == null || now > expiry) {
                return new ConcurrentHashMap<>();
            }
            return existing;
        });
        set.putIfAbsent(member, now);
        distinctExpiry.put(key, now + ttl.toMillis());
        return set.size();
    }

    public String get(String key) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis != null) {
            try {
                return redis.opsForValue().get(key);
            } catch (RuntimeException exception) {
                LOGGER.debug("Redis get failed for {}", key, exception);
            }
        }
        StringEntry entry = values.get(key);
        if (entry == null || entry.expiresAt() < System.currentTimeMillis()) {
            return null;
        }
        return entry.value();
    }

    public void set(String key, String value, Duration ttl) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis != null) {
            try {
                redis.opsForValue().set(key, value, ttl);
                return;
            } catch (RuntimeException exception) {
                LOGGER.debug("Redis set failed for {}", key, exception);
            }
        }
        values.put(key, new StringEntry(value, System.currentTimeMillis() + ttl.toMillis()));
    }

    public boolean setIfAbsent(String key, Duration ttl) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis != null) {
            try {
                Boolean created = redis.opsForValue().setIfAbsent(key, "1", ttl);
                return Boolean.TRUE.equals(created);
            } catch (RuntimeException exception) {
                LOGGER.debug("Redis setIfAbsent failed for {}", key, exception);
            }
        }
        long now = System.currentTimeMillis();
        StringEntry previous = values.get(key);
        if (previous != null && previous.expiresAt() > now) {
            return false;
        }
        values.put(key, new StringEntry("1", now + ttl.toMillis()));
        return true;
    }

    public void delete(String key) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis != null) {
            try {
                redis.delete(key);
            } catch (RuntimeException exception) {
                LOGGER.debug("Redis delete failed for {}", key, exception);
            }
        }
        counters.remove(key);
        values.remove(key);
        distinctFallback.remove(key);
        distinctExpiry.remove(key);
    }

    public int cleanExpired() {
        long now = System.currentTimeMillis();
        int before = counters.size() + values.size() + distinctFallback.size();
        counters.entrySet().removeIf(entry -> now - entry.getValue().startedAt() > 3_600_000L);
        values.entrySet().removeIf(entry -> entry.getValue().expiresAt() < now);
        distinctExpiry.entrySet().removeIf(entry -> entry.getValue() < now);
        distinctFallback.keySet().removeIf(key -> !distinctExpiry.containsKey(key));
        return before - (counters.size() + values.size() + distinctFallback.size());
    }

    private record WindowCounter(long startedAt, AtomicLong count) {
    }

    private record StringEntry(String value, long expiresAt) {
    }

    public Set<String> fallbackKeys() {
        return counters.keySet();
    }
}
