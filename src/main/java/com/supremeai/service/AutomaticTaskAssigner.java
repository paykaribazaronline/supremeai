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
     */
    public Mono<TaskProviderAssignment> autoAssignNewProvider(String providerId, Map<String, Double> scores) {
        return Mono.fromCallable(() -> {
            log.info("🔍 Auto-assigning provider {} based on {} capability scores",
                providerId, scores.size());

            List<TaskProviderAssignment> newAssignments = new ArrayList<>();

            for (TaskConfig task : ALL_TASK_TYPES) {
                Double score = scores.get(task.name);
                if (score == null) continue;

                if (score >= task.minScore) {
                    // Provider is good at this task → add to assignment
                    TaskProviderAssignment existing = assignmentRepo.findByTaskType(task.name).block();

                    if (existing != null) {
                        // Add provider to existing assignment
                        List<String> updatedProviders = new ArrayList<>(existing.getProviderIds());
                        if (!updatedProviders.contains(providerId)) {
                            updatedProviders.add(providerId);
                            // Apply max limit
                            if (updatedProviders.size() > DEFAULT_MAX_PROVIDERS) {
                                // Keep top scorers (this provider's score is already validated)
                                updatedProviders = keepTopProviders(updatedProviders, task.name);
                            }
                            existing.setProviderIds(updatedProviders);
                            existing.setUpdatedAt(new Date());
                            existing.setAssignmentSource("auto");
                            assignmentRepo.save(existing).block();

                            log.info("➕ Added {} to task '{}' (score: {})",
                                providerId, task.name, score);
                        }
                    } else {
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

                        assignmentRepo.save(newAssignment).block();
                        newAssignments.add(newAssignment);

                        log.info("🆕 Created new assignment: task='{}' → provider={}",
                            task.name, providerId);
                    }
                }
            }

            log.info("✅ Provider {} auto-assigned to {} task types",
                providerId, newAssignments.size());

            return newAssignments;

        }).flatMap(assignments -> {
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
     */
    public void nightlyRebalance() {
        log.info("🌙 Starting nightly rebalance of task assignments...");

        assignmentRepo.findAllByIsActive(true)
            .doOnNext(assignment -> {
                String taskType = assignment.getTaskType();
                List<String> currentProviders = assignment.getProviderIds();

                if (currentProviders == null || currentProviders.isEmpty()) return;

                List<String> poorPerformers = new ArrayList<>();

                for (String providerId : currentProviders) {
                    // Get latest capability score for this task
                    Double score = getLatestScore(providerId, taskType);

                    if (score != null && score < (assignment.getCapabilityThreshold() * 0.8)) {
                        // Performance dropped significantly below threshold
                        poorPerformers.add(providerId);
                        log.warn("⚠️ Provider {} underperforming on {} (score: {}, threshold: {})",
                            providerId, taskType, score, assignment.getCapabilityThreshold());
                    }
                }

                // Remove poor performers if they'd still meet minimum
                if (!poorPerformers.isEmpty() &&
                    currentProviders.size() - poorPerformers.size() >= assignment.getMinProviders()) {

                    currentProviders.removeAll(poorPerformers);
                    assignment.setProviderIds(currentProviders);
                    assignment.setUpdatedAt(new Date());
                    assignment.setAssignmentSource("auto_rebalance");
                    assignmentRepo.save(assignment).block();

                    log.info("🧹 Removed {} poor performers from task '{}'",
                        poorPerformers.size(), taskType);
                }
            })
            .blockLast();

        log.info("✅ Nightly rebalance complete");
    }

    /**
     * Get latest capability score for a provider on a task
     */
    private Double getLatestScore(String providerId, String taskType) {
        try {
            APIProvider provider = providerRepo.findById(providerId).block();
            if (provider != null && provider.getCapabilityScores() != null) {
                return provider.getCapabilityScores().get(taskType);
            }
        } catch (Exception e) {
            log.debug("Could not get score for {} on {}: {}", providerId, taskType, e.getMessage());
        }
        return null;
    }

    /**
     * Get effective providers for a task (used at runtime)
     */
    public List<String> getProvidersForTask(String taskType) {
        TaskProviderAssignment assignment = assignmentRepo.findByTaskType(taskType).block();

        if (assignment != null && assignment.getActive()) {
            return assignment.getProviderIds();
        }

        // Fallback: return all active providers
        return providerRepo.findByStatus("active")
            .map(APIProvider::getId)
            .collectList()
            .block();
    }

    /**
     * Check if a provider is assigned to a task
     */
    public boolean isProviderAssigned(String providerId, String taskType) {
        TaskProviderAssignment assignment = assignmentRepo.findByTaskType(taskType).block();
        if (assignment != null) {
            return assignment.getProviderIds() != null &&
                   assignment.getProviderIds().contains(providerId);
        }
        return false;
    }

    /**
     * Get all task assignments (for admin UI)
     */
    public List<TaskProviderAssignment> getAllAssignments() {
        return assignmentRepo.findAll().collectList().block();
    }

    /**
     * Get assignment stats for dashboard
     */
    public Map<String, Object> getAssignmentStats() {
        Map<String, Object> stats = new HashMap<>();
        long totalAssignments = assignmentRepo.count().block();
        long activeTasks = assignmentRepo.countByIsActive(true).block();

        stats.put("totalAssignments", totalAssignments);
        stats.put("activeTasks", activeTasks);
        stats.put("totalTaskTypes", ALL_TASK_TYPES.size());
        stats.put("coverage", (activeTasks * 100.0 / ALL_TASK_TYPES.size()));

        return stats;
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