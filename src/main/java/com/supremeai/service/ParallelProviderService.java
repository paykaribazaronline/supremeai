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
 */
@Service
public class ParallelProviderService {

    private static final Logger log = LoggerFactory.getLogger(ParallelProviderService.class);

    /**
     * Execute request across multiple providers in parallel.
     * Returns first successful response.
     */
    @SuppressWarnings("unchecked")
    public <T> T executeParallelFirstSuccess(
            Map<String, ? extends CompletionStage<T>> providerRequests) {
        
        CompletableFuture<Object> anyOf = CompletableFuture.anyOf(
            providerRequests.values().toArray(CompletableFuture[]::new)
        );
        
        try {
            return (T) anyOf.join();
        } catch (Exception e) {
            log.error("All parallel providers failed", e);
            throw new RuntimeException("All providers failed", e);
        }
    }

    /**
     * Execute request across multiple providers and collect all responses.
     */
    public <T> Map<String, T> executeParallelAll(
            Map<String, ? extends CompletionStage<T>> providerRequests) {
        
        return providerRequests.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    try {
                        return (T) entry.getValue().toCompletableFuture().join();
                    } catch (Exception e) {
                        log.warn("Provider {} failed", entry.getKey(), e);
                        return null;
                    }
                }
            ));
    }

    /**
     * Execute with consensus voting.
     * Returns the most common response.
     */
    public <T> T executeWithConsensus(
            Map<String, ? extends CompletionStage<T>> providerRequests,
            int minAgreement) {
        
        Map<String, T> responses = executeParallelAll(providerRequests);
        
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
    }

    /**
     * Execute with weighted voting based on provider reliability.
     */
    public <T> T executeWithWeightedConsensus(
            Map<String, ? extends CompletionStage<T>> providerRequests,
            Map<String, Double> providerWeights) {
        
        Map<String, T> responses = executeParallelAll(providerRequests);
        
        // Calculate weighted scores
        Map<T, Double> weightedScores = responses.entrySet().stream()
            .filter(entry -> entry.getValue() != null)
            .collect(Collectors.groupingBy(
                Map.Entry::getValue,
                Collectors.summingDouble(entry -> 
                    providerWeights.getOrDefault(entry.getKey(), 1.0)
                )
            ));
        
        return weightedScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElseThrow(() -> new RuntimeException("No consensus reached"));
    }

    /**
     * Execute with timeout and fallback.
     */
    @SuppressWarnings("unchecked")
    public <T> T executeParallelWithTimeout(
            Map<String, ? extends CompletionStage<T>> providerRequests,
            long timeoutMs,
            T fallback) {
        
        CompletableFuture<Object> anyOf = CompletableFuture.anyOf(
            providerRequests.values().toArray(CompletableFuture[]::new)
        );
        
        try {
            return (T) anyOf.completeOnTimeout(fallback, timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                .join();
        } catch (Exception e) {
            log.warn("Parallel execution failed, using fallback", e);
            return fallback;
        }
    }
}