package org.example.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Resilience Wrapper Service
 * Combines rate limiting, circuit breaker, and failover mechanisms
 * Provides unified resilience layer for all external calls
 */
@Service
public class ResilienceWrapperService {
    
    private static final Logger logger = LoggerFactory.getLogger(ResilienceWrapperService.class);
    
    @Autowired(required = false)
    private RateLimiterConfiguration.RateLimiterService rateLimiterService;
    
    @Autowired(required = false)
    private CircuitBreakerConfiguration.CircuitBreakerService circuitBreakerService;
    
    @Autowired(required = false)
    private FailoverConfiguration.FailoverService failoverService;
    
    @Autowired(required = false, name = "aiApiCircuitBreaker")
    private CircuitBreaker aiApiCircuitBreaker;
    
    @Autowired(required = false, name = "externalApiCircuitBreaker")
    private CircuitBreaker externalApiCircuitBreaker;
    
    @Autowired(required = false, name = "databaseCircuitBreaker")
    private CircuitBreaker databaseCircuitBreaker;
    
    /**
     * Execute function with full resilience protection
     * Combines: rate limiting -> circuit breaker -> failover
     */
    public <T> T executeWithResilience(
            String circuitBreakerName,
            String userId,
            String userRole,
            Supplier<T> function,
            Supplier<T> fallback
    ) {
        try {
            // 1. Check rate limit
            if (rateLimiterService != null && !rateLimiterService.allowRequest(userId, userRole)) {
                logger.warn("Rate limit exceeded for user: {} using fallback", userId);
                return fallback != null ? fallback.get() : null;
            }
            
            // 2. Get circuit breaker
            CircuitBreaker breaker = getCircuitBreaker(circuitBreakerName);
            
            if (breaker != null) {
                logger.debug("Executing with circuit breaker: {}", circuitBreakerName);
                return breaker.executeSupplier(() -> {
                    try {
                        T result = function.get();
                        recordSuccess(circuitBreakerName);
                        return result;
                    } catch (Exception e) {
                        recordFailure(circuitBreakerName, e);
                        throw e;
                    }
                });
            } else {
                // No circuit breaker, execute directly
                T result = function.get();
                recordSuccess(circuitBreakerName);
                return result;
            }
            
        } catch (Exception e) {
            logger.warn("Error in resilience execution, using fallback: {}", e.getMessage());
            if (fallback != null) {
                return fallback.get();
            }
            throw new RuntimeException("Resilience execution failed", e);
        }
    }
    
    /**
     * Execute function with resilience and automatic failover
     */
    public <T> T executeWithFailover(
            String failoverGroupName,
            String circuitBreakerName,
            String userId,
            String userRole,
            FailoverFunction<T> function,
            Supplier<T> fallback
    ) {
        try {
            // Get healthy endpoint from failover group
            String endpoint = failoverService != null ? 
                failoverService.getHealthyEndpoint(failoverGroupName) : null;
            
            if (endpoint == null) {
                logger.error("No healthy endpoint found for failover group: {}", failoverGroupName);
                return fallback != null ? fallback.get() : null;
            }
            
            // Execute with resilience protection
            return executeWithResilience(
                circuitBreakerName,
                userId,
                userRole,
                () -> {
                    try {
                        T result = function.execute(endpoint);
                        // Mark endpoint as healthy on success
                        if (failoverService != null) {
                            failoverService.markEndpointHealthy(endpoint);
                        }
                        return result;
                    } catch (Exception e) {
                        // Mark endpoint as unhealthy on failure
                        if (failoverService != null) {
                            failoverService.markEndpointUnhealthy(endpoint, e.getMessage());
                        }
                        throw e;
                    }
                },
                fallback
            );
            
        } catch (Exception e) {
            logger.error("Failover execution failed, using fallback: {}", e.getMessage());
            if (fallback != null) {
                return fallback.get();
            }
            throw new RuntimeException("Failover execution failed", e);
        }
    }
    
    /**
     * Get circuit breaker by name
     */
    private CircuitBreaker getCircuitBreaker(String name) {
        return switch (name) {
            case "aiApiCircuitBreaker" -> aiApiCircuitBreaker;
            case "externalApiCircuitBreaker" -> externalApiCircuitBreaker;
            case "databaseCircuitBreaker" -> databaseCircuitBreaker;
            default -> null;
        };
    }
    
    /**
     * Record successful execution
     */
    private void recordSuccess(String breaker) {
        if (circuitBreakerService != null) {
            circuitBreakerService.recordSuccess(breaker);
        }
    }
    
    /**
     * Record failed execution
     */
    private void recordFailure(String breaker, Throwable exception) {
        if (circuitBreakerService != null) {
            circuitBreakerService.recordFailure(breaker, exception);
        }
    }
    
    /**
     * Check if circuit breaker is open
     */
    public boolean isCircuitBreakerOpen(String name) {
        CircuitBreaker breaker = getCircuitBreaker(name);
        if (breaker != null) {
            return breaker.getState() == CircuitBreaker.State.OPEN;
        }
        return false;
    }
    
    /**
     * Get circuit breaker state
     */
    public String getCircuitBreakerState(String name) {
        CircuitBreaker breaker = getCircuitBreaker(name);
        if (breaker != null) {
            return breaker.getState().toString();
        }
        return "NOT_FOUND";
    }
    
    /**
     * Get failover group status
     */
    public FailoverConfiguration.GroupStatus getFailoverGroupStatus(String groupName) {
        if (failoverService != null) {
            return failoverService.getGroupStatus(groupName);
        }
        return null;
    }
    
    /**
     * Functional interface for failover function
     */
    @FunctionalInterface
    public interface FailoverFunction<T> {
        T execute(String endpoint) throws Exception;
    }
}
