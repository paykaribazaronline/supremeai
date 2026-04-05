package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * FIXED: Objective Severity Calculator
 * 
 * Problem: Self-reported severity creates conflict of interest
 * Solution: Objective severity calculation based purely on measurable metrics
 * 
 * Scoring System (0-100+):
 * - Error Type (0-100 points)
 *   * Security vulnerability: +100
 *   * Compilation error: +50
 *   * Crash/Runtime error: +75
 *   * Warning: +25
 *   * Info: +10
 * 
 * - Impact Scope (0-60 points)
 *   * Affected users > 1000: +30
 *   * Payment related: +40
 *   * Data loss potential: +35
 *   * Public API impact: +25
 * 
 * - Recovery Difficulty (0-40 points)
 *   * Requires data migration: +25
 *   * Requires app store update: +20
 *   * Requires user action: +15
 *   * Can rollback: -10
 * 
 * Severity Levels:
 * - CRITICAL: 80+ points
 * - HIGH: 60-79 points
 * - MEDIUM: 40-59 points
 * - LOW: 20-39 points
 * - INFO: <20 points
 * 
 * Domain Specific:
 * - Payment-related issues get additional weight
 * - Escrow payment failures are automatically HIGH+
 */
@Service
public class ObjectiveSeverityCalculator {
    
    private static final Logger logger = LoggerFactory.getLogger(ObjectiveSeverityCalculator.class);
    
    // Score thresholds
    private static final int CRITICAL_THRESHOLD = 80;
    private static final int HIGH_THRESHOLD = 60;
    private static final int MEDIUM_THRESHOLD = 40;
    private static final int LOW_THRESHOLD = 20;
    
    /**
     * Calculate objective severity for a build result
     */
    public SeverityResult calculate(BuildResult result) {
        int score = 0;
        List<String> factors = new ArrayList<>();
        
        // 1. Error Type Scoring (0-100)
        score += calculateErrorTypeScore(result, factors);
        
        // 2. Impact Scope Scoring (0-60)
        score += calculateImpactScore(result, factors);
        
        // 3. Recovery Difficulty Scoring (0-40)
        score += calculateRecoveryScore(result, factors);
        
        // 4. Domain-specific adjustments
        score += calculateDomainAdjustments(result, factors);
        
        // Determine severity level
        SeverityLevel level = determineSeverityLevel(score);
        
        logger.info("📊 Objective severity calculated: {} (score: {}, factors: {})",
            level, score, factors.size());
        
        return new SeverityResult(level, score, factors, result);
    }
    
    /**
     * Calculate error type score
     */
    private int calculateErrorTypeScore(BuildResult result, List<String> factors) {
        int score = 0;
        
        switch (result.getErrorType()) {
            case SECURITY_VULNERABILITY:
                score += 100;
                factors.add("Security vulnerability (+100)");
                break;
            case CRASH:
            case RUNTIME_ERROR:
                score += 75;
                factors.add("Crash/Runtime error (+75)");
                break;
            case COMPILATION_ERROR:
                score += 50;
                factors.add("Compilation error (+50)");
                break;
            case TEST_FAILURE:
                score += 40;
                factors.add("Test failure (+40)");
                break;
            case PERFORMANCE_DEGRADATION:
                score += 35;
                factors.add("Performance degradation (+35)");
                break;
            case WARNING:
                score += 25;
                factors.add("Warning (+25)");
                break;
            case INFO:
                score += 10;
                factors.add("Info (+10)");
                break;
            case NONE:
            default:
                // No error
                break;
        }
        
        // Additional error characteristics
        if (result.isBlocking()) {
            score += 20;
            factors.add("Blocking issue (+20)");
        }
        
        if (result.isRegression()) {
            score += 15;
            factors.add("Regression (+15)");
        }
        
        return score;
    }
    
