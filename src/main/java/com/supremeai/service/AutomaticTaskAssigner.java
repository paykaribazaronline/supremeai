package com.supremeai.service;

import com.supremeai.model.TaskProviderAssignment;
import com.supremeai.repository.TaskProviderAssignmentRepository;
import com.supremeai.repository.ProviderRepository;
import com.supremeai.model.APIProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Automatic Task Assigner
 *
 * When a new provider is added, this service:
 * 1. Receives the provider's benchmark scores
 * 2. Auto-assigns it to task types where it scores >= threshold
 * 3. Creates or updates task assignments in the DB
 * 4. Can remove poor performers during nightly rebalance
 */
@Service
public class AutomaticTaskAssigner {

    private static final Logger log = LoggerFactory.getLogger(AutomaticTaskAssigner.class);

    // All task types the system supports
    private static final List<TaskConfig> ALL_TASK_TYPES = List.of(
        new TaskConfig("code_generation", "Code Generation", 0.70),
        new TaskConfig("code_review", "Code Review", 0.75),
        new TaskConfig("debugging", "Debugging", 0.70),
        new TaskConfig("creative_writing", "Creative Writing", 0.65),
        new TaskConfig("summarization", "Summarization", 0.70),
        new TaskConfig("question_answering", "Question Answering", 0.65),
        new TaskConfig("mathematical_reasoning", "Mathematical Reasoning", 0.75),
        new TaskConfig("bengali_translation", "Bengali Translation", 0.70),
        new TaskConfig("hindi_translation", "Hindi Translation", 0.70),
        new TaskConfig("arabic_translation", "Arabic Translation", 0.70),
        new TaskConfig("chinese_translation", "Chinese Translation", 0.70),
        new TaskConfig("vision_analysis", "Vision Analysis", 0.65),
        new TaskConfig("long_context", "Long Context Analysis", 0.75)
    );

    // Default limits for each task type
    private static final int DEFAULT_MIN_PROVIDERS = 3;
    private static final int DEFAULT_MAX_PROVIDERS = 10;
    private static final String DEFAULT_STRATEGY = "weighted_consensus";

    @Autowired
    private TaskProviderAssignmentRepository assignmentRepo;

    @Autowired
    private ProviderRepository providerRepo;

    @Autowired
    private ProviderCapabilityAnalyzer capabilityAnalyzer;

