package com.supremeai.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Circuit breaker configuration with metrics and half-open state testing.
 * Provides automatic failover and recovery for external AI providers.
 */
@Configuration
public class CircuitBreakerConfig {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerConfig.class);

    private final ConcurrentHashMap<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    /**
     * Circuit breaker registry for all providers
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config = 
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
            .failureRateThreshold(50)           // 50% failure rate opens circuit
            .slowCallRateThreshold(50)          // 50% slow calls open circuit
            .slowCallDurationThreshold(Duration.ofSeconds(10))
            .waitDurationInOpenState(Duration.ofSeconds(30))  // 30s before half-open
            .permittedNumberOfCallsInHalfOpenState(3)         // Test 3 calls in half-open
            .slidingWindowType(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
            .slidingWindowSize(10)              // 10 second window
            .minimumNumberOfCalls(5)            // Min 5 calls before evaluating
            .build();

        return CircuitBreakerRegistry.of(config);
    }

    /**
     * Get or create circuit breaker for a provider
     */
    @Bean
    public Function<String, CircuitBreaker> circuitBreakerProvider(CircuitBreakerRegistry registry) {
        return providerName -> circuitBreakers.computeIfAbsent(providerName, name -> {
            log.info("Creating circuit breaker for provider: {}", name);
            return registry.circuitBreaker(name);
        });
    }

    /**
     * OpenAI circuit breaker with specific configuration
     */
    @Bean(name = "openaiCircuitBreaker")
    public CircuitBreaker openaiCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("openai");
    }

    /**
     * Groq circuit breaker - faster timeouts
     */
    @Bean(name = "groqCircuitBreaker")
    public CircuitBreaker groqCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("groq");
    }

    /**
     * Local/Ollama circuit breaker
     */
    @Bean(name = "localCircuitBreaker")
    public CircuitBreaker localCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("local");
    }

    /**
     * Anthropic circuit breaker
     */
    @Bean(name = "anthropicCircuitBreaker")
    public CircuitBreaker anthropicCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("anthropic");
    }
}