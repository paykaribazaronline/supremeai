package org.supremeai.selfhealing.repair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AutoCodeRepairAgent: The Surgeon
 * 
 * Analyzes runtime failures and automatically generates, validates, and applies code fixes
 * without human intervention. Uses AI agents for consensus-based repair decisions.
 * 
 * Repair Flow:
 *   1. Detect failure (from health checks or exceptions)
 *   2. Analyze root cause (stack trace + context)
 *   3. Query AI agents (X-Builder, Y-Reviewer, Z-Architect) for suggestions
 *   4. Apply fix if consensus >= 70%
 *   5. Deploy, test, and verify
 *   6. Escalate to admin if consensus < 70%
 */
@Component
public class AutoCodeRepairAgent {
    
    private static final Logger log = LoggerFactory.getLogger(AutoCodeRepairAgent.class);
    private static final double CONSENSUS_THRESHOLD = 0.70;
    
    @Autowired
    private SelfHealingService healingService;
    
    @Autowired
    private AIAgentOrchestrator aiAgents;
    
    @Autowired
    private GitRepositoryService gitRepo;
    
    @Autowired
    private DeploymentService deploymentService;
    
    public class CodeFixSuggestion {
        public String agentId;           // Which AI agent suggested this
        public String fixDescription;    // Human-readable explanation
        public String codeChange;        // The actual code to change
        public double confidence;        // 0.0-1.0 confidence level
        public String affectedComponent; // Component to fix
        public long estimatedImpact;     // Lines of code affected
        public List<String> affectedTests;
        public long timestamp;
    }
    
    public class RepairResult {
        public enum Status { SUCCESS, ESCALATED, FAILED }
        public Status status;
        public String message;
        public String gitCommit;
        public double consensusScore;
        public List<CodeFixSuggestion> suggestions;
        public Map<String, Object> metrics;
    }
    
    /**
     * Main repair entry point: triggered when failure detected
     */
    public RepairResult attemptAutoRepair(String failingComponent, Exception error, String contextData) {
        long startTime = System.currentTimeMillis();
        
        log.warn("🔧 AUTO-REPAIR AGENT ACTIVATED for: {}", failingComponent);
        log.warn("   Error: {} | Cause: {}", error.getClass().getSimpleName(), error.getMessage());
        
        try {
            // Step 1: Analyze root cause
            RootCauseAnalysis rootCause = analyzeRootCause(failingComponent, error, contextData);
            log.info("   Root Cause: {}", rootCause.description);
            
            // Step 2: Query AI agents for fix suggestions
            List<CodeFixSuggestion> suggestions = queryAIAgentsForFixes(
                failingComponent,
                rootCause,
                getCurrentCode(failingComponent)
            );
            
            if (suggestions.isEmpty()) {
                log.warn("   No fix suggestions generated - escalating");
                return new RepairResult(
                    RepairResult.Status.ESCALATED,
                    "No AI consensus on fix",
                    null,
                    0.0,
                    suggestions
                );
            }
            
            // Step 3: Calculate consensus score
            double consensusScore = calculateConsensus(suggestions);
            log.info("   Consensus Score: {}", String.format("%.1f%%", consensusScore * 100));
            
            // Step 4: Apply most confident fix if consensus threshold met
            if (consensusScore >= CONSENSUS_THRESHOLD) {
                CodeFixSuggestion bestFix = suggestions.get(0);
                log.info("   Applying Fix: {}", bestFix.fixDescription);
                
                // Apply the code change
                applyCodeFix(failingComponent, bestFix);
                
                // Verify tests pass
                boolean testsPassed = runAffectedTests(bestFix.affectedTests);
                if (!testsPassed) {
                    log.error("   Tests failed after fix - reverting");
                    revertCodeFix(failingComponent, bestFix);
                    return new RepairResult(
                        RepairResult.Status.FAILED,
                        "Tests failed after fix",
                        null,
                        consensusScore,
                        suggestions
                    );
                }
                
                // Commit and deploy
                String commitHash = commitAndDeploy(failingComponent, bestFix, rootCause);
                
                long duration = System.currentTimeMillis() - startTime;
                log.info("✅ AUTO-REPAIR SUCCESSFUL: {} fixed in {}ms", failingComponent, duration);
                
                return new RepairResult(
                    RepairResult.Status.SUCCESS,
                    "Auto-repair applied successfully",
                    commitHash,
                    consensusScore,
                    suggestions
                );
                
            } else {
                log.warn("   Consensus too low ({}) - escalating to admin", String.format("%.1f%%", consensusScore * 100));
                return new RepairResult(
                    RepairResult.Status.ESCALATED,
                    "Consensus below threshold (" + consensusScore + ")",
                    null,
                    consensusScore,
                    suggestions
                );
            }
            
        } catch (Exception e) {
            log.error("❌ AUTO-REPAIR FAILED: {}", e.getMessage(), e);
            return new RepairResult(
                RepairResult.Status.FAILED,
                "Exception during repair: " + e.getMessage(),
                null,
                0.0,
                Collections.emptyList()
            );
        }
    }
    
