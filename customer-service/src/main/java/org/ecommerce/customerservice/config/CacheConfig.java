package org.ecommerce.customerservice.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis-backed caching for customer reads.
 *
 * <p>{@code GET /v1/customers/{id}} is a hot path: order-service calls it on every order creation to
 * validate the buyer and snapshot their email. Customer records change rarely, so caching them
 * removes a database round trip from the critical order path.</p>
 *
 * <p>Values are stored as JSON rather than with JDK serialization so cache entries stay readable and
 * survive class changes that do not alter the JSON shape. A 10 minute TTL bounds staleness, and
 * writes evict explicitly so updates are visible immediately.</p>
 */
@Slf4j
@Configuration
@EnableCaching
@ConditionalOnProperty(prefix = "application.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CacheConfig implements CachingConfigurer {

    public static final String CUSTOMERS_CACHE = "customers";
    public static final String CUSTOMER_EXISTS_CACHE = "customer-exists";

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        ObjectMapper cacheObjectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                // Never cache nulls: a miss for a not-yet-created customer must not be pinned for 10 minutes.
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(cacheObjectMapper)));
    }

    /**
     * A cache is an optimisation, never a source of truth, so a Redis outage must not turn every
     * customer read into a 500. Errors are logged and the call falls through to the database.
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {

            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache read failed for {}[{}]; falling back to the database", cache.getName(), key, exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Cache write failed for {}[{}]", cache.getName(), key, exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache evict failed for {}[{}]", cache.getName(), key, exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Cache clear failed for {}", cache.getName(), exception);
            }
        };
    }
}

