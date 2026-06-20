package com.example.KendyDigital.config;

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Auto-configuration for Redis. Beans are created only when `app.redis.enabled=true`.
 */
@Configuration
@EnableConfigurationProperties
@EnableCaching
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
public class RedisAutoConfig {

    @Bean
    @ConfigurationProperties(prefix = "app.redis")
    public AppRedisProperties appRedisProperties() {
        return new AppRedisProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public LettuceConnectionFactory redisConnectionFactory(AppRedisProperties props) {
        RedisStandaloneConfiguration conf = new RedisStandaloneConfiguration();
        conf.setHostName(props.getHost());
        conf.setPort(props.getPort());
        if (props.getPassword() != null && !props.getPassword().isEmpty()) {
            conf.setPassword(RedisPassword.of(props.getPassword()));
        }
        conf.setDatabase(props.getDatabase());

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(props.getTimeoutMs()))
                .build();

        return new LettuceConnectionFactory(conf, clientConfig);
    }

    @Bean
    @ConditionalOnMissingBean
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory factory) {
        StringRedisTemplate tpl = new StringRedisTemplate();
        tpl.setConnectionFactory(factory);
        tpl.afterPropertiesSet();
        return tpl;
    }

    @Bean(name = "redisTemplate")
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory factory) {
        RedisTemplate<String, Object> tpl = new RedisTemplate<>();
        tpl.setConnectionFactory(factory);
        tpl.setKeySerializer(new StringRedisSerializer());
        tpl.setHashKeySerializer(new StringRedisSerializer());
        tpl.afterPropertiesSet();
        return tpl;
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheManager cacheManager(LettuceConnectionFactory factory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
                "services", defaultConfig.entryTtl(Duration.ofMinutes(5)),
                "service-categories", defaultConfig.entryTtl(Duration.ofMinutes(10)),
                "service-by-slug", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    public static class AppRedisProperties {
        private boolean enabled = true;
        private String host = "localhost";
        private int port = 6379;
        private String password;
        private int database = 0;
        private long timeoutMs = 2000;
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getDatabase() {
            return database;
        }

        public void setDatabase(int database) {
            this.database = database;
        }

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }
}