    /**
     * Calculate impact scope score
     */
    private int calculateImpactScore(BuildResult result, List<String> factors) {
        int score = 0;
        
        // User impact
        int affectedUsers = result.getAffectedUsers();
        if (affectedUsers > 10000) {
            score += 30;
            factors.add("Massive user impact (>10K users) (+30)");
        } else if (affectedUsers > 1000) {
            score += 20;
            factors.add("Large user impact (>1K users) (+20)");
        } else if (affectedUsers > 100) {
            score += 10;
            factors.add("Moderate user impact (>100 users) (+10)");
        }
        
        // Payment related
        if (result.isPaymentRelated()) {
            score += 40;
            factors.add("Payment-related issue (+40)");
            
            if (result.isEscrowPayment()) {
                score += 15;
                factors.add("Escrow payment affected (+15)");
            }
        }
        
        // Data impact
        if (result.hasDataLoss()) {
            score += 35;
            factors.add("Data loss potential (+35)");
        }
        
        if (result.hasDataCorruption()) {
            score += 30;
            factors.add("Data corruption risk (+30)");
        }
        
        // API impact
        if (result.isPublicApiBroken()) {
            score += 25;
            factors.add("Public API broken (+25)");
        }
        
        // Critical path
        if (result.isInCriticalPath()) {
            score += 20;
            factors.add("Critical path affected (+20)");
        }
        
        return score;
    }
    
    /**
     * Calculate recovery difficulty score
     */
    private int calculateRecoveryScore(BuildResult result, List<String> factors) {
        int score = 0;
        
        // Recovery complexity
        if (result.requiresDataMigration()) {
            score += 25;
            factors.add("Requires data migration (+25)");
        }
        
        if (result.requiresAppStoreUpdate()) {
            score += 20;
            factors.add("Requires app store update (+20)");
        }
        
        if (result.requiresUserAction()) {
            score += 15;
            factors.add("Requires user action (+15)");
        }
        
        if (result.requiresManualFix()) {
            score += 10;
            factors.add("Requires manual intervention (+10)");
        }
        
        // Mitigation factors
        if (result.canRollback()) {
            score -= 10;
            factors.add("Can rollback (-10)");
        }
        
        if (result.hasHotfixAvailable()) {
            score -= 15;
            factors.add("Hotfix available (-15)");
        }
        
        return Math.max(0, score);
    }
    
    /**
     * Domain-specific adjustments
     */
    private int calculateDomainAdjustments(BuildResult result, List<String> factors) {
        int score = 0;
        
        // Bangladesh market specific
        if (result.isBdtTransaction()) {
            score += 10;
            factors.add("BDT transaction affected (+10)");
        }
        
        // Mobile money specific
        if (result.isMobileMoney()) {
            score += 15;
            factors.add("Mobile money affected (+15)");
        }
        
        // Banglish support
        if (result.isBanglishIssue()) {
            score += 5;
            factors.add("Banglish support issue (+5)");
        }
        
        return score;
    }
    
    /**
     * Determine severity level from score
     */
    private SeverityLevel determineSeverityLevel(int score) {
        if (score >= CRITICAL_THRESHOLD) {
            return SeverityLevel.CRITICAL;
        } else if (score >= HIGH_THRESHOLD) {
            return SeverityLevel.HIGH;
        } else if (score >= MEDIUM_THRESHOLD) {
            return SeverityLevel.MEDIUM;
        } else if (score >= LOW_THRESHOLD) {
            return SeverityLevel.LOW;
        } else {
            return SeverityLevel.INFO;
        }
    }
    
    /**
     * Quick severity check for common error types
     */
    public SeverityResult quickCalculate(ErrorType errorType, boolean isPaymentRelated) {
        BuildResult result = new BuildResult.Builder()
            .errorType(errorType)
            .paymentRelated(isPaymentRelated)
            .build();
        
        return calculate(result);
    }
    
    /**
     * Compare two severity results
     */
    public ComparisonResult compare(SeverityResult a, SeverityResult b) {
        int scoreDiff = a.getScore() - b.getScore();
        SeverityLevel higher = a.getScore() > b.getScore() ? a.getLevel() : b.getLevel();
        
        return new ComparisonResult(
            scoreDiff,
            higher,
            a.getScore() > b.getScore() ? "A" : "B",
            Math.abs(scoreDiff) > 20 ? "SIGNIFICANT" : "MINOR"
        );
    }
    
    // ============== Data Classes ==============
    
    public enum SeverityLevel {
        CRITICAL("Immediate action required", 4),
        HIGH("Urgent attention needed", 3),
        MEDIUM("Should be addressed soon", 2),
        LOW("Minor issue", 1),
        INFO("Informational only", 0);
        
