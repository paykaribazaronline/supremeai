package com.supremeai.learning.knowledge;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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

    // Enhancement: Versioning & Rollback
    private long version = 1L;
    private String previousVersionId;

    // Enhancement: Unlearning / Obsolete flag
    private boolean obsolete = false;
    private String obsoleteReason;
    private LocalDateTime obsoletedAt;

    // Enhancement: Timeless flag
    private boolean timeless = false;

    // Enhancement: Knowledge Lineage (Audit trail)
    private String sourceUrl;         // Where this solution was found
    private String sourceSite;        // Which site (Wikipedia, SO, GitHub)
    private double sourceAuthority;  // Authority weight of the source
    private LocalDateTime extractedAt;
    private String extractedBy;      // "system", userId, or service name
    private String validationStatus; // PENDING, VALIDATED, REJECTED
    private LocalDateTime validatedAt;
    private String validatedBy;

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
     * 
     * Enhancement: Recency decay factor reduces confidence for older solutions
     * unless marked as timeless. Decay half-life: ~693 days (rate=0.001 per day).
     */
    public double calculateSupremeScore() {
        int totalAttempts = successCount + failureCount;
        if (totalAttempts == 0) return 0.0;

        // Base components
        double successRate = (double) successCount / totalAttempts;
        double speedScore = Math.max(0, 1.0 - (executionTimeMs / 1000.0));
        double simplicityScore = Math.max(0, 1.0 - (codeLength / 500.0));

        // Base weighted score
        double baseScore = (successRate * 0.50) +
                          (this.securityScore * 0.30) +
                          (speedScore * 0.10) +
                          (simplicityScore * 0.10);

        // Enhancement: Recency decay (skip if timeless)
        if (!timeless) {
            long ageDays = ChronoUnit.DAYS.between(timestamp, LocalDateTime.now());
            double decayFactor = Math.exp(-0.001 * ageDays);  // Configurable decay
            return baseScore * decayFactor;
        }

        return baseScore;
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

    // Versioning
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    public String getPreviousVersionId() { return previousVersionId; }
    public void setPreviousVersionId(String previousVersionId) { this.previousVersionId = previousVersionId; }

    // Obsolete
    public boolean isObsolete() { return obsolete; }
    public void setObsolete(boolean obsolete) { this.obsolete = obsolete; }
    public String getObsoleteReason() { return obsoleteReason; }
    public void setObsoleteReason(String obsoleteReason) { this.obsoleteReason = obsoleteReason; }
    public LocalDateTime getObsoletedAt() { return obsoletedAt; }
    public void setObsoletedAt(LocalDateTime obsoletedAt) { this.obsoletedAt = obsoletedAt; }

    // Timeless
    public boolean isTimeless() { return timeless; }
    public void setTimeless(boolean timeless) { this.timeless = timeless; }

    // Lineage
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getSourceSite() { return sourceSite; }
    public void setSourceSite(String sourceSite) { this.sourceSite = sourceSite; }
    public double getSourceAuthority() { return sourceAuthority; }
    public void setSourceAuthority(double sourceAuthority) { this.sourceAuthority = sourceAuthority; }
    public LocalDateTime getExtractedAt() { return extractedAt; }
    public void setExtractedAt(LocalDateTime extractedAt) { this.extractedAt = extractedAt; }
    public String getExtractedBy() { return extractedBy; }
    public void setExtractedBy(String extractedBy) { this.extractedBy = extractedBy; }
    public String getValidationStatus() { return validationStatus; }
    public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }
    public LocalDateTime getValidatedAt() { return validatedAt; }
    public void setValidatedAt(LocalDateTime validatedAt) { this.validatedAt = validatedAt; }
    public String getValidatedBy() { return validatedBy; }
    public void setValidatedBy(String validatedBy) { this.validatedBy = validatedBy; }

    // Mark obsolete
    public void markObsolete(String reason) {
        this.obsolete = true;
        this.obsoleteReason = reason;
        this.obsoletedAt = LocalDateTime.now();
    }

    /**
     * Create a new version of this solution (immutable update pattern).
     */
    public SolutionMemory createUpdate(String newResolvedCode, long newExecutionTimeMs, double newSecurityScore) {
        SolutionMemory updated = new SolutionMemory(
            this.triggerError,
            newResolvedCode,
            this.workingAIProvider,
            newExecutionTimeMs,
            newSecurityScore
        );
        updated.setVersion(this.version + 1);
        updated.setPreviousVersionId(this.id);
        updated.setSuccessCount(this.successCount);
        updated.setFailureCount(this.failureCount);
        // Copy lineage metadata
        updated.setSourceUrl(this.sourceUrl);
        updated.setSourceSite(this.sourceSite);
        updated.setSourceAuthority(this.sourceAuthority);
        updated.setExtractedAt(this.extractedAt);
        updated.setExtractedBy(this.extractedBy);
        // New version resets timestamp but inherits extractedAt
        return updated;
    }
}
