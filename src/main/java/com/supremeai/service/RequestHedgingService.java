package com.supremeai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Request hedging service for critical paths.
 * Sends requests to multiple providers simultaneously and takes the first response.
 */
@Service
public class RequestHedgingService {

    private static final Logger log = LoggerFactory.getLogger(RequestHedgingService.class);

    // Hedging delay in milliseconds
    private static final long HEDGING_DELAY_MS = 100;

    /**
     * Execute request with hedging - send to multiple providers, take first response.
     * Useful for critical paths where latency is more important than cost.
     */
    public <T> T executeWithHedging(List<Supplier<T>> providers, long timeoutMs) {
        if (providers.isEmpty()) {
            throw new IllegalArgumentException("At least one provider is required");
        }

        if (providers.size() == 1) {
            return providers.get(0).get();
        }

        CompletableFuture<T> primaryFuture = CompletableFuture.supplyAsync(providers.get(0));
        
        // Schedule hedged requests after delay
        CompletableFuture<T> hedgedFuture = null;
        if (providers.size() > 1) {
            hedgedFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    TimeUnit.MILLISECONDS.sleep(HEDGING_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                // Try remaining providers
                for (int i = 1; i < providers.size(); i++) {
                    try {
                        return providers.get(i).get();
                    } catch (Exception e) {
                        log.debug("Hedged provider {} failed", i, e);
                    }
                }
                return null;
            });
        }

        // Return first completed response
        try {
            if (hedgedFuture != null) {
                return CompletableFuture.anyOf(primaryFuture, hedgedFuture)
                    .get(timeoutMs, TimeUnit.MILLISECONDS);
            } else {
                return primaryFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Request interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Request execution failed", e.getCause());
        } catch (TimeoutException e) {
            // Cancel pending requests
            primaryFuture.cancel(true);
            if (hedgedFuture != null) {
                hedgedFuture.cancel(true);
            }
            throw new RuntimeException("Request timed out after " + timeoutMs + "ms", e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error during hedged request", e);
        }
    }

    /**
     * Execute async request with hedging.
     */
    public <T> CompletableFuture<T> executeWithHedgingAsync(
            List<Supplier<CompletableFuture<T>>> providers, 
            long timeoutMs) {
        
        if (providers.isEmpty()) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("At least one provider is required"));
        }

        if (providers.size() == 1) {
            return providers.get(0).get();
        }

        CompletableFuture<T> primaryFuture = providers.get(0).get();
        
        // Schedule hedged requests after delay
        CompletableFuture<T> hedgedFuture = null;
        if (providers.size() > 1) {
            hedgedFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    TimeUnit.MILLISECONDS.sleep(HEDGING_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                // Try remaining providers
                for (int i = 1; i < providers.size(); i++) {
                    try {
                        return providers.get(i).get().join();
                    } catch (Exception e) {
                        log.debug("Hedged provider {} failed", i, e);
                    }
                }
                return null;
            });
        }

        // Return first completed response
        if (hedgedFuture != null) {
            return (CompletableFuture<T>) CompletableFuture.anyOf(primaryFuture, hedgedFuture)
                .orTimeout(timeoutMs, TimeUnit.MILLISECONDS);
        } else {
            return primaryFuture.orTimeout(timeoutMs, TimeUnit.MILLISECONDS);
        }
    }
}