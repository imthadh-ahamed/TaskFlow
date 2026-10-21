package com.taskflow.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Configures Spring Cache with Redis as the backing store.
 *
 * Falls back to a no-op CacheManager when Redis is unavailable so that
 * application startup and request handling are never blocked by Redis outages.
 * All cached reads fall through to the database in the no-op case.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    /** Default TTL for all caches unless overridden per-cache. */
    public static final Duration DEFAULT_TTL = Duration.ofSeconds(30);

    /** Cache name for per-user project list pages. */
    public static final String PROJECTS_BY_USER = "projects-by-user";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        try {
            connectionFactory.getConnection().close();
        } catch (Exception ex) {
            log.warn("Redis unavailable — caching disabled ({})", ex.getMessage());
            return new NoOpCacheManager();
        }

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .build();
    }
}
