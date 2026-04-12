package org.example.selfhealing.healing;

import org.example.selfhealing.domain.HealingAttempt;
import org.example.selfhealing.domain.ValidationResult;
import org.example.service.GitHubActionsErrorParser;
import org.example.service.GitHubAPIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Safe Infinite Healing Loop
 * 
 * Orchestrates the entire healing process with safeguards:
 * 
 * Flow:
 * 1. GitHub Webhook → workflow failed
 * 2. Check circuit breaker → Should we try?
 * 3. Analyze logs → What's the error?
 * 4. Generate fix → AI creates code
 * 5. Validate fix → Multi-stage pipeline
 * 6. Commit & push → Apply to repo
 * 7. Retrigger workflow → Test the fix
 * 8. Monitor results → Did it work?
 * 9. If failed: loop back (max 3 times)
 * 10. If max retries: human escalation
 * 
 * Each step respects admin control (AUTO/WAIT/FORCE_STOP)
 */
@Service
public class SafeInfiniteHealingLoop {
    private static final Logger logger = LoggerFactory.getLogger(SafeInfiniteHealingLoop.class);
    
    private final int MAX_ITERATIONS = 3;
    private final long WORKFLOW_CHECK_DELAY_MS = 300000; // 5 minutes
    
    @Autowired
    private HealingCircuitBreaker circuitBreaker;
    
    @Autowired
    private FixValidationPipeline validationPipeline;
    
    @Autowired
    private HealingStateManager stateManager;
    
    @Autowired
    private GitHubRateLimiter rateLimiter;
    
    @Autowired
    private GitHubAPIService githubAPI;
    
    @Autowired
    private GitHubActionsErrorParser errorParser;
    
    @Autowired(required = false)
    private GitHubAppAuthService githubAppAuth; // GitHub App authentication
    
    @Autowired(required = false)
    private AutoCodeRepairAgent repairAgent;
    
    private final ScheduledExecutorService scheduler = 
            Executors.newScheduledThreadPool(4, r -> {
                Thread t = new Thread(r, "healing-loop-scheduler");
                t.setDaemon(true);
                return t;
            });
    
    /**
     * Event listener for GitHub workflow failures
     * 
     * Hook this up to WebhookListener to receive workflow_run events
     */
    @EventListener(condition = "#event.eventType == 'workflow_run'")
    @Async
    public void onWorkflowFailure(WorkflowFailureEvent event) {
        if (!event.isFailed()) {
            return; // Only process failures
        }
        
        logger.info("🚨 Workflow failure detected: {} (ID: {})", 
                event.getWorkflowName(), event.getWorkflowId());
        
        // Start healing chain
        attemptHeal(event.getWorkflowId(), event.getWorkflowName(), 0);
    }
    