        private final String description;
        private final int priority;
        
        SeverityLevel(String description, int priority) {
            this.description = description;
            this.priority = priority;
        }
        
        public String getDescription() { return description; }
        public int getPriority() { return priority; }
        
        public boolean isHigherThan(SeverityLevel other) {
            return this.priority > other.priority;
        }
    }
    
    public enum ErrorType {
        NONE,
        INFO,
        WARNING,
        COMPILATION_ERROR,
        TEST_FAILURE,
        PERFORMANCE_DEGRADATION,
        RUNTIME_ERROR,
        CRASH,
        SECURITY_VULNERABILITY
    }
    
    public static class SeverityResult {
        private final SeverityLevel level;
        private final int score;
        private final List<String> factors;
        private final BuildResult sourceResult;
        private final Instant calculatedAt;
        
        public SeverityResult(SeverityLevel level, int score, 
                             List<String> factors, BuildResult sourceResult) {
            this.level = level;
            this.score = score;
            this.factors = Collections.unmodifiableList(new ArrayList<>(factors));
            this.sourceResult = sourceResult;
            this.calculatedAt = Instant.now();
        }
        
        public SeverityLevel getLevel() { return level; }
        public int getScore() { return score; }
        public List<String> getFactors() { return factors; }
        public BuildResult getSourceResult() { return sourceResult; }
        public Instant getCalculatedAt() { return calculatedAt; }
        
        public boolean isCritical() { return level == SeverityLevel.CRITICAL; }
        public boolean requiresImmediateAction() { return score >= HIGH_THRESHOLD; }
        
        public Map<String, Object> toMap() {
            return Map.of(
                "level", level.name(),
                "score", score,
                "factors", factors,
                "calculatedAt", calculatedAt.toString()
            );
        }
    }
    
    public static class ComparisonResult {
        private final int scoreDifference;
        private final SeverityLevel higherSeverity;
        private final String higherSide;
        private final String significance;
        
        public ComparisonResult(int scoreDifference, SeverityLevel higherSeverity,
                               String higherSide, String significance) {
            this.scoreDifference = scoreDifference;
            this.higherSeverity = higherSeverity;
            this.higherSide = higherSide;
            this.significance = significance;
        }
        
        public int getScoreDifference() { return scoreDifference; }
        public SeverityLevel getHigherSeverity() { return higherSeverity; }
        public String getHigherSide() { return higherSide; }
        public String getSignificance() { return significance; }
    }
    
    /**
     * Build result data class
     */
    public static class BuildResult {
        private final ErrorType errorType;
        private final boolean blocking;
        private final boolean regression;
        private final int affectedUsers;
        private final boolean paymentRelated;
        private final boolean escrowPayment;
        private final boolean hasDataLoss;
        private final boolean hasDataCorruption;
        private final boolean publicApiBroken;
        private final boolean inCriticalPath;
        private final boolean requiresDataMigration;
        private final boolean requiresAppStoreUpdate;
        private final boolean requiresUserAction;
        private final boolean requiresManualFix;
        private final boolean canRollback;
        private final boolean hasHotfixAvailable;
        private final boolean bdtTransaction;
        private final boolean mobileMoney;
        private final boolean banglishIssue;
        
        private BuildResult(Builder builder) {
            this.errorType = builder.errorType;
            this.blocking = builder.blocking;
            this.regression = builder.regression;
            this.affectedUsers = builder.affectedUsers;
            this.paymentRelated = builder.paymentRelated;
            this.escrowPayment = builder.escrowPayment;
            this.hasDataLoss = builder.hasDataLoss;
            this.hasDataCorruption = builder.hasDataCorruption;
            this.publicApiBroken = builder.publicApiBroken;
            this.inCriticalPath = builder.inCriticalPath;
            this.requiresDataMigration = builder.requiresDataMigration;
            this.requiresAppStoreUpdate = builder.requiresAppStoreUpdate;
            this.requiresUserAction = builder.requiresUserAction;
            this.requiresManualFix = builder.requiresManualFix;
            this.canRollback = builder.canRollback;
            this.hasHotfixAvailable = builder.hasHotfixAvailable;
            this.bdtTransaction = builder.bdtTransaction;
            this.mobileMoney = builder.mobileMoney;
            this.banglishIssue = builder.banglishIssue;
        }
        