    /**
     * Analyze root cause from stack trace + context
     */
    private RootCauseAnalysis analyzeRootCause(String component, Exception error, String context) {
        RootCauseAnalysis analysis = new RootCauseAnalysis();
        analysis.component = component;
        analysis.exceptionType = error.getClass().getSimpleName();
        analysis.message = error.getMessage();
        analysis.stackTrace = stackTraceToString(error);
        analysis.timestamp = System.currentTimeMillis();
        
        // Pattern matching for common failures
        if (error instanceof NullPointerException) {
            analysis.description = "Null pointer dereference in " + component;
            analysis.category = "NULL_CHECK";
        } else if (error instanceof OutOfMemoryError) {
            analysis.description = "Memory leak in " + component;
            analysis.category = "MEMORY_LEAK";
        } else if (error.getMessage().contains("timeout")) {
            analysis.description = "Network timeout in " + component;
            analysis.category = "TIMEOUT";
        } else {
            analysis.description = "General exception in " + component;
            analysis.category = "GENERIC";
        }
        
        return analysis;
    }
    
    /**
     * Query X-Builder, Y-Reviewer, Z-Architect for fix suggestions
     */
    private List<CodeFixSuggestion> queryAIAgentsForFixes(
            String component, 
            RootCauseAnalysis rootCause, 
            String currentCode) {
        
        List<CodeFixSuggestion> suggestions = new ArrayList<>();
        
        // Query X-Builder (Code Generation)
        try {
            log.info("   Querying X-Builder for code generation...");
            CodeFixSuggestion builderSuggestion = aiAgents.xBuilder.suggestFix(
                component,
                rootCause,
                currentCode
            );
            if (builderSuggestion != null && builderSuggestion.confidence > 0.5) {
                builderSuggestion.agentId = "X-Builder";
                suggestions.add(builderSuggestion);
                log.info("   X-Builder confidence: {}", String.format("%.1f%%", builderSuggestion.confidence * 100));
            }
        } catch (Exception e) {
            log.warn("   X-Builder query failed: {}", e.getMessage());
        }
        
        // Query Y-Reviewer (Validation)
        try {
            log.info("   Querying Y-Reviewer for validation...");
            CodeFixSuggestion reviewerSuggestion = aiAgents.yReviewer.suggestFix(
                component,
                rootCause,
                currentCode
            );
            if (reviewerSuggestion != null && reviewerSuggestion.confidence > 0.5) {
                reviewerSuggestion.agentId = "Y-Reviewer";
                suggestions.add(reviewerSuggestion);
                log.info("   Y-Reviewer confidence: {}", String.format("%.1f%%", reviewerSuggestion.confidence * 100));
            }
        } catch (Exception e) {
            log.warn("   Y-Reviewer query failed: {}", e.getMessage());
        }
        
        // Query Z-Architect (Design)
        try {
            log.info("   Querying Z-Architect for architectural fix...");
            CodeFixSuggestion architectSuggestion = aiAgents.zArchitect.suggestFix(
                component,
                rootCause,
                currentCode
            );
            if (architectSuggestion != null && architectSuggestion.confidence > 0.5) {
                architectSuggestion.agentId = "Z-Architect";
                suggestions.add(architectSuggestion);
                log.info("   Z-Architect confidence: {}", String.format("%.1f%%", architectSuggestion.confidence * 100));
            }
        } catch (Exception e) {
            log.warn("   Z-Architect query failed: {}", e.getMessage());
        }
        
        // Sort by confidence
        suggestions.sort((a, b) -> Double.compare(b.confidence, a.confidence));
        return suggestions;
    }
    
