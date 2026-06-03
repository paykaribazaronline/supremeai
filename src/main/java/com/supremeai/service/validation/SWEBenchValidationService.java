package com.supremeai.service.validation;

import com.supremeai.service.EnhancedMultiAIConsensusService;
import com.supremeai.service.FirebaseRealtimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * BV-02: SWE-bench style test set
 * Executes a curated list of coding tasks with known expected outcomes to measure 
 * SupremeAI's pass rate vs single-model baselines.
 */
@Service
public class SWEBenchValidationService {
    public SWEBenchValidationService(AIValidationHarnessService harnessService, FirebaseRealtimeService firebaseRealtimeService, EnhancedMultiAIConsensusService consensusService) {
        this.harnessService = harnessService;
        this.firebaseRealtimeService = firebaseRealtimeService;
        this.consensusService = consensusService;
    }


    private static final Logger log = LoggerFactory.getLogger(SWEBenchValidationService.class);




    // Simulated SWE-bench dataset (normally loaded from a file/DB)
    private final List<SweTask> sweTasks = Arrays.asList(
            new SweTask("swe_001", "Fix NPE when user ID is null in UserService.getUser()", "if (userId == null) throw new IllegalArgumentException();"),
            new SweTask("swe_002", "Optimize the O(N^2) loop in calculateMetrics to O(N)", "Map<String, Object> lookup = new HashMap<>();"),
            new SweTask("swe_003", "Add CORS mapping for api/v2 endpoints", "registry.addMapping(\"/api/v2/**\").allowedOrigins(\"*\");")
            // In a real scenario, this would contain 50+ tasks loaded dynamically.
    );

    public Mono<Map<String, Object>> runSweBenchSuite(List<String> baselineProviders) {
        log.info("Starting SWE-bench style validation suite...");
        String runId = "swe_" + UUID.randomUUID().toString().substring(0, 8);
        
        return Flux.fromIterable(sweTasks)
                .flatMap(task -> evaluateTask(task, baselineProviders))
                .collectList()
                .flatMap(results -> {
                    // Calculate pass rates
                    int totalTasks = results.size();
                    int supremePasses = 0;
                    Map<String, Integer> baselinePasses = new HashMap<>();
                    
                    for (Map<String, Object> res : results) {
                        if ((Boolean) res.getOrDefault("supremePass", false)) supremePasses++;
                        
                        Map<String, Boolean> baseResults = (Map<String, Boolean>) res.get("baselinePasses");
                        if (baseResults != null) {
                            for (Map.Entry<String, Boolean> e : baseResults.entrySet()) {
                                if (e.getValue()) {
                                    baselinePasses.put(e.getKey(), baselinePasses.getOrDefault(e.getKey(), 0) + 1);
                                }
                            }
                        }
                    }

                    Map<String, Object> summary = new HashMap<>();
                    summary.put("runId", runId);
                    summary.put("totalTasks", totalTasks);
                    summary.put("supremePassRate", (double) supremePasses / totalTasks);
                    
                    Map<String, Double> baseRates = new HashMap<>();
                    for (Map.Entry<String, Integer> entry : baselinePasses.entrySet()) {
                        baseRates.put(entry.getKey(), (double) entry.getValue() / totalTasks);
                    }
                    summary.put("baselinePassRates", baseRates);
                    summary.put("details", results);

                    return firebaseRealtimeService.setData("benchmark_swe/" + runId, summary)
                            .thenReturn(summary);
                });
    }

    private Mono<Map<String, Object>> evaluateTask(SweTask task, List<String> baselineProviders) {
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.id);
        
        // Simplified evaluation: checking if expected code snippet is in the response
        // In a real system, this would compile and run unit tests.
        
        return consensusService.discussAndVote(task.prompt, baselineProviders, 1)
                .map(consensus -> {
                    boolean pass = consensus.consensusAnswer.contains(task.expectedSnippet);
                    result.put("supremePass", pass);
                    result.put("supremeResponse", consensus.consensusAnswer);
                    
                    // For baselines, we look at the individual votes returned in the consensus
                    Map<String, Boolean> baselinePasses = new HashMap<>();
                    for (com.supremeai.model.ProviderVote vote : consensus.votes) {
                        baselinePasses.put(vote.getProviderName(), vote.getResponse().contains(task.expectedSnippet));
                    }
                    result.put("baselinePasses", baselinePasses);
                    
                    return result;
                })
                .onErrorResume(e -> {
                    result.put("error", e.getMessage());
                    result.put("supremePass", false);
                    return Mono.just(result);
                });
    }

    private static class SweTask {
        String id;
        String prompt;
        String expectedSnippet;

        public SweTask(String id, String prompt, String expectedSnippet) {
            this.id = id;
            this.prompt = prompt;
            this.expectedSnippet = expectedSnippet;
        }
    }
}
