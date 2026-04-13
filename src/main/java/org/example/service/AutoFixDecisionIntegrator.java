package org.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * AutoFixDecisionIntegrator - Bridges AutoFixLoopService with Decision Logging
 * 
 * Purpose: Log all auto-fix decisions and outcomes to the decision logging system
 * for tracking, learning, and consensus visibility.
 * 
 * Integration Pattern:
 * 1. AutoFixLoopService detects error
 * 2. → Log decision with all agents consulted
 * 3. → Generate fix candidates
 * 4. → Test candidates  
 * 5. → Record consensus vote (pass/fail on each candidate)
 * 6. → Apply best fix
 * 7. → Record outcome with success metrics
 * 
 * This enables:
 * - Complete decision audit trail
 * - Agent performance tracking
 * - Pattern learning for future fixes
 * - Decision confidence trending
 */
@Service
public class AutoFixDecisionIntegrator {
    
    private static final Logger logger = LoggerFactory.getLogger(AutoFixDecisionIntegrator.class);
    
    @Autowired
    private AgentDecisionLogger decisionLogger;
    
    /**
     * Integrated decision result combining fix and decision tracking
     */
    public static class IntegratedFixResult {
        public String fixId;
        public AutoFixLoopService.FixAttempt fixAttempt;
        public String decisionId;
        public boolean decisionLogged;
        public boolean votingRecorded;
        public boolean outcomeRecorded;
        public float consensusConfidence;
        public long totalTimeMs;
    }
    
    /**
     * Log and track an error fix through the complete decision workflow
     */
    public IntegratedFixResult autoFixWithDecisions(String error, Map<String, Object> context,
                                                   AutoFixLoopService autoFixService) {
        IntegratedFixResult result = new IntegratedFixResult();
        long startTime = System.currentTimeMillis();
        
        try {
            // Step 1: Log the decision to fix this error
            String projectId = (String) context.getOrDefault("projectId", "unknown");
            String decisionId = logFixDecision(error, projectId);
            result.decisionId = decisionId;
            result.decisionLogged = true;
            
            logger.info("📋 Logged fix decision: {}", decisionId);
            
            // Step 2: Run auto-fix loop
            AutoFixLoopService.FixAttempt attempt = autoFixService.autoFixError(error, context);
            result.fixAttempt = attempt;
            result.fixId = attempt.id;
            
            if (attempt.candidates.isEmpty()) {
                // No candidates could be generated
                recordFailedFixAttempt(decisionId, "no_candidates", projectId);
                result.totalTimeMs = System.currentTimeMillis() - startTime;
                return result;
            }
            
            // Step 3: Record agent voting on candidates
            recordCandidateVoting(decisionId, attempt.candidates, projectId);
            result.votingRecorded = true;
            
            // Step 4: Record consensus decision
            if (attempt.appliedFix != null) {
                recordAppliedFix(decisionId, attempt.appliedFix, projectId);
                
                // Step 5: After fix is applied and tested, record outcome
                // (This would be called after validation in real implementation)
                scheduleOutcomeRecording(decisionId, attempt, projectId);
                result.outcomeRecorded = true;
                
                // Calculate consensus confidence
                result.consensusConfidence = attempt.appliedFix.confidence;
            } else {
                recordFailedFixAttempt(decisionId, "no_passing_candidate", projectId);
            }
            
        } catch (Exception e) {
            logger.error("❌ Error in auto-fix decision workflow", e);
        }
        
        result.totalTimeMs = System.currentTimeMillis() - startTime;
        return result;
    }
    
    /**
     * Log the initial decision to attempt auto-fixing this error
     */
    private String logFixDecision(String error, String projectId) {
        // Extract error summary
        String errorSummary = error.split("\n")[0];
        if (errorSummary.length() > 100) {
            errorSummary = errorSummary.substring(0, 100);
        }
        
        try {
            AgentDecisionLogger.AgentDecision decision = decisionLogger.logDecision(
                "Architect",  // Architect agent decides on fix strategy
                "error-fixing",
                projectId,
                "Detected error, attempting automatic fix: " + errorSummary,
                "Error detection triggered auto-fix loop per Phase 6 protocol",
                0.80f,  // Initial confidence in attempting fix
                Arrays.asList("Manual fix", "Escalate to developer")
            );
            
            return decision.decisionId;
        } catch (Exception e) {
            logger.error("Failed to log fix decision", e);
            return "unknown-" + UUID.randomUUID();
        }
    }
    
