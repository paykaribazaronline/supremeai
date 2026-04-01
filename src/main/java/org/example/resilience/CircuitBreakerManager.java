package org.example.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Circuit Breaker Manager Service
 * Centralized management of circuit breakers for failover scenarios
 */
@Service
public class CircuitBreakerManager {
    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerManager.class);
    
    private final CircuitBreakerRegistry registry;
    private final Map<String, CircuitBreakerStats> stats = new ConcurrentHashMap<>();
    
    public CircuitBreakerManager() {
        // Default config for circuit breakers
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50.0f)              // 50% failure rate triggers open
            .slowCallRateThreshold(50.0f)             // 50% slow calls triggers open
            .slowCallDurationThreshold(Duration.ofSeconds(2))
            .waitDurationInOpenState(Duration.ofSeconds(30))  // 30 seconds before half-open
            .permittedNumberOfCallsInHalfOpenState(3)
            .minimumNumberOfCalls(5)                  // At least 5 calls to evaluate
            .recordExceptions(Exception.class)
            .ignoreExceptions()
            .build();
        
        this.registry = CircuitBreakerRegistry.of(defaultConfig);
        registry.getEventPublisher()
            .onEntryAdded(event -> {
                String name = event.getAddedEntry().getName();
                stats.put(name, new CircuitBreakerStats(name));
                logger.info("✅ Circuit breaker created: {}", name);
            })
            .onEntryRemoved(event -> {
                String name = event.getRemovedEntry().getName();
                stats.remove(name);
                logger.info("❌ Circuit breaker removed: {}", name);
            });
    }
    
    /**
     * Create or get circuit breaker with custom config
     */
    public CircuitBreaker getOrCreateCircuitBreaker(String name, CircuitBreakerConfig config) {
        return registry.circuitBreaker(name, config);
    }
    
    /**
     * Create circuit breaker with default config
     */
    public CircuitBreaker getOrCreateCircuitBreaker(String name) {
        return registry.circuitBreaker(name);
    }
    
    /**
     * Execute function with circuit breaker protection
     */
    public <T> T executeWithCircuitBreaker(String name, java.util.function.Callable<T> callable) throws Exception {
        CircuitBreaker breaker = getOrCreateCircuitBreaker(name);
        CircuitBreakerStats stat = stats.get(name);
        
        long startTime = System.currentTimeMillis();
        try {
            T result = breaker.executeCallable(callable);
            long duration = System.currentTimeMillis() - startTime;
            if (stat != null) {
                stat.recordSuccess(duration);
            }
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            if (stat != null) {
                stat.recordFailure(duration);
            }
            logger.error("❌ Circuit breaker {} failed: {}", name, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Get circuit breaker status
     */
    public Map<String, Object> getCircuitBreakerStatus(String name) {
        CircuitBreaker breaker = registry.find(name).orElse(null);
        if (breaker == null) {
            return null;
        }
        
        CircuitBreakerStats stat = stats.get(name);
        
        return new LinkedHashMap<String, Object>() {{
            put("name", name);
            put("state", breaker.getState().toString());
            put("failure_rate", breaker.getMetrics().getFailureRate());
            put("slow_call_rate", breaker.getMetrics().getSlowCallRate());
            put("total_calls", breaker.getMetrics().getNumberOfTotalCalls());
            put("successful_calls", breaker.getMetrics().getNumberOfSuccessfulCalls());
            put("failed_calls", breaker.getMetrics().getNumberOfFailedCalls());
            put("not_permitted_calls", breaker.getMetrics().getNumberOfNotPermittedCalls());
            if (stat != null) {
                put("avg_duration_ms", stat.getAverageDuration());
                put("min_duration_ms", stat.getMinDuration());
                put("max_duration_ms", stat.getMaxDuration());
            }
        }};
    }
    
    /**
     * Get all circuit breaker statuses
     */
    public Map<String, Map<String, Object>> getAllCircuitBreakers() {
        Map<String, Map<String, Object>> result = new HashMap<>();
        registry.getAllCircuitBreakers().forEach(cb -> {
            String name = cb.getName();
            result.put(name, getCircuitBreakerStatus(name));
        });
        return result;
    }
    
    /**
     * Reset circuit breaker
     */
    public void resetCircuitBreaker(String name) {
        CircuitBreaker breaker = registry.find(name).orElse(null);
        if (breaker != null) {
            breaker.reset();
            CircuitBreakerStats stat = stats.get(name);
            if (stat != null) {
                stat.reset();
            }
            logger.info("🔄 Circuit breaker reset: {}", name);
        }
    }
    
    /**
     * Transition to half-open state (for testing)
     */
    public void transitionToHalfOpen(String name) {
        CircuitBreaker breaker = registry.find(name).orElse(null);
        if (breaker != null) {
            if (breaker.getState() == CircuitBreaker.State.OPEN) {
                breaker.transitionToHalfOpenState();
                logger.info("🔄 Circuit breaker transitioned to HALF_OPEN: {}", name);
            }
        }
    }
    
    // Inner class for statistics
    private static class CircuitBreakerStats {
        private final String name;
        private long successCount = 0;
        private long failureCount = 0;
        private long totalDuration = 0;
        private long minDuration = Long.MAX_VALUE;
        private long maxDuration = 0;
        
        public CircuitBreakerStats(String name) {
            this.name = name;
        }
        
        public synchronized void recordSuccess(long duration) {
            successCount++;
            totalDuration += duration;
            minDuration = Math.min(minDuration, duration);
            maxDuration = Math.max(maxDuration, duration);
        }
        
        public synchronized void recordFailure(long duration) {
            failureCount++;
            totalDuration += duration;
            maxDuration = Math.max(maxDuration, duration);
        }
        
        public synchronized void reset() {
            successCount = 0;
            failureCount = 0;
            totalDuration = 0;
            minDuration = Long.MAX_VALUE;
            maxDuration = 0;
        }
        
        public synchronized long getAverageDuration() {
            long total = successCount + failureCount;
            return total > 0 ? totalDuration / total : 0;
        }
        
        public synchronized long getMinDuration() {
            return minDuration == Long.MAX_VALUE ? 0 : minDuration;
        }
        
        public synchronized long getMaxDuration() {
            return maxDuration;
        }
    }
}
