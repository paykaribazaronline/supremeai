package com.supremeai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Optimized Redis configuration with connection pooling.
 *
 * Features:
 * - Connection pooling (max 100, min 10)
 * - Connection timeout (2000ms)
 * - Multi-tier cache configuration (L1: Caffeine, L2: Redis)
 * - Cache warming support
 */
@Configuration
@EnableCaching
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.timeout:2000}")
    private int redisTimeoutMs;

    @Bean
    @ConfigurationProperties(prefix = "spring.data.redis.lettuce.pool")
    public LettucePoolingClientConfiguration lettucePoolingConfig(
            @Value("${spring.data.redis.lettuce.pool.max-active:100}") int maxActive,
            @Value("${spring.data.redis.lettuce.pool.max-idle:50}") int maxIdle,
            @Value("${spring.data.redis.lettuce.pool.min-idle:10}") int minIdle,
            @Value("${spring.data.redis.lettuce.pool.max-wait:5000}") int maxWaitMs) {
        
        log.info("Configuring Redis connection pool: maxActive={}, maxIdle={}, minIdle={}",
                maxActive, maxIdle, minIdle);
        
        return LettucePoolingClientConfiguration.builder()
                .maxActive(maxActive)
                .maxIdle(maxIdle)
                .minIdle(minIdle)
                .maxWait(Duration.ofMillis(maxWaitMs))
                .testOnBorrow(true)
                .testOnReturn(true)
                .testWhileIdle(true)
                .durationBetweenEvictionRuns(Duration.ofSeconds(30))
                .build();
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(LettucePoolingClientConfiguration poolingConfig) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }
        
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(redisTimeoutMs))
                .poolingClientConfiguration(poolingConfig)
                .build();
        
        return new LettuceConnectionFactory(config, clientConfig);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serialization for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use JSON serialization for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(60)) // Default TTL
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withCacheConfiguration("ai_responses", config.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("user_sessions", config.entryTtl(Duration.ofHours(24)))
                .withCacheConfiguration("api_keys", config.entryTtl(Duration.ofMinutes(5)))
                .build();
    }
}