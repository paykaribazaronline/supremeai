package org.example.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Circuit Breaker Configuration using Resilience4j
 * Prevents cascading failures and protects external API calls
 * Automatically recovers when service is healthy
 */
@Configuration
public class CircuitBreakerConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerConfiguration.class);
    
    // Circuit breaker thresholds
    private static final float FAILURE_RATE_THRESHOLD = 50.0f; // 50%
    private static final int MINIMUM_NUMBER_OF_CALLS = 5;
    private static final int SLIDING_WINDOW_SIZE = 10;
    private static final int WAIT_DURATION_IN_OPEN_STATE = 10; // seconds
    private static final int PERMITTED_CALLS_IN_HALF_OPEN_STATE = 3;
    
    /**
     * AI API circuit breaker
     */
    @Bean(name = "aiApiCircuitBreaker")
    public CircuitBreaker aiApiCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(FAILURE_RATE_THRESHOLD)
            .minimumNumberOfCalls(MINIMUM_NUMBER_OF_CALLS)
            .slidingWindowSize(SLIDING_WINDOW_SIZE)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .waitDurationInOpenState(Duration.ofSeconds(WAIT_DURATION_IN_OPEN_STATE))
            .permittedNumberOfCallsInHalfOpenState(PERMITTED_CALLS_IN_HALF_OPEN_STATE)
            .recordExceptions(Exception.class)
            .ignoreExceptions(IllegalArgumentException.class)
            .build();
        
        return CircuitBreaker.of("aiApiCircuitBreaker", config);
    }
    
    /**
     * External API circuit breaker
     */
    @Bean(name = "externalApiCircuitBreaker")
    public CircuitBreaker externalApiCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(FAILURE_RATE_THRESHOLD)
            .minimumNumberOfCalls(MINIMUM_NUMBER_OF_CALLS)
            .slidingWindowSize(SLIDING_WINDOW_SIZE)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .waitDurationInOpenState(Duration.ofSeconds(WAIT_DURATION_IN_OPEN_STATE))
            .permittedNumberOfCallsInHalfOpenState(PERMITTED_CALLS_IN_HALF_OPEN_STATE)
            .recordExceptions(Exception.class)
            .build();
        
        return CircuitBreaker.of("externalApiCircuitBreaker", config);
    }
    
    /**
     * Database circuit breaker
     */
    @Bean(name = "databaseCircuitBreaker")
    public CircuitBreaker databaseCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(30.0f) // Lower threshold for critical database
            .minimumNumberOfCalls(3)
            .slidingWindowSize(5)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .waitDurationInOpenState(Duration.ofSeconds(5))
            .permittedNumberOfCallsInHalfOpenState(2)
            .recordExceptions(Exception.class)
            .build();
        
        return CircuitBreaker.of("databaseCircuitBreaker", config);
    }
    
    /**
     * Authentication circuit breaker
     */
    @Bean(name = "authCircuitBreaker")
    public CircuitBreaker authCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(40.0f)
            .minimumNumberOfCalls(4)
            .slidingWindowSize(8)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .waitDurationInOpenState(Duration.ofSeconds(15))
            .permittedNumberOfCallsInHalfOpenState(2)
            .recordExceptions(Exception.class)
            .build();
        
        return CircuitBreaker.of("authCircuitBreaker", config);
    }
    
    /**
     * Circuit Breaker Registry and management service
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(
        @Qualifier("aiApiCircuitBreaker") CircuitBreaker aiApiCircuitBreaker,
        @Qualifier("externalApiCircuitBreaker") CircuitBreaker externalApiCircuitBreaker,
        @Qualifier("databaseCircuitBreaker") CircuitBreaker databaseCircuitBreaker,
        @Qualifier("authCircuitBreaker") CircuitBreaker authCircuitBreaker
    ) {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(CircuitBreakerConfig.ofDefaults());
        
        // Register all circuit breakers
        registry.circuitBreaker("aiApiCircuitBreaker", aiApiCircuitBreaker.getCircuitBreakerConfig());
        registry.circuitBreaker("externalApiCircuitBreaker", externalApiCircuitBreaker.getCircuitBreakerConfig());
        registry.circuitBreaker("databaseCircuitBreaker", databaseCircuitBreaker.getCircuitBreakerConfig());
        registry.circuitBreaker("authCircuitBreaker", authCircuitBreaker.getCircuitBreakerConfig());
        
        logger.info("✅ Circuit Breakers registered: aiApiCircuitBreaker, externalApiCircuitBreaker, databaseCircuitBreaker, authCircuitBreaker");
        
        return registry;
    }
    
    /**
     * Circuit Breaker management service
     */
    @Bean
    public CircuitBreakerService circuitBreakerService() {
        return new CircuitBreakerService();
    }
    
    /**
     * Service to manage and monitor circuit breakers
     */
    public static class CircuitBreakerService {
        private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerService.class);
        private final ConcurrentHashMap<String, CircuitBreakerMetrics> metrics = new ConcurrentHashMap<>();
        
        /**
         * Record successful call
         */
        public void recordSuccess(String breaker) {
            metrics.computeIfAbsent(breaker, k -> new CircuitBreakerMetrics())
                .recordSuccess();
            logger.debug("Circuit breaker '{}' recorded success", breaker);
        }
        
        /**
         * Record failed call
         */
        public void recordFailure(String breaker, Throwable exception) {
            CircuitBreakerMetrics m = metrics.computeIfAbsent(breaker, k -> new CircuitBreakerMetrics());
            m.recordFailure();
            logger.warn("Circuit breaker '{}' recorded failure: {}", breaker, exception.getMessage());
        }
        
        /**
         * Get circuit breaker metrics
         */
        public CircuitBreakerMetrics getMetrics(String breaker) {
            return metrics.getOrDefault(breaker, new CircuitBreakerMetrics());
        }
        
        /**
         * Get all metrics
         */
        public ConcurrentHashMap<String, CircuitBreakerMetrics> getAllMetrics() {
            return new ConcurrentHashMap<>(metrics);
        }
        
        /**
         * Reset metrics for a breaker
         */
        public void resetMetrics(String breaker) {
            metrics.put(breaker, new CircuitBreakerMetrics());
            logger.info("Metrics reset for circuit breaker: {}", breaker);
        }
    }
    
    /**
     * Metrics for circuit breaker
     */
    public static class CircuitBreakerMetrics {
        private long successCount = 0;
        private long failureCount = 0;
        private long totalCalls = 0;
        
        public synchronized void recordSuccess() {
            successCount++;
            totalCalls++;
        }
        
        public synchronized void recordFailure() {
            failureCount++;
            totalCalls++;
        }
        
        public synchronized double getFailureRate() {
            if (totalCalls == 0) return 0;
            return (double) failureCount / totalCalls * 100;
        }
        
        public long getSuccessCount() { return successCount; }
        public long getFailureCount() { return failureCount; }
        public long getTotalCalls() { return totalCalls; }
    }
}
