package com.supremeai.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Graceful degradation service for fallback to simpler models.
 * Provides fallback mechanisms when complex models fail.
 */
@Service
public class GracefulDegradationService {

    private static final Logger log = LoggerFactory.getLogger(GracefulDegradationService.class);

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    // Provider priority order (simpler to more complex)
    private static final List<String> PROVIDER_FALLBACK_CHAIN = List.of(
        "local",      // Local/Ollama (simplest, most reliable)
        "groq",       // Groq (fast, good for simple tasks)
        "openai",     // OpenAI (balanced)
        "anthropic"   // Anthropic (most complex)
    );

    public GracefulDegradationService(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    /**
     * Execute with fallback to simpler providers.
     * Tries providers in order of simplicity until one succeeds.
     */
    public <T> T executeWithFallback(String primaryProvider, 
                                      Map<String, Supplier<T>> providerSuppliers,
                                      T fallbackResponse) {
        // Try primary provider first
        if (isProviderAvailable(primaryProvider)) {
            try {
                return providerSuppliers.get(primaryProvider).get();
            } catch (Exception e) {
                log.warn("Primary provider {} failed, trying fallbacks", primaryProvider, e);
            }
        }

        // Try fallback providers in order
        for (String fallbackProvider : PROVIDER_FALLBACK_CHAIN) {
            if (!fallbackProvider.equals(primaryProvider) && 
                isProviderAvailable(fallbackProvider) &&
                providerSuppliers.containsKey(fallbackProvider)) {
                try {
                    log.info("Falling back to provider: {}", fallbackProvider);
                    return providerSuppliers.get(fallbackProvider).get();
                } catch (Exception e) {
                    log.warn("Fallback provider {} failed", fallbackProvider, e);
                }
            }
        }

        // Return default fallback response
        log.warn("All providers failed, returning default fallback response");
        return fallbackResponse;
    }

    /**
     * Execute with fallback using CompletableFuture for async operations.
     */
    public <T> CompletableFuture<T> executeWithFallbackAsync(
            String primaryProvider,
            Map<String, Supplier<CompletableFuture<T>>> providerSuppliers,
            T fallbackResponse) {
        
        return CompletableFuture.supplyAsync(() -> {
            // Try primary provider first
            if (isProviderAvailable(primaryProvider)) {
                try {
                    return providerSuppliers.get(primaryProvider).get().join();
                } catch (Exception e) {
                    log.warn("Primary provider {} failed, trying fallbacks", primaryProvider, e);
                }
            }

            // Try fallback providers in order
            for (String fallbackProvider : PROVIDER_FALLBACK_CHAIN) {
                if (!fallbackProvider.equals(primaryProvider) && 
                    isProviderAvailable(fallbackProvider) &&
                    providerSuppliers.containsKey(fallbackProvider)) {
                    try {
                        log.info("Falling back to provider: {}", fallbackProvider);
                        return providerSuppliers.get(fallbackProvider).get().join();
                    } catch (Exception e) {
                        log.warn("Fallback provider {} failed", fallbackProvider, e);
                    }
                }
            }

            return fallbackResponse;
        });
    }

    /**
     * Check if a provider is available (circuit not open).
     */
    private boolean isProviderAvailable(String providerName) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(providerName);
        return circuitBreaker.getState() != CircuitBreaker.State.OPEN;
    }

    /**
     * Get the best available provider for a given complexity level.
     */
    public String getBestAvailableProvider(String complexity) {
        for (String provider : PROVIDER_FALLBACK_CHAIN) {
            if (isProviderAvailable(provider)) {
                return provider;
            }
        }
        return "local"; // Default to local as last resort
    }
}