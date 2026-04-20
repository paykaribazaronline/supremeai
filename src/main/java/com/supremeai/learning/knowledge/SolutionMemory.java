package com.supremeai.learning.knowledge;

import java.util.Map;

public class SolutionMemory {
    private String triggerError;
    private String resolvedCode;
    private String workingAIProvider;
    private long timestamp;
    
    // Multi-dimensional scoring for ranking solutions
    private int successCount;         // How many times it worked
    private int failureCount;         // How many times it failed to fix the issue
    private long executionTimeMs;     // How fast this code runs
    private int codeLength;           // Shorter code is usually better (less complex)
    private double securityScore;     // Evaluated by Alpha-Security Agent (0.0 to 1.0)

    public SolutionMemory(String triggerError, String resolvedCode, String workingAIProvider, 
                          long executionTimeMs, double securityScore) {
        this.triggerError = triggerError;
        this.resolvedCode = resolvedCode;
        this.workingAIProvider = workingAIProvider;
        this.timestamp = System.currentTimeMillis();
        
        this.successCount = 1;
        this.failureCount = 0;
        this.executionTimeMs = executionTimeMs;
        this.codeLength = resolvedCode.length();
        this.securityScore = securityScore;
    }

    public void incrementSuccess() { this.successCount++; }
    public void incrementFailure() { this.failureCount++; }
    
    /**
     * Calculates the "Supreme Score" to rank which solution is best.
     * Weights: 
     * 50% Success Rate (Reliability)
     * 30% Security Score (Safety)
     * 10% Performance/Speed
     * 10% Simplicity (Code Length)
     */
    public double calculateSupremeScore() {
        int totalAttempts = successCount + failureCount;
        if (totalAttempts == 0) return 0.0;
        
        // 1. Reliability (0.0 to 1.0)
        double successRate = (double) successCount / totalAttempts;
        
        // 2. Speed Score (Faster = closer to 1.0) - Assuming 1000ms is standard
        double speedScore = Math.max(0, 1.0 - (executionTimeMs / 1000.0)); 
        
        // 3. Simplicity Score (Shorter = closer to 1.0) - Assuming 500 chars is standard
        double simplicityScore = Math.max(0, 1.0 - (codeLength / 500.0));
        
        // Weighted formula
        return (successRate * 0.50) + 
               (this.securityScore * 0.30) + 
               (speedScore * 0.10) + 
               (simplicityScore * 0.10);
    }

    // Getters
    public String getTriggerError() { return triggerError; }
    public String getResolvedCode() { return resolvedCode; }
    public String getWorkingAIProvider() { return workingAIProvider; }
    public int getSuccessCount() { return successCount; }
    public int getFailureCount() { return failureCount; }
}