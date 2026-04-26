package com.supremeai.learning.knowledge;

import com.supremeai.admin.AdminDashboardService;
import com.supremeai.admin.ImprovementProposal;
import com.supremeai.repository.SolutionMemoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class GlobalKnowledgeBase {

    private static final Logger log = LoggerFactory.getLogger(GlobalKnowledgeBase.class);
    private final Map<String, List<SolutionMemory>> globalMemory = new ConcurrentHashMap<>();

    @Autowired
    private AdminDashboardService adminDashboard;

    @Autowired(required = false)
    private SolutionMemoryRepository solutionMemoryRepository;

    /**
     * Load memories from Firestore on startup.
     * Uses reactive repository with blocking call suitable for @PostConstruct.
     */
    public void loadMemories() {
        if (solutionMemoryRepository == null) {
            log.warn("SolutionMemoryRepository not available, using in-memory only");
            return;
        }

         try {
             List<SolutionMemory> allSolutions = solutionMemoryRepository.findAll()
                     .timeout(Duration.ofSeconds(10))
                     .collectList()
                     .block();

            if (allSolutions != null) {
                for (SolutionMemory solution : allSolutions) {
                    String errorSignature = solution.getTriggerError();
                    if (errorSignature == null) continue;

                    globalMemory.computeIfAbsent(errorSignature, k -> new ArrayList<>())
                               .add(solution);
                }
                log.info("Loaded {} solution memories from Firestore", allSolutions.size());
            }
        } catch (Exception e) {
            log.error("Failed to load memories from Firestore: {}", e.getMessage(), e);
        }
    }

    /**
     * Save a solution memory to Firestore.
     */
    private void saveMemory(SolutionMemory memory) {
        if (solutionMemoryRepository == null) {
            log.warn("SolutionMemoryRepository not available, skipping Firestore save");
            return;
        }

        try {
            solutionMemoryRepository.save(memory)
                    .timeout(Duration.ofSeconds(5))
                    .doOnSuccess(saved -> log.debug("Saved solution memory to Firestore: {}", saved.getId()))
                    .doOnError(err -> log.error("Failed to save solution memory: {}", err.getMessage()))
                    .subscribe();
        } catch (Exception e) {
            log.error("Exception while saving solution memory: {}", e.getMessage(), e);
        }
    }

    /**
     * Records a successful fix, checking with Admin Dashboard first for new solutions.
     * Existing solutions are updated with versioning.
     */
    public void recordSuccessWithPermission(String errorSignature, String successfulCode, String aiProvider,
                              long executionTimeMs, double securityScore) {

        List<SolutionMemory> solutions = globalMemory.computeIfAbsent(errorSignature, k -> new ArrayList<>());

        // Check if we already have this exact solution
        for (SolutionMemory solution : solutions) {
            if (solution.getResolvedCode().equals(successfulCode)) {
                if (solution.isObsolete()) {
                    // Un-obsolete if it was previously marked obsolete (revival)
                    solution.setObsolete(false);
                    solution.setObsoleteReason(null);
                    solution.setObsoletedAt(null);
                    log.info("[Knowledge Base] Revived previously obsolete solution by {}", aiProvider);
                }

                // Create a new version for update (versioning + rollback support)
                SolutionMemory updated = solution.createUpdate(
                    successfulCode, // Same code but new version captures updated counters
                    executionTimeMs,
                    securityScore
                );
                updated.setId(null); // Let Firestore generate new ID for version
                updated.setPreviousVersionId(solution.getId());

                // Transfer counters from previous version to base
                updated.setSuccessCount(solution.getSuccessCount() + 1);
                updated.setFailureCount(solution.getFailureCount());

                solutions.add(updated);
                log.info("[Knowledge Base] Updated solution (new version: v{}) by {}", updated.getVersion(), aiProvider);
                saveMemory(updated);
                return;
            }
        }

        // New solution requires admin approval (or auto-pilot)
        ImprovementProposal proposal = new ImprovementProposal(
            "Learn new fix for " + errorSignature,
            "AI Provider " + aiProvider + " successfully fixed this error. Should I add it to Global Memory?",
            "KNOWLEDGE_BASE",
            successfulCode
        );

        boolean isApprovedImmediately = adminDashboard.submitImprovement(proposal);

        if (isApprovedImmediately) {
            SolutionMemory newMemory = new SolutionMemory(
                errorSignature,
                successfulCode,
                aiProvider,
                executionTimeMs,
                securityScore
            );
            solutions.add(newMemory);
            log.info("[Knowledge Base] Learned NEW solution automatically!");
            saveMemory(newMemory);
        } else {
            log.info("[Knowledge Base] Solution pending. Waiting for Admin to approve in the Dashboard.");
        }
    }

    /**
     * Record a failure for an existing solution to help refine scoring.
     * Creates a new version to preserve history.
     */
    public void recordFailure(String errorSignature, String failedCode) {
        List<SolutionMemory> solutions = globalMemory.get(errorSignature);
        if (solutions != null) {
            for (SolutionMemory solution : solutions) {
                if (solution.getResolvedCode().equals(failedCode)) {
                    if (solution.isObsolete()) {
                        log.debug("Ignoring failure for obsolete solution: {}", failedCode);
                        return;
                    }

                    // Create new version with updated failure count
                    SolutionMemory updated = solution.createUpdate(
                        failedCode,
                        solution.getExecutionTimeMs(),
                        solution.getSecurityScore()
                    );
                    updated.setSuccessCount(solution.getSuccessCount());
                    updated.setFailureCount(solution.getFailureCount() + 1);

                    solutions.add(updated);
                    log.warn("[Knowledge Base] Penalized a solution that failed in production (v{})", updated.getVersion());
                    saveMemory(updated);
                    return;
                }
            }
        }
    }

    /**
     * Find the best known solution for an error signature.
     * Returns null if no solution meets minimum confidence threshold.
     */
    public String findKnownSolution(String errorSignature) {
        // Check in-memory cache first
        List<SolutionMemory> solutions = globalMemory.get(errorSignature);
        if (solutions == null || solutions.isEmpty()) {
            // Try loading from Firestore if available
            loadSolutionsFromFirestore(errorSignature);
            solutions = globalMemory.get(errorSignature);
        }

        if (solutions == null || solutions.isEmpty()) return null;

        // Sort by supreme score descending
        solutions.sort((s1, s2) -> Double.compare(s2.calculateSupremeScore(), s1.calculateSupremeScore()));
        SolutionMemory bestSolution = solutions.get(0);

        if (bestSolution.calculateSupremeScore() < 0.4) return null;

        return bestSolution.getResolvedCode();
    }

    /**
     * Load solutions for a specific error from Firestore into memory cache.
     */
    private void loadSolutionsFromFirestore(String errorSignature) {
        if (solutionMemoryRepository == null) return;

        try {
            List<SolutionMemory> loaded = solutionMemoryRepository
                    .findByTriggerError(errorSignature)
                    .collectList()
                    .block();

            if (loaded != null && !loaded.isEmpty()) {
                List<SolutionMemory> existing = globalMemory.computeIfAbsent(errorSignature, k -> new ArrayList<>());
                existing.addAll(loaded);
                log.debug("Loaded {} solutions for error signature {} from Firestore", loaded.size(), errorSignature);
            }
        } catch (Exception e) {
            log.error("Failed to load solutions from Firestore for {}: {}", errorSignature, e.getMessage());
        }
    }

    /**
     * Get all solutions for a given error signature.
     */
    public List<SolutionMemory> getSolutions(String errorSignature) {
        return globalMemory.getOrDefault(errorSignature, Collections.emptyList());
    }
}
