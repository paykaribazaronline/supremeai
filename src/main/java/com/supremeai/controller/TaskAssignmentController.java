package com.supremeai.controller;

import com.supremeai.model.APIProvider;
import com.supremeai.model.TaskProviderAssignment;
import com.supremeai.repository.ProviderRepository;
import com.supremeai.repository.TaskProviderAssignmentRepository;
import com.supremeai.service.AutomaticTaskAssigner;
import com.supremeai.service.ProviderCapabilityAnalyzer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin controller for managing task-to-provider assignments.
 *
 * Endpoints:
 * - GET /api/admin/tasks - List all task assignments
 * - GET /api/admin/tasks/:taskType - Get assignment for specific task
 * - POST /api/admin/tasks/:taskType/assign - Manually assign providers to task
 * - POST /api/admin/tasks/:taskType/unassign - Remove provider from task
 * - POST /api/admin/tasks/rebalance - Trigger rebalance
 * - GET /api/admin/providers/:id/capabilities - Get provider capability scores
 * - POST /api/admin/providers/:id/benchmark - Trigger benchmark
 */
@RestController
@RequestMapping("/api/admin/tasks")
@PreAuthorize("hasRole('ADMIN')")
public class TaskAssignmentController {

    private static final Logger log = LoggerFactory.getLogger(TaskAssignmentController.class);

    @Autowired
    private TaskProviderAssignmentRepository assignmentRepo;

    @Autowired
    private ProviderRepository providerRepo;

    @Autowired
    private AutomaticTaskAssigner taskAssigner;

    @Autowired
    private ProviderCapabilityAnalyzer capabilityAnalyzer;

    /**
     * Get all task assignments
     */
    @GetMapping
    public Flux<TaskProviderAssignment> getAllAssignments() {
        log.info("📋 Admin requested all task assignments");
        return assignmentRepo.findAll();
    }

    /**
     * Get assignment stats
     */
    @GetMapping("/stats")
    public Mono<ResponseEntity<Map<String, Object>>> getStats() {
        Map<String, Object> stats = taskAssigner.getAssignmentStats();
        return Mono.just(ResponseEntity.ok(stats));
    }

    /**
     * Get assignment for a specific task
     */
    @GetMapping("/{taskType}")
    public Mono<ResponseEntity<TaskProviderAssignment>> getAssignment(
            @PathVariable String taskType) {
        return assignmentRepo.findByTaskType(taskType)
            .map(assignment -> ResponseEntity.ok(assignment))
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Manual assignment: admin assigns specific providers to a task
     */
    @PostMapping("/{taskType}/assign")
    public Mono<ResponseEntity<Map<String, Object>>> assignProviders(
            @PathVariable String taskType,
            @Valid @RequestBody AssignmentRequest request) {

        return assignmentRepo.findByTaskType(taskType)
            .flatMap(existing -> {
                existing.setProviderIds(request.getProviderIds());
                existing.setAssignmentSource("manual");
                existing.setUpdatedAt(new java.util.Date());
                // Validate min/max
                if (existing.getMinProviders() == null) {
                    existing.setMinProviders(3);
                }
                if (existing.getMaxProviders() == null) {
                    existing.setMaxProviders(10);
                }
                return assignmentRepo.save(existing);
            })
            .map(updated -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Task '" + taskType + "' updated with " +
                    request.getProviderIds().size() + " providers");
                response.put("assignment", updated);
                log.info("✏️ Admin manually assigned {} providers to task '{}'",
                    request.getProviderIds().size(), taskType);
                return ResponseEntity.ok(response);
            })
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Auto-assign: let system analyze and assign
     */
    @PostMapping("/{taskType}/auto-assign")
    public Mono<ResponseEntity<Map<String, Object>>> autoAssign(
            @PathVariable String taskType) {

        return providerRepo.findByStatus("active")
            .map(APIProvider::getId)
            .collectList()
            .flatMap(providerIds -> {
                Map<String, Object> response = new HashMap<>();
                response.put("taskType", taskType);
                response.put("activeProviders", providerIds.size());
                // Trigger re-evaluation
                taskAssigner.nightlyRebalance();
                response.put("message", "Auto-assignment triggered for task '" + taskType + "'");
                response.put("success", true);
                return Mono.just(ResponseEntity.ok(response));
            });
    }

    /**
     * Get capability scores for a specific provider
     */
    @GetMapping("/providers/{providerId}/capabilities")
    public Mono<ResponseEntity<Map<String, Object>>> getProviderCapabilities(
            @PathVariable String providerId) {

        return providerRepo.findById(providerId)
            .map(provider -> {
                Map<String, Object> response = new HashMap<>();
                response.put("providerId", provider.getId());
                response.put("providerName", provider.getName());
                response.put("capabilityScores",
                    provider.getCapabilityScores() != null ?
                        provider.getCapabilityScores() : Map.of());
                response.put("benchmarkedAt", provider.getLastBenchmarkedAt());
                response.put("benchmarkCount", provider.getBenchmarkCount());
                return ResponseEntity.ok(response);
            })
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Trigger manual benchmark for a provider
     */
    @PostMapping("/providers/{providerId}/benchmark")
    public Mono<ResponseEntity<Map<String, Object>>> triggerBenchmark(
            @PathVariable String providerId) {

        return capabilityAnalyzer.benchmarkProvider(providerId)
            .map(scores -> {
                Map<String, Object> response = new HashMap<>();
                response.put("providerId", providerId);
                response.put("scores", scores);
                response.put("message", "Benchmark complete for " + providerId);
                response.put("success", true);

                // Auto-assign based on scores
                taskAssigner.autoAssignNewProvider(providerId, scores).subscribe();

                log.info("📊 Manual benchmark complete for {}: {} scores",
                    providerId, scores.size());

                return ResponseEntity.ok(response);
            });
    }

    /**
     * Trigger full rebalance across all tasks
     */
    @PostMapping("/rebalance")
    public Mono<ResponseEntity<Map<String, Object>>> triggerRebalance() {
        taskAssigner.nightlyRebalance();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Rebalance triggered");
        response.put("success", true);
        log.info("🔄 Admin triggered global rebalance");

        return Mono.just(ResponseEntity.ok(response));
    }

    /**
     * Get list of all providers (for assignment UI)
     */
    @GetMapping("/providers")
    public Flux<Map<String, Object>> getAllProviders() {
        return providerRepo.findAll()
            .map(provider -> {
                Map<String, Object> info = new HashMap<>();
                info.put("id", provider.getId());
                info.put("name", provider.getName());
                info.put("status", provider.getStatus());
                info.put("capabilities", provider.getCapabilityScores());
                info.put("canParticipate", provider.isCanParticipateInVoting());
                return info;
            });
    }

    /** Request DTO */
    public static class AssignmentRequest {
        @NotEmpty(message = "Provider IDs list cannot be empty")
        private List<String> providerIds;

        private Integer minProviders;
        private Integer maxProviders;
        private String votingStrategy;

        public List<String> getProviderIds() { return providerIds; }
        public void setProviderIds(List<String> providerIds) { this.providerIds = providerIds; }
        public Integer getMinProviders() { return minProviders; }
        public void setMinProviders(Integer minProviders) { this.minProviders = minProviders; }
        public Integer getMaxProviders() { return maxProviders; }
        public void setMaxProviders(Integer maxProviders) { this.maxProviders = maxProviders; }
        public String getVotingStrategy() { return votingStrategy; }
        public void setVotingStrategy(String votingStrategy) { this.votingStrategy = votingStrategy; }
    }
}