    /**
     * Calculate consensus score (average confidence of all suggestions)
     */
    private double calculateConsensus(List<CodeFixSuggestion> suggestions) {
        if (suggestions.isEmpty()) return 0.0;
        return suggestions.stream()
            .mapToDouble(s -> s.confidence)
            .average()
            .orElse(0.0);
    }
    
    /**
     * Apply code fix to the component
     */
    private void applyCodeFix(String component, CodeFixSuggestion fix) throws Exception {
        String filePath = getComponentFilePath(component);
        String currentCode = gitRepo.readFile(filePath);
        String newCode = applyPatch(currentCode, fix.codeChange);
        gitRepo.writeFile(filePath, newCode);
        log.info("   Code fix applied to: {}", filePath);
    }
    
    /**
     * Run affected tests to verify fix
     */
    private boolean runAffectedTests(List<String> testFiles) {
        try {
            log.info("   Running {} affected tests...", testFiles.size());
            boolean allPassed = true;
            for (String testFile : testFiles) {
                boolean passed = deploymentService.runTest(testFile);
                if (!passed) {
                    allPassed = false;
                    log.error("   Test FAILED: {}", testFile);
                }
            }
            return allPassed;
        } catch (Exception e) {
            log.error("   Test execution failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Commit and deploy the fix
     */
    private String commitAndDeploy(String component, CodeFixSuggestion fix, RootCauseAnalysis rootCause) 
            throws Exception {
        
        String commitMessage = String.format(
            "🔧 Auto-fix: %s\n\nComponent: %s\nRoot Cause: %s\nAgent: %s\nConfidence: %.1f%%",
            fix.fixDescription,
            component,
            rootCause.description,
            fix.agentId,
            fix.confidence * 100
        );
        
        String commitHash = gitRepo.commit(commitMessage);
        log.info("   Committed: {}", commitHash);
        
        // Deploy the fix
        boolean deploySuccess = deploymentService.deploy(commitHash);
        if (deploySuccess) {
            log.info("   Deployment successful");
        } else {
            log.error("   Deployment failed - reverting");
            gitRepo.revert(commitHash);
            throw new Exception("Deployment failed");
        }
        
        return commitHash;
    }
    
    /**
     * Revert a failed code fix
     */
    private void revertCodeFix(String component, CodeFixSuggestion fix) {
        try {
            log.info("   Reverting failed fix...");
            gitRepo.revert("HEAD");
            log.info("   Revert successful");
        } catch (Exception e) {
            log.error("   Revert failed: {}", e.getMessage());
        }
    }
    
    // Helper classes
    private class RootCauseAnalysis {
        String component;
        String exceptionType;
        String message;
        String stackTrace;
        String description;
        String category;
        long timestamp;
    }
    
    private String getCurrentCode(String component) throws Exception {
        return gitRepo.readFile(getComponentFilePath(component));
    }
    
    private String getComponentFilePath(String component) {
        return "src/main/java/org/supremeai/" + component.toLowerCase() + ".java";
    }
    
    private String stackTraceToString(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element).append("\n");
        }
        return sb.toString();
    }
    
    private String applyPatch(String originalCode, String patch) {
        // Simplified patch application - real implementation would use unified diff
        return originalCode + "\n// Applied patch\n" + patch;
    }
    
    // Dependencies
    @lombok.Data
    @lombok.AllArgsConstructor
    private class RepairResult {
        enum Status { SUCCESS, ESCALATED, FAILED }
        Status status;
        String message;
        String gitCommit;
        double consensusScore;
        List<CodeFixSuggestion> suggestions;
    }
}