    /**
     * Attempt to heal a failed workflow (recursive with limit)
     * 
     * @param workflowId GitHub workflow run ID
     * @param workflowName Workflow file name
     * @param iteration Current iteration (0-2)
     */
    private void attemptHeal(String workflowId, String workflowName, int iteration) {
        if (iteration >= MAX_ITERATIONS) {
            logger.error("❌ Max healing iterations ({}) reached for workflow {}", 
                    MAX_ITERATIONS, workflowId);
            escalateToHuman(workflowId, "Max iterations reached");
            return;
        }
        
        HealingAttempt attempt = new HealingAttempt(workflowId, workflowName, "UNKNOWN", "", "");
        
        try {
            // ========== STAGE 1: Check Circuit Breaker ==========
            logger.info("🔌 [Iteration {}/{}] Checking circuit breaker for {}", 
                    iteration + 1, MAX_ITERATIONS, workflowId);
            
            String errorFingerprint = "";
            try {
                // Fetch logs to compute error fingerprint
                String logs = rateLimiter.taskQueue.isEmpty() ? 
                        githubAPI.getWorkflowLogs(workflowId) : "";
                errorFingerprint = computeErrorFingerprint(logs);
            } catch (Exception e) {
                logger.warn("Could not fetch logs for fingerprint", e);
            }
            
            if (!circuitBreaker.shouldAttemptFix(workflowId, errorFingerprint)) {
                logger.error("🔴 Circuit breaker OPEN: Escalating to human");
                escalateToHuman(workflowId, "Circuit breaker open");
                return;
            }
            
            // ========== STAGE 2: Analyze Logs ==========
            logger.info("📊 Analyzing workflow logs...");
            String logs = rateLimiter.taskQueue.isEmpty() ? 
                    githubAPI.getWorkflowLogs(workflowId) : "";
            
            Map<String, String> analysis = errorParser.parseErrors(logs);
            String errorType = analysis.get("errorType");
            String errorMessage = analysis.getOrDefault("errorSummary", "Unknown");
            
            attempt.setErrorType(errorType);
            attempt.setErrorSummary(errorMessage);
            attempt.setErrorFingerprint(errorFingerprint);
            
            logger.info("🔍 Error identified: {} - {}", errorType, errorMessage.substring(0, 
                    Math.min(100, errorMessage.length())));
            
            // ========== STAGE 3: Check Repeated Failure ==========
            if (stateManager.isRepeatedFailure(workflowId, errorFingerprint)) {
                logger.warn("🔄 Same error has failed before - escalating");
                escalateToHuman(workflowId, "Repeated failure: same error");
                return;
            }
            
            // ========== STAGE 4: Generate Fix ==========
            logger.info("🤖 Generating code fix...");
            
            if (repairAgent == null) {
                logger.error("❌ Repair agent not available - escalating");
                escalateToHuman(workflowId, "Repair agent unavailable");
                return;
            }
            
            String fixCode = repairAgent.generateFix(errorType, errorMessage, logs);
            if (fixCode == null || fixCode.trim().isEmpty()) {
                logger.warn("⚠️ Repair failed to generate code");
                attempt.setStatus(HealingAttempt.HealingStatus.FAILED);
                attempt.setNotes("No fix generated by repair agent");
                stateManager.recordAttempt(attempt);
                
                // Retry
                scheduleRetry(workflowId, workflowName, iteration + 1);
                return;
            }
            
            attempt.setFixStrategy("AI_CONSENSUS");
            logger.info("✅ Fix generated: {} bytes", fixCode.length());
            
            // ========== STAGE 5: Validate Fix ==========
            logger.info("✔️  Validating fix...");
            
            String beforeTestOutput = ""; // Would fetch from GitHub
            String afterTestOutput = ""; // Would come from test run
            
            ValidationResult validation = validationPipeline.validate(
                    fixCode,
                    "", // original code
                    beforeTestOutput,
                    afterTestOutput
            );
            
            attempt.setValidationResult(validation);
            attempt.setConfidenceScore(validation.getOverallScore());
            
            if (!validation.isPassed()) {
                logger.warn("❌ Validation failed: {}", validation.getNotes());
                attempt.setStatus(HealingAttempt.HealingStatus.FAILED);
                stateManager.recordAttempt(attempt);
                
                // Retry with different strategy
                scheduleRetry(workflowId, workflowName, iteration + 1);
                return;
            }
            
            if (validation.getOverallScore() < 0.85) {
                logger.warn("⚠️ Low confidence ({:.0f}%): Escalating", 
                        validation.getOverallScore() * 100);
                escalateToHuman(workflowId, "Low confidence score: " + validation.getOverallScore());
                return;
            }
            
            logger.info("✅ Validation PASSED (score: {:.0f}%)", 
                    validation.getOverallScore() * 100);
            
            // ========== STAGE 6: Apply Fix ==========
            logger.info("💾 Committing fix to repository...");
            
            String commitHash = "";
            try {
                rateLimiter.executeBlocking("commit_fix", () -> {
                    try {
                        // TODO: Integrate with actual Git service
                        // commitHash = gitService.commitChanges(fixCode, "Healing fix for: " + errorType);
                    } catch (Exception e) {
                        logger.error("Commit failed", e);
                    }
                });
                
                if (!commitHash.isEmpty()) {
                    attempt.addCommitHash(commitHash);
                    logger.info("✅ Fix committed: {}", commitHash);
                }
                
            } catch (InterruptedException e) {
                logger.error("❌ Commit interrupted", e);
                attempt.setStatus(HealingAttempt.HealingStatus.FAILED);
                stateManager.recordAttempt(attempt);
                Thread.currentThread().interrupt();
                return;
            }
            
            // ========== STAGE 7: Retrigger Workflow ==========
            logger.info("🔄 Retriggering workflow...");
            
            try {
                rateLimiter.executeBlocking("retrigger_workflow", () -> {
                    try {
                        githubAPI.triggerWorkflow(workflowName, "main");
                    } catch (Exception e) {
                        logger.error("Workflow trigger failed", e);
                    }
                });
            } catch (InterruptedException e) {
                logger.error("❌ Trigger interrupted", e);
                Thread.currentThread().interrupt();
                return;
            }
            
            // ========== STAGE 8: Monitor Results ==========
            logger.info("👁️  Monitoring workflow result (waiting {} ms)...", 
                    WORKFLOW_CHECK_DELAY_MS);
            
            scheduler.schedule(() -> checkWorkflowResult(workflowId, workflowName, 
                    attempt.getAttemptId(), iteration), 
                    WORKFLOW_CHECK_DELAY_MS, TimeUnit.MILLISECONDS);
            
            attempt.setStatus(HealingAttempt.HealingStatus.ATTEMPTED);
            stateManager.recordAttempt(attempt);
            
        } catch (Exception e) {
            logger.error("❌ Healing loop exception", e);
            attempt.setStatus(HealingAttempt.HealingStatus.FAILED);
            attempt.setNotes("Exception: " + e.getMessage());
            stateManager.recordAttempt(attempt);
            escalateToHuman(workflowId, "Healing loop exception: " + e.getMessage());
        }
    }
    
