package com.supremeai.learning.knowledge;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;

@Document(collectionName = "solution_memories")
public class SolutionMemory {
    @DocumentId
    private String id;
    private String triggerError;
    private String resolvedCode;
    private String workingAIProvider;
    private LocalDateTime timestamp;

    // Multi-dimensional scoring for ranking solutions
    private int successCount;
    private int failureCount;
    private long executionTimeMs;
    private int codeLength;
    private double securityScore;

    public SolutionMemory() {
        // Default constructor for Firestore deserialization
    }

    public SolutionMemory(String triggerError, String resolvedCode, String workingAIProvider,
                          long executionTimeMs, double securityScore) {
        this.triggerError = triggerError;
        this.resolvedCode = resolvedCode;
        this.workingAIProvider = workingAIProvider;
        this.timestamp = LocalDateTime.now();
        this.successCount = 1;
        this.failureCount = 0;
        this.executionTimeMs = executionTimeMs;
        this.codeLength = resolvedCode != null ? resolvedCode.length() : 0;
        this.securityScore = securityScore;
    }

    public void incrementSuccess() { this.successCount++; }
    public void incrementFailure() { this.failureCount++; }

    /**
     * Calculates the "Supreme Score" to rank which solution is best.
     * Weights: 50% Success Rate, 30% Security Score, 10% Speed, 10% Simplicity.
     */
    public double calculateSupremeScore() {
        int totalAttempts = successCount + failureCount;
        if (totalAttempts == 0) return 0.0;

        double successRate = (double) successCount / totalAttempts;
        double speedScore = Math.max(0, 1.0 - (executionTimeMs / 1000.0));
        double simplicityScore = Math.max(0, 1.0 - (codeLength / 500.0));

        return (successRate * 0.50) +
               (this.securityScore * 0.30) +
               (speedScore * 0.10) +
               (simplicityScore * 0.10);
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTriggerError() { return triggerError; }
    public void setTriggerError(String triggerError) { this.triggerError = triggerError; }

    public String getResolvedCode() { return resolvedCode; }
    public void setResolvedCode(String resolvedCode) {
        this.resolvedCode = resolvedCode;
        this.codeLength = resolvedCode != null ? resolvedCode.length() : 0;
    }

    public String getWorkingAIProvider() { return workingAIProvider; }
    public void setWorkingAIProvider(String workingAIProvider) { this.workingAIProvider = workingAIProvider; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int count) { this.successCount = count; }

    public int getFailureCount() { return failureCount; }
    public void setFailureCount(int count) { this.failureCount = count; }

    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public double getSecurityScore() { return securityScore; }
    public void setSecurityScore(double securityScore) { this.securityScore = securityScore; }

    public int getCodeLength() { return codeLength; }
    public void setCodeLength(int codeLength) { this.codeLength = codeLength; }
}
