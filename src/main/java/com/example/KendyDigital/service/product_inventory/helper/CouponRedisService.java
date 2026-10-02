package com.example.KendyDigital.service.product_inventory.helper;

import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

@Service
public class CouponRedisService {
    private static final Logger log = LoggerFactory.getLogger(CouponRedisService.class);

    private static final String STOCK_KEY_PREFIX = "coupon:stock:";
    private static final String USERS_KEY_PREFIX = "coupon:users:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    private static final String ACQUIRE_LUA = """
            local stockKey = KEYS[1]
            local usersKey = KEYS[2]
            local userId = ARGV[1]
            local perUserLimit = tonumber(ARGV[2] or '0')
            local hasUsageLimit = tonumber(ARGV[3] or '0')

            if perUserLimit > 0 then
                if redis.call('SISMEMBER', usersKey, userId) == 1 then
                    return -1
                end
            end

            if hasUsageLimit == 1 then
                if redis.call('EXISTS', stockKey) == 0 then
                    return -2
                end
                local stock = tonumber(redis.call('GET', stockKey) or '0')
                if stock <= 0 then
                    return 0
                end
                redis.call('DECR', stockKey)
            end

            if perUserLimit > 0 then
                redis.call('SADD', usersKey, userId)
            end

            return 1
            """;

    private static final String RELEASE_LUA = """
            local stockKey = KEYS[1]
            local usersKey = KEYS[2]
            local userId = ARGV[1]
            local hasUsageLimit = tonumber(ARGV[2] or '0')
            local perUserLimit = tonumber(ARGV[3] or '0')

