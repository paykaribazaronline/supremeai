package com.supremeai.intelligence;

import com.supremeai.service.SuperHubOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * StressTestService - Phase 3 Implementation
 * 
 * Performs high-concurrency testing on Super-Hubs to ensure stability 
 * and measure response times/accuracy under load.
 */
@Service
public class StressTestService {

    private static final Logger log = LoggerFactory.getLogger(StressTestService.class);

    private final SuperHubOrchestrator superHubOrchestrator;
public StressTestService(SuperHubOrchestrator superHubOrchestrator) {
        this.superHubOrchestrator = superHubOrchestrator;
    }

    /**
     * Executes a batch of complex tasks simultaneously.
     */
    public Mono<Map<String, Object>> runStressTest(int concurrency, List<String> tasks) {
        log.info("[STRESS_TEST] Starting stress test with concurrency: {} for {} tasks", concurrency, tasks.size());
        
        long startTime = System.currentTimeMillis();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Long> latencies = new ArrayList<>();

        return Flux.fromIterable(tasks)
            .flatMap(task -> {
                long taskStartTime = System.currentTimeMillis();
                return superHubOrchestrator.orchestrate(task, new HashMap<>())
                    .doOnNext(res -> {
                        successCount.incrementAndGet();
                        latencies.add(System.currentTimeMillis() - taskStartTime);
                    })
                    .onErrorResume(e -> {
                        failureCount.incrementAndGet();
                        return Mono.just("Error: " + e.getMessage());
                    });
            }, concurrency)
            .collectList()
            .map(results -> {
                long totalTime = System.currentTimeMillis() - startTime;
                double avgLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
                
                Map<String, Object> stats = new HashMap<>();
                stats.put("totalTasks", tasks.size());
                stats.put("concurrency", concurrency);
                stats.put("successCount", successCount.get());
                stats.put("failureCount", failureCount.get());
                stats.put("totalTimeMs", totalTime);
                stats.put("avgLatencyMs", avgLatency);
                stats.put("throughput", (double) tasks.size() / (totalTime / 1000.0));
                
                log.info("[STRESS_TEST] Completed. Success: {}, Avg Latency: {}ms", successCount.get(), avgLatency);
                return stats;
            });
    }

    /**
     * Generates a list of default complex tasks for testing.
     */
    public List<String> getDefaultTestTasks() {
        return List.of(
            "Create a complex React component with state management using Redux and API integration",
            "Write a Spring Boot controller for managing multi-tenant file uploads to GCS",
            "Design a PostgreSQL database schema for a high-traffic social media platform",
            "Explain the quantum entanglement theory in simple Bengali",
            "Analyze this security vulnerability: SQL injection in legacy PHP code",
            "Generate a marketing strategy for a new AI-powered SaaS product in the healthcare industry",
            "Create a 3D architectural mockup of a modern eco-friendly office building",
            "Optimize a Kubernetes cluster for running high-memory GPU workloads",
            "Perform a sentiment analysis on a batch of 1000 customer reviews in Bengali",
            "Draft a legal compliance policy for data privacy in a cross-border fintech app"
        );
    }
}
