package org.example.selfhealing.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain Model: Represents a single healing attempt
 * 
 * Each time SupremeAI attempts to fix a failed workflow,
 * a HealingAttempt is recorded for:
 * - Audit trail
 * - Loop detection (repeated failures)
 * - Learning & metrics
 * - State management
 */
public class HealingAttempt {
    
    @JsonProperty("attemptId")
    private String attemptId;
    
    @JsonProperty("workflowId")
    private String workflowId;
    
    @JsonProperty("workflowName")
    private String workflowName;
    
    @JsonProperty("errorFingerprint")
    private String errorFingerprint; // Hash of error message for comparison
    
    @JsonProperty("errorType")
    private String errorType; // COMPILATION_ERROR, TEST_FAILURE, etc.
    
    @JsonProperty("errorSummary")
    private String errorSummary; // First 500 chars of error
    
    @JsonProperty("fixStrategy")
    private String fixStrategy; // What approach was used (AI consensus, pattern match, etc)
    
    @JsonProperty("status")
    private HealingStatus status; // ATTEMPTED, SUCCESS, FAILED, ESCALATED
    
    @JsonProperty("retryCount")
    private int retryCount;
    
    @JsonProperty("maxRetries")
    private int maxRetries;
    
    @JsonProperty("createdAt")
    private Instant createdAt;
    
    @JsonProperty("resolvedAt")
    private Instant resolvedAt;
    
    @JsonProperty("commitHashes")
    private List<String> commitHashes = new ArrayList<>();
    
    @JsonProperty("validationResult")
    private ValidationResult validationResult;
    
    @JsonProperty("confidenceScore")
    private double confidenceScore; // 0.0 to 1.0
    
    @JsonProperty("notes")
    private String notes;
    
    // Constructors
    public HealingAttempt() {
    }
    
    public HealingAttempt(String workflowId, String workflowName, String errorType, 
                         String errorFingerprint, String errorSummary) {
        this.workflowId = workflowId;
        this.workflowName = workflowName;
        this.errorType = errorType;
        this.errorFingerprint = errorFingerprint;
        this.errorSummary = errorSummary;
        this.attemptId = "attempt_" + System.nanoTime();
        this.status = HealingStatus.ATTEMPTED;
        this.retryCount = 0;
        this.createdAt = Instant.now();
        this.confidenceScore = 0.5; // Default to 50%
    }
    
    // Getters & Setters
    public String getAttemptId() {
        return attemptId;
    }
    
    public void setAttemptId(String attemptId) {
        this.attemptId = attemptId;
    }
    
    public String getWorkflowId() {
        return workflowId;
    }
    
    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }
    
    public String getWorkflowName() {
        return workflowName;
    }
    
    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }
    
    public String getErrorFingerprint() {
        return errorFingerprint;
    }
    
    public void setErrorFingerprint(String errorFingerprint) {
        this.errorFingerprint = errorFingerprint;
    }
    
    public String getErrorType() {
        return errorType;
    }
    
    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }
    
    public String getErrorSummary() {
        return errorSummary;
    }
    
    public void setErrorSummary(String errorSummary) {
        this.errorSummary = errorSummary;
    }
    
    public String getFixStrategy() {
        return fixStrategy;
    }
    
    public void setFixStrategy(String fixStrategy) {
        this.fixStrategy = fixStrategy;
    }
    
    public HealingStatus getStatus() {
        return status;
    }
    
    public void setStatus(HealingStatus status) {
        this.status = status;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
    
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getResolvedAt() {
        return resolvedAt;
    }
    
    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
    
    public List<String> getCommitHashes() {
        return commitHashes;
    }
    
    public void setCommitHashes(List<String> commitHashes) {
        this.commitHashes = commitHashes;
    }
    
    public void addCommitHash(String hash) {
        this.commitHashes.add(hash);
    }
    
    public ValidationResult getValidationResult() {
        return validationResult;
    }
    
    public void setValidationResult(ValidationResult validationResult) {
        this.validationResult = validationResult;
    }
    
    public double getConfidenceScore() {
        return confidenceScore;
    }
    
    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    // Enum for healing status
    public enum HealingStatus {
        ATTEMPTED,   // Fix generated and being attempted
        SUCCESS,     // Workflow now passes
        FAILED,      // Fix didn't work
        ESCALATED    // Human review needed
    }
    
    @Override
    public String toString() {
        return "HealingAttempt{" +
                "attemptId='" + attemptId + '\'' +
                ", workflowId='" + workflowId + '\'' +
                ", status=" + status +
                ", retryCount=" + retryCount +
                ", confidenceScore=" + confidenceScore +
                '}';
    }
}