            if hasUsageLimit == 1 and redis.call('EXISTS', stockKey) == 1 then
                redis.call('INCR', stockKey)
            end
            if perUserLimit > 0 then
                redis.call('SREM', usersKey, userId)
            end
            return 1
            """;

    private final RedisScript<Long> acquireScript = new DefaultRedisScript<>(ACQUIRE_LUA, Long.class);
    private final RedisScript<Long> releaseScript = new DefaultRedisScript<>(RELEASE_LUA, Long.class);

    public CouponRedisService(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.redisTemplateProvider = redisTemplateProvider;
    }

    public enum AcquireResult {
        SUCCESS,
        OUT_OF_STOCK,
        USER_LIMIT_EXCEEDED,
        SKIPPED_OR_UNAVAILABLE
    }

    public boolean isAvailable() {
        try {
            StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
            return redis != null;
        } catch (Exception ex) {
            log.warn("Redis unavailable for coupon gatekeeper: {}", ex.getMessage());
            return false;
        }
    }

    public AcquireResult tryAcquire(String couponCode, Long userId, Integer usageLimit, Integer perUserLimit,
            int currentRemainingInDb) {
        StringRedisTemplate redis = getRedis();
        if (redis == null) {
            return AcquireResult.SKIPPED_OR_UNAVAILABLE;
        }

        String normalizedCode = couponCode.trim().toUpperCase();
        String stockKey = STOCK_KEY_PREFIX + normalizedCode;
        String usersKey = USERS_KEY_PREFIX + normalizedCode;
        int hasUsageLimit = (usageLimit != null && usageLimit > 0) ? 1 : 0;
        int userLimit = (perUserLimit != null && perUserLimit > 0) ? perUserLimit : 0;

        try {
            Long result = redis.execute(
                    acquireScript,
                    List.of(stockKey, usersKey),
                    String.valueOf(userId),
                    String.valueOf(userLimit),
                    String.valueOf(hasUsageLimit));

            if (result != null && result == -2L) {
                // Key not cached yet: initialize and retry
                initStock(redis, stockKey, currentRemainingInDb);
                result = redis.execute(
                        acquireScript,
                        List.of(stockKey, usersKey),
                        String.valueOf(userId),
                        String.valueOf(userLimit),
                        String.valueOf(hasUsageLimit));
            }

            if (result == null) {
                return AcquireResult.SKIPPED_OR_UNAVAILABLE;
            }
            return switch (result.intValue()) {
                case 1 -> AcquireResult.SUCCESS;
                case 0 -> AcquireResult.OUT_OF_STOCK;
                case -1 -> AcquireResult.USER_LIMIT_EXCEEDED;
                default -> AcquireResult.SKIPPED_OR_UNAVAILABLE;
            };
        } catch (Exception ex) {
            log.warn("Redis acquire failed for coupon {}, falling back to DB: {}", normalizedCode, ex.getMessage());
            return AcquireResult.SKIPPED_OR_UNAVAILABLE;
        }
    }

    public void release(String couponCode, Long userId, Integer usageLimit, Integer perUserLimit) {
        StringRedisTemplate redis = getRedis();
        if (redis == null || couponCode == null || userId == null) {
            return;
        }

        String normalizedCode = couponCode.trim().toUpperCase();
        String stockKey = STOCK_KEY_PREFIX + normalizedCode;
        String usersKey = USERS_KEY_PREFIX + normalizedCode;
        int hasUsageLimit = (usageLimit != null && usageLimit > 0) ? 1 : 0;
        int userLimit = (perUserLimit != null && perUserLimit > 0) ? perUserLimit : 0;

        try {
            redis.execute(
                    releaseScript,
                    List.of(stockKey, usersKey),
                    String.valueOf(userId),
                    String.valueOf(hasUsageLimit),
                    String.valueOf(userLimit));
            log.debug("Released coupon {} for user {} in Redis", normalizedCode, userId);
        } catch (Exception ex) {
            log.warn("Failed to release coupon in Redis {}: {}", normalizedCode, ex.getMessage());
        }
    }

    public void syncCoupon(String couponCode, int remainingStock, boolean active) {
        StringRedisTemplate redis = getRedis();
        if (redis == null || couponCode == null) {
            return;
        }

        String normalizedCode = couponCode.trim().toUpperCase();
        String stockKey = STOCK_KEY_PREFIX + normalizedCode;

        try {
            if (!active) {
                redis.opsForValue().set(stockKey, "0", DEFAULT_TTL);
            } else {
                redis.opsForValue().set(stockKey, String.valueOf(Math.max(0, remainingStock)), DEFAULT_TTL);
            }
            log.info("Synced coupon {} to Redis with stock={}", normalizedCode, remainingStock);
        } catch (Exception ex) {
            log.warn("Failed to sync coupon {} to Redis: {}", normalizedCode, ex.getMessage());
        }
    }

    public void evictCoupon(String couponCode) {
        StringRedisTemplate redis = getRedis();
        if (redis == null || couponCode == null) {
            return;
        }

        String normalizedCode = couponCode.trim().toUpperCase();
        try {
            redis.delete(List.of(STOCK_KEY_PREFIX + normalizedCode, USERS_KEY_PREFIX + normalizedCode));
            log.info("Evicted coupon {} from Redis", normalizedCode);
        } catch (Exception ex) {
            log.warn("Failed to evict coupon {} from Redis: {}", normalizedCode, ex.getMessage());
        }
    }

    public Boolean fastCheckAvailable(String couponCode, Long userId) {
        StringRedisTemplate redis = getRedis();
        if (redis == null || couponCode == null) {
            return null; // Not cached or Redis unavailable
        }

        String normalizedCode = couponCode.trim().toUpperCase();
        String stockKey = STOCK_KEY_PREFIX + normalizedCode;
        String usersKey = USERS_KEY_PREFIX + normalizedCode;

        try {
            if (userId != null) {
                Boolean isMember = redis.opsForSet().isMember(usersKey, String.valueOf(userId));
                if (Boolean.TRUE.equals(isMember)) {
                    return false; // User already used this coupon
                }
            }

            String stockVal = redis.opsForValue().get(stockKey);
            if (stockVal != null) {
                int stock = Integer.parseInt(stockVal);
                if (stock <= 0) {
                    return false; // Out of stock
                }
            }
            return true;
        } catch (Exception ex) {
            return null;
        }
    }

    private void initStock(StringRedisTemplate redis, String stockKey, int remainingStock) {
        try {
            redis.opsForValue().setIfAbsent(stockKey, String.valueOf(Math.max(0, remainingStock)), DEFAULT_TTL);
        } catch (Exception ex) {
            log.warn("Failed to init stock in Redis {}: {}", stockKey, ex.getMessage());
        }
    }

    private StringRedisTemplate getRedis() {
        try {
            return redisTemplateProvider.getIfAvailable();
        } catch (Exception ex) {
            return null;
        }
    }
}
