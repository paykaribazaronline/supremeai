package com.supremeai.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Service
public class GracefulDegradationService {

    private static final Logger log = LoggerFactory.getLogger(GracefulDegradationService.class);

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final com.supremeai.repository.ProviderRepository providerRepository;

    public GracefulDegradationService(CircuitBreakerRegistry circuitBreakerRegistry,
                                      com.supremeai.repository.ProviderRepository providerRepository) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.providerRepository = providerRepository;
    }

    public <T> T executeWithFallback(String primaryProvider,
                                      Map<String, Supplier<T>> providerSuppliers,
                                      T fallbackResponse) {
        if (isProviderAvailable(primaryProvider)) {
            try {
                return providerSuppliers.get(primaryProvider).get();
            } catch (Exception e) {
                log.warn("Primary provider {} failed, trying fallbacks", primaryProvider, e);
            }
        }

        List<String> cloudProviders = getActiveCloudProviders();
        for (String fallbackProvider : cloudProviders) {
            if (!fallbackProvider.equals(primaryProvider)
                && isProviderAvailable(fallbackProvider)
                && providerSuppliers.containsKey(fallbackProvider)) {
                try {
                    log.info("Falling back to cloud provider: {}", fallbackProvider);
                    return providerSuppliers.get(fallbackProvider).get();
                } catch (Exception e) {
                    log.warn("Cloud fallback provider {} failed", fallbackProvider, e);
                }
            }
        }

        log.warn("All cloud providers failed, returning default fallback response");
        return fallbackResponse;
    }

    public <T> CompletableFuture<T> executeWithFallbackAsync(
            String primaryProvider,
            Map<String, Supplier<CompletableFuture<T>>> providerSuppliers,
            T fallbackResponse) {

        return CompletableFuture.supplyAsync(() -> {
            if (isProviderAvailable(primaryProvider)) {
                try {
                    return providerSuppliers.get(primaryProvider).get().join();
                } catch (Exception e) {
                    log.warn("Primary provider {} failed, trying fallbacks", primaryProvider, e);
                }
            }

            List<String> cloudProviders = getActiveCloudProviders();
            for (String fallbackProvider : cloudProviders) {
                if (!fallbackProvider.equals(primaryProvider)
                    && isProviderAvailable(fallbackProvider)
                    && providerSuppliers.containsKey(fallbackProvider)) {
                    try {
                        log.info("Falling back to cloud provider: {}", fallbackProvider);
                        return providerSuppliers.get(fallbackProvider).get().join();
                    } catch (Exception e) {
                        log.warn("Cloud fallback provider {} failed", fallbackProvider, e);
                    }
                }
            }

            return fallbackResponse;
        });
    }

    private boolean isProviderAvailable(String providerName) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(providerName);
        return circuitBreaker.getState() != CircuitBreaker.State.OPEN;
    }

    public String getBestAvailableProvider(String complexity) {
        List<String> cloudProviders = getActiveCloudProviders();
        for (String provider : cloudProviders) {
            if (isProviderAvailable(provider)) {
                return provider;
            }
        }
        return cloudProviders.isEmpty() ? null : cloudProviders.get(0);
    }

    private List<String> getActiveCloudProviders() {
        try {
            List<String> activeNames = providerRepository.findByStatus("active")
                    .map(com.supremeai.model.APIProvider::getName)
                    .collectList()
                    .block(java.time.Duration.ofSeconds(2));
            return activeNames != null ? activeNames : java.util.Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to resolve active cloud providers: {}", e.getMessage());
            return java.util.Collections.emptyList();
        }
    }
}