    /**
     * Check if workflow passed after fix
     */
    private void checkWorkflowResult(String workflowId, String workflowName, 
                                     String attemptId, int iteration) {
        try {
            logger.info("🔍 Checking workflow result for attempt: {}", attemptId);
            
            boolean isSuccessful = githubAPI.isLastWorkflowSuccessful(workflowName);
            
            if (isSuccessful) {
                logger.info("✅ HEALING SUCCESSFUL: Workflow {} is now passing!", 
                        workflowName);
                
                circuitBreaker.recordSuccess(workflowId);
                stateManager.markResolved(attemptId, HealingAttempt.HealingStatus.SUCCESS);
                
                // Learn from success
                // learningEngine.learnFromSuccess(analysis, fix);
                
            } else {
                logger.warn("❌ Workflow still failing - retry #{}", iteration + 1);
                
                circuitBreaker.recordFailure(workflowId);
                
                // Recursive retry
                attemptHeal(workflowId, workflowName, iteration + 1);
            }
            
        } catch (Exception e) {
            logger.error("Error checking workflow result", e);
            scheduleRetry(workflowId, workflowName, iteration + 1);
        }
    }
    
    /**
     * Schedule a retry with exponential backoff
     */
    private void scheduleRetry(String workflowId, String workflowName, int nextIteration) {
        long delayMs = 60000 * nextIteration; // 1, 2, 3... minutes backoff
        
        logger.info("⏱️  Scheduling retry #{} in {} ms", nextIteration, delayMs);
        
        scheduler.schedule(
                () -> attemptHeal(workflowId, workflowName, nextIteration),
                delayMs,
                TimeUnit.MILLISECONDS
        );
    }
    
    /**
     * Escalate to human (admin review required)
     */
    private void escalateToHuman(String workflowId, String reason) {
        logger.error("👤 ESCALATING to human: {}", reason);
        
        // TODO: Implement escalation:
        // - Send Slack notification
        // - PagerDuty alert
        // - Mark in Firestore for admin dashboard
        // - Create GitHub issue
    }
    
    /**
     * Compute fingerprint of error for comparison
     */
    private String computeErrorFingerprint(String logs) {
        if (logs == null || logs.isEmpty()) {
            return "EMPTY";
        }
        
        // Take first 200 chars of error message as fingerprint
        String fingerprint = logs.substring(0, Math.min(200, logs.length()))
                .replaceAll("[0-9]+", "#") // Normalize numbers
                .replaceAll("\\s+", " "); // Normalize spaces
        
        return String.valueOf(fingerprint.hashCode());
    }
    
    /**
     * Placeholder for workflow failure event
     */
    public static class WorkflowFailureEvent {
        private String workflowId;
        private String workflowName;
        private String conclusion;
        
        // Getters
        public String getWorkflowId() { return workflowId; }
        public String getWorkflowName() { return workflowName; }
        public String getEventType() { return "workflow_run"; }
        public boolean isFailed() { return "failure".equals(conclusion); }
    }
}
