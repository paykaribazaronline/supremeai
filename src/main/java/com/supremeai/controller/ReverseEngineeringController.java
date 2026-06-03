package com.supremeai.controller;

import com.supremeai.service.PubSubPublisherService;
import com.supremeai.service.ReverseEngineeringIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for website reverse engineering feature.
 * Submits jobs to Pub/Sub queue for async processing by Python FastAPI worker.
 */
@RestController
@RequestMapping("/api/reverse-engineer")
public class ReverseEngineeringController {
    public ReverseEngineeringController(ReverseEngineeringIntegrationService integrationService) {
        this.integrationService = integrationService;
    }


    private static final Logger logger = LoggerFactory.getLogger(ReverseEngineeringController.class);
    

    @PostMapping("/submit")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Map<String, Object>>> submitReverseEngineering(
            @RequestBody Map<String, Object> request,
            Authentication auth) {
        
        String url = (String) request.get("url");
        String taskType = (String) request.getOrDefault("taskType", "REVERSE_ENGINEER");
        String customInstructions = (String) request.get("customInstructions");
        String userId = auth != null ? auth.getName() : "admin";
        
        Map<String, Object> extraParams = new HashMap<>(request);
        extraParams.remove("url");
        extraParams.remove("taskType");
        extraParams.remove("customInstructions");

        return integrationService.startJob(userId, url, taskType, customInstructions, extraParams)
            .map(job -> {
                Map<String, Object> response = new HashMap<>();
                response.put("jobId", job.getJobId());
                response.put("status", job.getStatus());
                response.put("message", "Reverse engineering job queued via service layer");
                return ResponseEntity.ok(response);
            });
    }

    /**
     * Get history of reverse engineering jobs.
     * Frontend: GET /api/reverse-engineer/history?limit=50
     */
    @GetMapping("/history")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Map<String, Object>>> getHistory(
            @RequestParam(defaultValue = "50") int limit,
            Authentication auth) {
        
        // Query recent jobs from Firestore
        return integrationService.getRecentJobs(limit)
            .map(jobs -> {
                List<Map<String, Object>> jobList = jobs.stream()
                    .map(job -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("jobId", job.getJobId());
                        m.put("url", job.getWebsiteUrl());
                        m.put("status", job.getStatus());
                        m.put("submittedAt", job.getCreatedAt());
                        m.put("startedAt", job.getStartedAt() != null ? job.getStartedAt() : job.getCreatedAt());
                        m.put("completedAt", job.getUpdatedAt());
                        m.put("error", job.getErrorMessage());
                        
                        // Progress & phase based on status
                        int progress = "COMPLETED".equals(job.getStatus()) ? 100 :
                                       "FAILED".equals(job.getStatus()) ? 0 :
                                       "PENDING".equals(job.getStatus()) ? 0 : 50;
                        m.put("progress", progress);
                        m.put("currentPhase", job.getStatus());
                        
                        // Results structure
                        Map<String, Object> results = new HashMap<>();
                        results.put("endpoints", job.getDiscoveredApis() != null ? job.getDiscoveredApis() : List.of());
                        results.put("observation", Map.of());
                        results.put("auth", Map.of());
                        results.put("connectors", Map.of());
                        m.put("results", results);
                        
                        return m;
                    })
                    .collect(Collectors.toList());
                Map<String, Object> response = new HashMap<>();
                response.put("jobs", jobList);
                response.put("total", jobList.size());
                return ResponseEntity.ok(response);
            })
            .defaultIfEmpty(ResponseEntity.ok(Map.of("jobs", List.of(), "total", 0)));
    }

    /**
     * Delete/cancel a reverse engineering job.
     * Frontend: DELETE /api/reverse-engineer/job/{jobId}
     */
    @DeleteMapping("/job/{jobId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> cancelJob(
            @PathVariable String jobId,
            Authentication auth) {
        
        // In production: update job status to CANCELLED in Firestore
        logger.info("Job cancellation requested: {} by user {}", jobId, auth != null ? auth.getName() : "unknown");
        
        Map<String, Object> response = new HashMap<>();
        response.put("jobId", jobId);
        response.put("status", "CANCELLED");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/job/{jobId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getJobStatus(
            @PathVariable String jobId,
            Authentication auth) {
        
        String userId = auth != null ? auth.getName() : null;
        
        // Query job status from Firestore via integration service
        return ResponseEntity.ok(Map.of(
            "jobId", jobId,
            "status", "PENDING",
            "message", "Job status endpoint - implementation depends on Firestore query"
        ));
    }

    @PostMapping("/job/{jobId}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> completeJob(
            @PathVariable String jobId,
            @RequestBody Map<String, Object> result,
            Authentication auth) {
        
        // Called by Python worker when job completes (or admin to manually mark complete)
        String userId = auth != null ? auth.getName() : "system";
        
        @SuppressWarnings("unchecked")
        Map<String, Object> discoveredApis = (Map<String, Object>) result.get("discoveredApis");
        if (discoveredApis == null) discoveredApis = Map.of();
        
        integrationService.completeJob(jobId, discoveredApis)
            .subscribe(
                savedJob -> logger.info("Job {} marked completed by {}", jobId, userId),
                error -> logger.error("Failed to complete job {}: {}", jobId, error.getMessage())
            );
        
        return ResponseEntity.ok(Map.of("jobId", jobId, "status", "COMPLETED"));
    }

    /**
     * Manually trigger integration: generate app from completed reverse engineering job.
     */
    @PostMapping("/integrate/{jobId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> integrateWithCodeGen(
            @PathVariable String jobId,
            Authentication auth) {
        
        String userId = auth != null ? auth.getName() : "anonymous";
        
        integrationService.onJobCompletion(jobId, userId)
            .subscribe(
                job -> {
                    logger.info("Integration triggered for job {}: generated app {}", jobId, job.getGeneratedAppId());
                },
                error -> {
                    logger.error("Integration failed for job {}: {}", jobId, error.getMessage());
                }
            );
        
        return ResponseEntity.ok(Map.of(
            "jobId", jobId,
            "status", "INTEGRATING",
            "message", "Code generation started from reverse engineering results"
        ));
    }
}
