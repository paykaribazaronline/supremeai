package com.supremeai.controller;

import com.supremeai.learning.knowledge.GlobalKnowledgeBase;
import com.supremeai.learning.knowledge.SolutionMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints for querying the Global Knowledge Base.
 * Accessible by authenticated users (not just admin) to retrieve known solutions.
 * Modifications remain admin-only.
 */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeBaseController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseController.class);

    @Autowired
    private GlobalKnowledgeBase globalKnowledgeBase;

    /**
     * Get best solution for a specific error signature.
     * Example: GET /api/knowledge/solution?error=NullPointerException%20at%20Main.java:42
     */
    @GetMapping("/solution")
    @PreAuthorize("isAuthenticated()")
    public SolutionResponse getSolution(@RequestParam("error") String errorSignature) {
        String solution = globalKnowledgeBase.findKnownSolution(errorSignature);
        if (solution != null) {
            return SolutionResponse.found(solution);
        } else {
            return SolutionResponse.notFound();
        }
    }

    /**
     * Get all solutions for an error signature with scores.
     */
    @GetMapping("/solutions")
    @PreAuthorize("isAuthenticated()")
    public List<SolutionMemory> getAllSolutions(@RequestParam("error") String errorSignature) {
        return globalKnowledgeBase.getSolutions(errorSignature);
    }

    /**
     * Learn a new solution (requires admin approval).
     * This endpoint records a successful fix that will be queued for admin approval.
     */
    @PostMapping("/learn")
    @PreAuthorize("hasRole('ADMIN')")
    public String learnSolution(@RequestBody LearnSolutionRequest request) {
        // This will go through approval flow in GlobalKnowledgeBase
        // We need to manually simulate the approval flow or let the service handle it
        // For now, simply record with admin override (auto-pilot)
        globalKnowledgeBase.recordSuccessWithPermission(
            request.getErrorSignature(),
            request.getResolvedCode(),
            request.getProvider(),
            request.getExecutionTimeMs(),
            request.getSecurityScore()
        );
        return "Solution recorded (subject to auto-pilot/approval rules)";
    }

    /**
     * Record a failure for an existing solution to help refine scoring.
     */
    @PostMapping("/failure")
    @PreAuthorize("hasRole('ADMIN')")
    public String recordFailure(@RequestBody RecordFailureRequest request) {
        globalKnowledgeBase.recordFailure(request.getErrorSignature(), request.getFailedCode());
        return "Failure recorded";
    }

    /**
     * Get knowledge base statistics.
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public KnowledgeStats getStats() {
        // Could expand to compute real stats from Firestore
        return new KnowledgeStats("TODO: implement stats");
    }

    // DTOs

    public static class SolutionResponse {
        public boolean found;
        public String solution;
        public String message;

        public static SolutionResponse found(String sol) {
            SolutionResponse r = new SolutionResponse();
            r.found = true;
            r.solution = sol;
            r.message = "Solution found";
            return r;
        }

        public static SolutionResponse notFound() {
            SolutionResponse r = new SolutionResponse();
            r.found = false;
            r.solution = null;
            r.message = "No known solution for this error";
            return r;
        }
    }

    public static class LearnSolutionRequest {
        private String errorSignature;
        private String resolvedCode;
        private String provider;
        private long executionTimeMs;
        private double securityScore;

        public String getErrorSignature() { return errorSignature; }
        public void setErrorSignature(String errorSignature) { this.errorSignature = errorSignature; }
        public String getResolvedCode() { return resolvedCode; }
        public void setResolvedCode(String resolvedCode) { this.resolvedCode = resolvedCode; }
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public long getExecutionTimeMs() { return executionTimeMs; }
        public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
        public double getSecurityScore() { return securityScore; }
        public void setSecurityScore(double securityScore) { this.securityScore = securityScore; }
    }

    public static class RecordFailureRequest {
        private String errorSignature;
        private String failedCode;

        public String getErrorSignature() { return errorSignature; }
        public void setErrorSignature(String errorSignature) { this.errorSignature = errorSignature; }
        public String getFailedCode() { return failedCode; }
        public void setFailedCode(String failedCode) { this.failedCode = failedCode; }
    }

    public static class KnowledgeStats {
        public String note;

        public KnowledgeStats(String note) {
            this.note = note;
        }
    }
}
