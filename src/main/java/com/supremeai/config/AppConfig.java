package com.supremeai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import jakarta.annotation.PostConstruct;

/**
 * Application Configuration Validation
 * 
 * Validates critical configuration values at startup to prevent runtime errors.
 * 
 * User Experience Benefits:
 * - Early error detection with clear messages
 * - Prevents application from starting with invalid configuration
 * - Reduces debugging time for configuration issues
 */
@Configuration
@PropertySource("classpath:application.yml")
public class AppConfig {
    
    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
    
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;
    
    @Value("${spring.data.redis.port:6379}")
    private int redisPort;
    
    @Value("${spring.data.redis.lettuce.pool.max-active:100}")
    private int redisMaxActive;
    
    @Value("${spring.data.redis.lettuce.pool.min-idle:10}")
    private int redisMinIdle;
    
    @Value("${performance.async.core-pool-size:10}")
    private int asyncCorePoolSize;
    
    @Value("${performance.async.max-pool-size:100}")
    private int asyncMaxPoolSize;
    
    @Value("${performance.async.queue-capacity:1000}")
    private int asyncQueueCapacity;
    
    /**
     * Validates configuration at startup
     */
    @PostConstruct
    public void validateConfig() {
        log.info("==================================================");
        log.info("  Validating Application Configuration");
        log.info("==================================================");
        
        // Validate Redis configuration
        validateRedisConfig();
        
        // Validate thread pool configuration
        validateThreadPoolConfig();
        
        log.info("==================================================");
        log.info("  Configuration Validation Complete - All Checks Passed!");
        log.info("==================================================");
    }
    
    private void validateRedisConfig() {
        if (redisHost == null || redisHost.trim().isEmpty()) {
            throw new IllegalStateException(
                "Redis host is not configured. Please set spring.data.redis.host in application.yml");
        }
        
        if (redisPort <= 0 || redisPort > 65535) {
            throw new IllegalStateException(
                String.format("Invalid Redis port: %d. Port must be between 1 and 65535", redisPort));
        }
        
        if (redisMaxActive <= 0) {
            throw new IllegalStateException(
                String.format("Invalid Redis max-active: %d. Must be positive", redisMaxActive));
        }
        
        if (redisMinIdle < 0) {
            throw new IllegalStateException(
                String.format("Invalid Redis min-idle: %d. Must be non-negative", redisMinIdle));
        }
        
        if (redisMinIdle > redisMaxActive) {
            throw new IllegalStateException(
                String.format("Redis min-idle (%d) cannot be greater than max-active (%d)", 
                    redisMinIdle, redisMaxActive));
        }
        
        log.info("Redis Configuration: {}:{}", redisHost, redisPort);
        log.info("  Connection Pool: max-active={}, min-idle={}", redisMaxActive, redisMinIdle);
    }
    
    private void validateThreadPoolConfig() {
        if (asyncCorePoolSize <= 0) {
            throw new IllegalStateException(
                String.format("Invalid async core pool size: %d. Must be positive", asyncCorePoolSize));
        }
        
        if (asyncMaxPoolSize <= 0) {
            throw new IllegalStateException(
                String.format("Invalid async max pool size: %d. Must be positive", asyncMaxPoolSize));
        }
        
        if (asyncCorePoolSize > asyncMaxPoolSize) {
            throw new IllegalStateException(
                String.format("Async core pool size (%d) cannot be greater than max pool size (%d)",
                    asyncCorePoolSize, asyncMaxPoolSize));
        }
        
        if (asyncQueueCapacity < 0) {
            throw new IllegalStateException(
                String.format("Invalid async queue capacity: %d. Must be non-negative", asyncQueueCapacity));
        }
        
        log.info("Async Thread Pool Configuration:");
        log.info("  Core Pool Size: {}", asyncCorePoolSize);
        log.info("  Max Pool Size: {}", asyncMaxPoolSize);
        log.info("  Queue Capacity: {}", asyncQueueCapacity);
    }
}