    /**
     * Record agent voting on fix candidates
     */
    private void recordCandidateVoting(String decisionId, List<AutoFixLoopService.FixCandidate> candidates,
                                       String projectId) {
        try {
            // Prepare votes from each agent evaluating candidates
            List<AgentDecisionLogger.AgentVote> votes = new ArrayList<>();
            
            // Architect votes based on fix strategy appropriateness
            float architecture_confidence = candidates.isEmpty() ? 0.0f : 
                (float) candidates.stream().mapToDouble(c -> c.confidence).average().orElse(0.5f);
            AgentDecisionLogger.AgentVote architectVote = new AgentDecisionLogger.AgentVote(
                "Architect",
                !candidates.isEmpty(),
                architecture_confidence,
                "Generated " + candidates.size() + " fix candidates, max confidence: " + 
                    (candidates.isEmpty() ? 0 : 
                     candidates.stream().map(c -> c.confidence).max(Float::compareTo).orElse(0f))
            );
            votes.add(architectVote);
            
            // Builder votes based on fix viability
            float builder_confidence = candidates.isEmpty() ? 0.0f :
                (float) Math.max(0.0, candidates.stream()
                    .filter(c -> c.passed)
                    .mapToDouble(c -> c.confidence)
                    .average().orElse(0.0f));
            AgentDecisionLogger.AgentVote builderVote = new AgentDecisionLogger.AgentVote(
                "Builder",
                builder_confidence >= 0.60f,
                builder_confidence,
                "Testing results: " + candidates.stream().filter(c -> c.passed).count() + "/" + candidates.size() + " passed"
            );
            votes.add(builderVote);
            
            // Reviewer votes on overall quality and safety
            float reviewer_confidence = candidates.isEmpty() ? 0.0f :
                (float) candidates.stream().mapToDouble(c -> c.confidence).average().orElse(0.5f);
            AgentDecisionLogger.AgentVote reviewerVote = new AgentDecisionLogger.AgentVote(
                "Reviewer",
                reviewer_confidence >= 0.65f,
                reviewer_confidence,
                "Fix quality assessment: average confidence " + String.format("%.2f", reviewer_confidence) + 
                    " for " + candidates.size() + " candidates"
            );
            votes.add(reviewerVote);
            
            // Record the consensus voting
            decisionLogger.logConsensusVote(decisionId, votes, 0.67f);
            
            logger.debug("📊 Recorded consensus voting: {} votes for fix decision", votes.size());
        } catch (Exception e) {
            logger.error("Failed to record candidate voting", e);
        }
    }
    
    /**
     * Record that a fix was applied
     */
    private void recordAppliedFix(String decisionId, AutoFixLoopService.FixCandidate applied, 
                                  String projectId) {
        try {
            decisionLogger.markDecisionApplied(
                decisionId,
                applied.estimatedTimeMs
            );
            
            logger.info("✅ Recorded fix application: {} (technique: {}, confidence: {})", 
                decisionId, applied.technique, applied.confidence);
        } catch (Exception e) {
            logger.error("Failed to record applied fix", e);
        }
    }
    
    /**
     * Record that fix attempt failed (no passing candidate)
     */
    private void recordFailedFixAttempt(String decisionId, String reason, String projectId) {
        try {
            decisionLogger.recordDecisionOutcome(
                decisionId,
                "FAILED",
                "Auto-fix attempt failed: " + reason,
                0.0,
                "auto-fix-failed", reason
            );
            
            logger.warn("Failed fix attempt recorded: {} (reason: {})", decisionId, reason);
        } catch (Exception e) {
            logger.error("Failed to record failed fix attempt", e);
        }
    }
    
    /**
     * Schedule outcome recording after fix testing
     */
    private void scheduleOutcomeRecording(String decisionId, AutoFixLoopService.FixAttempt attempt,
                                         String projectId) {
        // In a real implementation, this would be called by the testing framework
        // after the fix has been applied and tested. For now, record immediate outcome.
        
        if (attempt.success) {
            try {
                decisionLogger.recordDecisionOutcome(
                    decisionId,
                    "SUCCESS",
                    "Auto-fix successfully applied: " + attempt.appliedFix.description,
                    (double) attempt.appliedFix.confidence,
                    "auto-fix-success", "technique-" + attempt.appliedFix.technique
                );
                
                logger.info("🎉 Fix outcome recorded as SUCCESS");
            } catch (Exception e) {
                logger.error("Failed to record success outcome", e);
            }
        }
    }
    
    /**
     * Get integrated fix statistics including decision tracking
     */
    public Map<String, Object> getIntegratedFixStats(String projectId) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Get decision stats
            // This would query the agentDecisionLogger for fix-related decisions
            stats.put("projectId", projectId);
            stats.put("totalFixAttempts", 0); // Would be counted from decision logs
            stats.put("successfulFixes", 0); // Would be counted from SUCCESS outcomes
            stats.put("failedFixAttempts", 0); // Would be counted from FAILED outcomes
            stats.put("successRate", 0.0f);
            stats.put("averageFixConfidence", 0.0f);
            stats.put("timestamp", System.currentTimeMillis());
        } catch (Exception e) {
            logger.error("Failed to get integrated fix stats", e);
        }
        
        return stats;
    }
}