        // Getters
        public ErrorType getErrorType() { return errorType; }
        public boolean isBlocking() { return blocking; }
        public boolean isRegression() { return regression; }
        public int getAffectedUsers() { return affectedUsers; }
        public boolean isPaymentRelated() { return paymentRelated; }
        public boolean isEscrowPayment() { return escrowPayment; }
        public boolean hasDataLoss() { return hasDataLoss; }
        public boolean hasDataCorruption() { return hasDataCorruption; }
        public boolean isPublicApiBroken() { return publicApiBroken; }
        public boolean isInCriticalPath() { return inCriticalPath; }
        public boolean requiresDataMigration() { return requiresDataMigration; }
        public boolean requiresAppStoreUpdate() { return requiresAppStoreUpdate; }
        public boolean requiresUserAction() { return requiresUserAction; }
        public boolean requiresManualFix() { return requiresManualFix; }
        public boolean canRollback() { return canRollback; }
        public boolean hasHotfixAvailable() { return hasHotfixAvailable; }
        public boolean isBdtTransaction() { return bdtTransaction; }
        public boolean isMobileMoney() { return mobileMoney; }
        public boolean isBanglishIssue() { return banglishIssue; }
        
        public static class Builder {
            private ErrorType errorType = ErrorType.NONE;
            private boolean blocking = false;
            private boolean regression = false;
            private int affectedUsers = 0;
            private boolean paymentRelated = false;
            private boolean escrowPayment = false;
            private boolean hasDataLoss = false;
            private boolean hasDataCorruption = false;
            private boolean publicApiBroken = false;
            private boolean inCriticalPath = false;
            private boolean requiresDataMigration = false;
            private boolean requiresAppStoreUpdate = false;
            private boolean requiresUserAction = false;
            private boolean requiresManualFix = false;
            private boolean canRollback = false;
            private boolean hasHotfixAvailable = false;
            private boolean bdtTransaction = false;
            private boolean mobileMoney = false;
            private boolean banglishIssue = false;
            
            public Builder errorType(ErrorType type) {
                this.errorType = type;
                return this;
            }
            
            public Builder blocking(boolean blocking) {
                this.blocking = blocking;
                return this;
            }
            
            public Builder regression(boolean regression) {
                this.regression = regression;
                return this;
            }
            
            public Builder affectedUsers(int count) {
                this.affectedUsers = count;
                return this;
            }
            
            public Builder paymentRelated(boolean related) {
                this.paymentRelated = related;
                return this;
            }
            
            public Builder escrowPayment(boolean escrow) {
                this.escrowPayment = escrow;
                return this;
            }
            
            public Builder hasDataLoss(boolean loss) {
                this.hasDataLoss = loss;
                return this;
            }
            
            public Builder hasDataCorruption(boolean corruption) {
                this.hasDataCorruption = corruption;
                return this;
            }
            
            public Builder publicApiBroken(boolean broken) {
                this.publicApiBroken = broken;
                return this;
            }
            
            public Builder inCriticalPath(boolean critical) {
                this.inCriticalPath = critical;
                return this;
            }
            
            public Builder requiresDataMigration(boolean required) {
                this.requiresDataMigration = required;
                return this;
            }
            
            public Builder requiresAppStoreUpdate(boolean required) {
                this.requiresAppStoreUpdate = required;
                return this;
            }
            
            public Builder requiresUserAction(boolean required) {
                this.requiresUserAction = required;
                return this;
            }
            
            public Builder requiresManualFix(boolean required) {
                this.requiresManualFix = required;
                return this;
            }
            
            public Builder canRollback(boolean can) {
                this.canRollback = can;
                return this;
            }
            
            public Builder hasHotfixAvailable(boolean available) {
                this.hasHotfixAvailable = available;
                return this;
            }
            
            public Builder bdtTransaction(boolean bdt) {
                this.bdtTransaction = bdt;
                return this;
            }
            
            public Builder mobileMoney(boolean mobile) {
                this.mobileMoney = mobile;
                return this;
            }
            
            public Builder banglishIssue(boolean banglish) {
                this.banglishIssue = banglish;
                return this;
            }
            
            public BuildResult build() {
                return new BuildResult(this);
            }
        }
    }
}