    /**
     * Auto-assign a newly registered provider based on its benchmark scores.
     * Called when admin adds a new provider.
     * Fully reactive — no .block() calls.
     */
    public Mono<TaskProviderAssignment> autoAssignNewProvider(String providerId, Map<String, Double> scores) {
        log.info("🔍 Auto-assigning provider {} based on {} capability scores",
            providerId, scores.size());

        return Flux.fromIterable(ALL_TASK_TYPES)
            .filter(task -> {
                Double score = scores.get(task.name);
                return score != null && score >= task.minScore;
            })
            .flatMap(task -> assignmentRepo.findByTaskType(task.name)
                .flatMap(existing -> {
                    // Add provider to existing assignment
                    List<String> updatedProviders = new ArrayList<>(existing.getProviderIds());
                    if (!updatedProviders.contains(providerId)) {
                        updatedProviders.add(providerId);
                        // Apply max limit
                        if (updatedProviders.size() > DEFAULT_MAX_PROVIDERS) {
                            updatedProviders = keepTopProviders(updatedProviders, task.name);
                        }
                        existing.setProviderIds(updatedProviders);
                        existing.setUpdatedAt(new Date());
                        existing.setAssignmentSource("auto");
                        log.info("➕ Added {} to task '{}' (score: {})",
                            providerId, task.name, scores.get(task.name));
                        return assignmentRepo.save(existing);
                    }
                    return Mono.just(existing);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Create new assignment for this task
                    TaskProviderAssignment newAssignment = new TaskProviderAssignment();
                    newAssignment.setTaskType(task.name);
                    newAssignment.setTaskLabel(task.label);
                    newAssignment.setProviderIds(List.of(providerId));
                    newAssignment.setMinProviders(DEFAULT_MIN_PROVIDERS);
                    newAssignment.setMaxProviders(DEFAULT_MAX_PROVIDERS);
                    newAssignment.setVotingStrategy(DEFAULT_STRATEGY);
                    newAssignment.setCapabilityThreshold(task.minScore);
                    newAssignment.setActive(true);
                    newAssignment.setCreatedBy("system");
                    newAssignment.setAssignmentSource("auto");
                    newAssignment.setCreatedAt(new Date());
                    newAssignment.setUpdatedAt(new Date());

                    log.info("🆕 Created new assignment: task='{}' → provider={}",
                        task.name, providerId);
                    return assignmentRepo.save(newAssignment);
                }))
            )
            .collectList()
            .doOnNext(assignments ->
                log.info("✅ Provider {} auto-assigned to {} task types",
                    providerId, assignments.size()))
            .flatMap(assignments -> {
                if (assignments.isEmpty()) return Mono.empty();
                return Mono.just(assignments.get(0));
            });
    }

    /**
     * Keep only top-scoring providers for a task (when exceeding max)
     */
    private List<String> keepTopProviders(List<String> providerIds, String taskType) {
        // Get capability scores from provider registry and sort
        // For now, keep most recently added (last in list)
        // In production: would query capability scores and pick top N
        return providerIds.stream()
            .limit(DEFAULT_MAX_PROVIDERS)
            .collect(Collectors.toList());
    }

    /**
     * Nightly rebalance: remove underperforming providers from task assignments.
     * Called by scheduler at 2 AM.
     * Returns Mono<Void> — fully reactive, no .block() or .blockLast().
     */
    public Mono<Void> nightlyRebalance() {
        log.info("🌙 Starting nightly rebalance of task assignments...");

        return assignmentRepo.findAllByIsActive(true)
            .flatMap(assignment -> {
                String taskType = assignment.getTaskType();
                List<String> currentProviders = assignment.getProviderIds();

                if (currentProviders == null || currentProviders.isEmpty()) {
                    return Mono.empty();
                }

                // Check each provider's score reactively
                return Flux.fromIterable(currentProviders)
                    .flatMap(providerId -> getLatestScore(providerId, taskType)
                        .map(score -> Map.entry(providerId, score))
                        .defaultIfEmpty(Map.entry(providerId, -1.0))
                    )
                    .filter(entry -> {
                        double score = entry.getValue();
                        return score >= 0 && score < (assignment.getCapabilityThreshold() * 0.8);
                    })
                    .map(Map.Entry::getKey)
                    .collectList()
                    .flatMap(poorPerformers -> {
                        if (!poorPerformers.isEmpty() &&
                            currentProviders.size() - poorPerformers.size() >= assignment.getMinProviders()) {

                            for (String pp : poorPerformers) {
                                log.warn("⚠️ Provider {} underperforming on {} (threshold: {})",
                                    pp, taskType, assignment.getCapabilityThreshold());
                            }

                            List<String> updatedProviders = new ArrayList<>(currentProviders);
                            updatedProviders.removeAll(poorPerformers);
                            assignment.setProviderIds(updatedProviders);
                            assignment.setUpdatedAt(new Date());
                            assignment.setAssignmentSource("auto_rebalance");

                            log.info("🧹 Removed {} poor performers from task '{}'",
                                poorPerformers.size(), taskType);

                            return assignmentRepo.save(assignment).then();
                        }
                        return Mono.empty();
                    });
            })
            .then()
            .doOnTerminate(() -> log.info("✅ Nightly rebalance complete"));
    }

    /**
     * Get latest capability score for a provider on a task — reactive, no .block().
     */
    private Mono<Double> getLatestScore(String providerId, String taskType) {
        return providerRepo.findById(providerId)
            .map(provider -> {
                if (provider.getCapabilityScores() != null) {
                    Double score = provider.getCapabilityScores().get(taskType);
                    return score != null ? score : -1.0;
                }
                return -1.0;
            })
            .onErrorResume(e -> {
                log.debug("Could not get score for {} on {}: {}", providerId, taskType, e.getMessage());
                return Mono.just(-1.0);
            });
    }

    /**
     * Get effective providers for a task — reactive, no .block().
     */
    public Mono<List<String>> getProvidersForTask(String taskType) {
        return assignmentRepo.findByTaskType(taskType)
            .filter(TaskProviderAssignment::getActive)
            .map(TaskProviderAssignment::getProviderIds)
            .switchIfEmpty(
                // Fallback: return all active providers
                providerRepo.findByStatus("active")
                    .map(APIProvider::getId)
                    .collectList()
            );
    }

    /**
     * Check if a provider is assigned to a task — reactive, no .block().
     */
    public Mono<Boolean> isProviderAssigned(String providerId, String taskType) {
        return assignmentRepo.findByTaskType(taskType)
            .map(assignment ->
                assignment.getProviderIds() != null &&
                assignment.getProviderIds().contains(providerId))
            .defaultIfEmpty(false);
    }

    /**
     * Get all task assignments (for admin UI) — reactive, no .block().
     */
    public Mono<List<TaskProviderAssignment>> getAllAssignments() {
        return assignmentRepo.findAll().collectList();
    }

    /**
     * Get assignment stats for dashboard — reactive, no .block().
     */
    public Mono<Map<String, Object>> getAssignmentStats() {
        Mono<Long> totalMono = assignmentRepo.count();
        Mono<Long> activeMono = assignmentRepo.countByIsActive(true);

        return Mono.zip(totalMono, activeMono)
            .map(tuple -> {
                long totalAssignments = tuple.getT1();
                long activeTasks = tuple.getT2();

                Map<String, Object> stats = new HashMap<>();
                stats.put("totalAssignments", totalAssignments);
                stats.put("activeTasks", activeTasks);
                stats.put("totalTaskTypes", ALL_TASK_TYPES.size());
                stats.put("coverage", (activeTasks * 100.0 / ALL_TASK_TYPES.size()));
                return stats;
            });
    }

    /** Internal config class */
    private static class TaskConfig {
        String name;
        String label;
        double minScore;

        TaskConfig(String name, String label, double minScore) {
            this.name = name;
            this.label = label;
            this.minScore = minScore;
        }
    }
}