package com.supremeai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Parallel provider execution service for consensus voting.
 * Executes requests across multiple providers and aggregates results.
 * FIXED: Removed blocking .join() calls, now fully reactive.
 */
@Service
public class ParallelProviderService {

    private static final Logger log = LoggerFactory.getLogger(ParallelProviderService.class);

    /**
     * Execute request across multiple providers in parallel.
     * Returns first successful response.
     * FIXED: Returns CompletionStage instead of blocking.
     */
    @SuppressWarnings("unchecked")
    public <T> CompletionStage<T> executeParallelFirstSuccess(
            Map<String, ? extends CompletionStage<T>> providerRequests) {
        
        CompletableFuture<T>[] futures = providerRequests.values().stream()
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new);
        
        CompletableFuture<Object> anyOf = CompletableFuture.anyOf(futures);
        
        return anyOf.thenApply(result -> {
            try {
                return (T) result;
            } catch (Exception e) {
                log.error("All parallel providers failed", e);
                throw new RuntimeException("All providers failed", e);
            }
        });
    }

    /**
     * Execute request across multiple providers and collect all responses.
     * FIXED: Returns CompletionStage instead of blocking.
     */
    public <T> CompletionStage<Map<String, T>> executeParallelAll(
            Map<String, ? extends CompletionStage<T>> providerRequests) {
        
        List<CompletionStage<Map.Entry<String, T>>> entryFutures = providerRequests.entrySet().stream()
            .map(entry -> entry.getValue().toCompletableFuture()
                .handle((result, ex) -> {
                    if (ex != null) {
                        log.warn("Provider {} failed", entry.getKey(), ex);
                        return Map.<String, T>entry(entry.getKey(), null);
                    }
                    return Map.<String, T>entry(entry.getKey(), result);
                }))
            .collect(Collectors.toList());
        
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            entryFutures.stream()
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new)
        );
        
        return allFutures.thenApply(v -> 
            entryFutures.stream()
                .map(CompletionStage::toCompletableFuture)
                .map(CompletableFuture::join)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }

    /**
     * Execute with consensus voting.
     * Returns the most common response.
     * FIXED: Returns CompletionStage instead of blocking.
     */
    public <T> CompletionStage<T> executeWithConsensus(
            Map<String, ? extends CompletionStage<T>> providerRequests,
            int minAgreement) {
        
        return executeParallelAll(providerRequests).thenApply(responses -> {
            // Count response frequencies
            Map<T, Long> responseCounts = responses.values().stream()
                .filter(response -> response != null)
                .collect(Collectors.groupingBy(
                    response -> response,
                    Collectors.counting()
                ));
            
            // Find most common response
            return responseCounts.entrySet().stream()
                .filter(entry -> entry.getValue() >= minAgreement)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new RuntimeException("No consensus reached"));
        });
    }

    /**
     * Execute with weighted voting based on provider reliability.
     * FIXED: Returns CompletionStage instead of blocking.
     */
    public <T> CompletionStage<T> executeWithWeightedConsensus(
            Map<String, ? extends CompletionStage<T>> providerRequests,
            Map<String, Double> providerWeights) {
        
        return executeParallelAll(providerRequests).thenApply(responses -> {
            // Calculate weighted scores
            Map<T, Double> weightedScores = responses.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.groupingBy(
                    Map.Entry::getValue,
                    Collectors.summingDouble(entry -> 
                        providerWeights.getOrDefault(entry.getKey(), 1.0)
                    )
                ));
            
            // Find highest weighted score
            return weightedScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new RuntimeException("No consensus reached"));
        });
    }
}