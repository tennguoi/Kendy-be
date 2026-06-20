package com.example.KendyDigital.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fallback cache configuration when Redis is disabled.
 * Uses a NoOpCacheManager so @Cacheable annotations still work
 * without errors but do not actually cache anything.
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new NoOpCacheManager();
    }
}
