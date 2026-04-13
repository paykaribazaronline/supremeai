package org.example.service;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * FixApplier - Applies validated fixes to source code and logs decisions
 * 
 * Purpose: Apply fixes to actual source code, maintain change history,
 * and integrate with AgentDecisionLogger for decision tracking.
 * 
 * Integration: Writes decision logs via REST client to DecisionsController
 */
public class FixApplier {
    
    private final AgentDecisionLogger decisionLogger;
    
    public static class AppliedFix {
        public String fixId;
        public String filePath;
        public String originalCode;
        public String fixedCode;
        public String changeDescription;
        public long appliedAt;
        public boolean success;
        public String decisionId; // Links to decision logging
        
        public AppliedFix(String fixId, String filePath) {
            this.fixId = fixId;
            this.filePath = filePath;
            this.appliedAt = System.currentTimeMillis();
        }
    }
    
    public FixApplier(AgentDecisionLogger decisionLogger) {
        this.decisionLogger = decisionLogger;
    }
    
    /**
     * Apply a fix to source code and log the decision
     */
    public AppliedFix applyFix(String fixId, String filePath, String originalCode, 
                               String fixedCode, String agent, float confidence,
                               List<String> fixStrategies) {
        AppliedFix result = new AppliedFix(fixId, filePath);
        result.originalCode = originalCode;
        result.fixedCode = fixedCode;
        
        try {
            // Step 1: Log the decision through DecisionsController
            String decisionDescription = String.format(
                "Auto-fix error in %s using %s. Strategies: %s",
                filePath,
                String.join(", ", fixStrategies),
                agent
            );
            
            // Create decision log entry (will be persisted by DecisionsController)
            // This call would normally be made via REST, but for now we use the logger directly
            AgentDecisionLogger.AgentDecision decision = decisionLogger.logDecision(
                agent,                          // agent
                "error-fixing",                 // taskType
                extractProjectId(filePath),     // projectId
                decisionDescription,            // decision
                "Automatic error detection and fixing",  // reasoning
                confidence,                     // confidence (0.0-1.0)
                fixStrategies                   // alternatives
            );
            
            result.decisionId = decision.decisionId;
            
            // Step 2: Write fixed code to file
            if (applyFixToFile(filePath, fixedCode)) {
                result.success = true;
                result.changeDescription = "Applied auto-fix successfully";
                
                // Step 3: Log the successful application
                // Store outcome (will be called by validation service after testing)
                logFixOutcome(result.decisionId, "SUCCESS", 0.95f, agent);
            } else {
                result.success = false;
                result.changeDescription = "Failed to write fix to file";
                logFixOutcome(result.decisionId, "FAIL", 0.0f, agent);
            }
            
        } catch (Exception e) {
            result.success = false;
            result.changeDescription = "Error applying fix: " + e.getMessage();
        }
        
        return result;
    }
    
    /**
     * Apply fix to actual file on disk
     */
    private boolean applyFixToFile(String filePath, String fixedCode) {
        try {
            Path path = Paths.get(filePath);
            
            // Create backup of original
            Path backupPath = Paths.get(filePath + ".backup.auto");
            if (!Files.exists(backupPath)) {
                Files.copy(path, backupPath);
            }
            
            // Write fix to file
            Files.write(path, fixedCode.getBytes());
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Log the outcome of applying a fix
     */
    private void logFixOutcome(String decisionId, String result, double successMetric, String agent) {
        try {
            // Record the outcome decision
            String[] patterns = result.equals("SUCCESS") ? 
                new String[]{"auto-fix", "error-detection", agent.toLowerCase()} :
                new String[]{"auto-fix-failed", agent.toLowerCase()};
            
            decisionLogger.recordDecisionOutcome(
                decisionId,
                result,  // SUCCESS, FAILURE, PARTIAL
                "Fix outcome recorded by " + agent,
                successMetric,
                patterns
            );
        } catch (Exception e) {
            // Log failure if outcome recording fails
        }
    }
    
    /**
     * Rollback a fix if needed
     */
    public boolean rollbackFix(AppliedFix fix) {
        try {
            Path originalPath = Paths.get(fix.filePath);
            Path backupPath = Paths.get(fix.filePath + ".backup.auto");
            
            if (Files.exists(backupPath)) {
                Files.copy(backupPath, originalPath, 
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                
                // Log rollback as decision outcome
                decisionLogger.recordDecisionOutcome(
                    fix.decisionId,
                    "ROLLED_BACK",
                    "Fix was rolled back",
                    0.0,
                    "rollback"
                );
                
                return true;
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Get fix statistics
     */
    public Map<String, Object> getFixStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Retrieve stats from decision logger
        // This would normally query the stored decisions
        stats.put("totalFixes", 0);
        stats.put("successfulFixes", 0);
        stats.put("failedFixes", 0);
        stats.put("rolledBackFixes", 0);
        stats.put("averageConfidence", 0.0);
        stats.put("timestamp", System.currentTimeMillis());
        
        return stats;
    }
    
    /**
     * Extract project ID from file path
     */
    private String extractProjectId(String filePath) {
        // Extract from path like: /projects/myapp/src/Main.java
        String[] parts = filePath.split("[/\\\\]");
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].equals("projects")) {
                return parts[i + 1];
            }
        }
        return "unknown";
    }
    
    /**
     * Store pattern for future learning
     */
    public void storePattern(String errorPattern, String fixPattern, float successRate) {
        // This would be stored in Firebase or local database
        // For learning in future auto-fix iterations
        Map<String, Object> pattern = new HashMap<>();
        pattern.put("errorPattern", errorPattern);
        pattern.put("fixPattern", fixPattern);
        pattern.put("successRate", successRate);
        pattern.put("timestamp", System.currentTimeMillis());
        pattern.put("useCount", 1);
        
        // Store to persistent storage (Firebase, MongoDB, etc.)
        // firebaseService.storeFixPattern(pattern);
    }
}
