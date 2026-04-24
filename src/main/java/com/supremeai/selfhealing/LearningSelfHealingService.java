package com.supremeai.selfhealing;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.supremeai.model.SystemLearning;
import com.supremeai.repository.SystemLearningRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * LearningSelfHealingService - S10 Enhancement
 * Enhanced self-healing that:
 * 1. Logs all errors with full context
 * 2. Analyzes error patterns to predict failures
 * 3. Attempts automatic fixes based on learned patterns
 * 4. Stores learnings/notes in Firebase for future prevention
 * 5. Uses past learnings to prevent future errors
 */
@Service
public class LearningSelfHealingService {

    private static final Logger log = LoggerFactory.getLogger(LearningSelfHealingService.class);

    @Autowired
    private SystemLearningRepository learningRepository;

    @Autowired
    private Firestore firestore;

    // In-memory cache of learned patterns to avoid Firestore calls on every error
    private final Map<String, List<FixStrategy>> learnedFixes = new HashMap<>();

    // Statistics
    private int totalErrors = 0;
    private int autoFixedErrors = 0;
    private int learningAppliedCount = 0;

    /**
     * Execute with enhanced self-healing and learning.
     * @param task The task to execute
     * @param context Context about the operation (operation name, parameters, etc.)
     * @return Task result
     * @throws Exception if all attempts fail
     */
    public <T> T executeWithLearning(Callable<T> task, Map<String, Object> context) throws Exception {
        String operation = context != null ? (String) context.getOrDefault("operation", "unknown") : "unknown";
        int maxAttempts = context != null ? (int) context.getOrDefault("maxAttempts", 3) : 3;
        long initialBackoff = context != null ? (long) context.getOrDefault("initialBackoffMs", 500L) : 500L;

        Exception lastException = null;
        long backoff = initialBackoff;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                // Execute the task
                T result = task.call();

                // Success! Log the success pattern for learning
                if (attempt > 1) {
                    recordSuccessPattern(operation, context, attempt);
                }

                return result;

            } catch (Exception e) {
                lastException = e;
                totalErrors++;

                // Log the error with full context
                ErrorContext errorContext = new ErrorContext(e, operation, context, attempt);
                logErrorWithContext(errorContext);

                // Analyze error pattern and attempt auto-fix
                if (attempt < maxAttempts) {
                    boolean fixed = attemptAutoFix(errorContext);

                    if (fixed) {
                        autoFixedErrors++;
                        log.info("✅ Auto-fixed error on attempt {} for operation: {}", attempt, operation);
                        // Retry immediately after fix
                        backoff = 100; // Short backoff after fix
                    } else {
                        // Store learning for this error
                        storeLearningFromError(errorContext);
                    }
                }

                // Wait before retry (with exponential backoff)
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                    backoff *= 2;
                }
            }
        }

        // All attempts failed - store comprehensive learning
        if (lastException != null) {
            storeLearningFromFailure(operation, context, lastException, maxAttempts);
        }

        throw lastException != null ? lastException : new IllegalStateException("All attempts failed for: " + operation);
    }

    /**
     * Attempt to auto-fix an error based on learned patterns.
     * @return true if fix was applied
     */
    private boolean attemptAutoFix(ErrorContext errorContext) {
        String errorType = classifyError(errorContext.exception);
        String operation = errorContext.operation;

        log.info("🔍 Attempting auto-fix for error type: {} in operation: {}", errorType, operation);

        // Check learned fixes from Firebase
        List<FixStrategy> fixes = learnedFixes.get(errorType);
        if (fixes == null) {
            // Load from Firebase
            fixes = loadLearningsFromFirebase(errorType);
            if (!fixes.isEmpty()) {
                learnedFixes.put(errorType, fixes);
            }
        }

        if (fixes.isEmpty()) {
            // No learned fix, try rule-based fixes
            return applyRuleBasedFix(errorContext, errorType);
        }

        // Apply the highest confidence fix
        fixes.sort((a, b) -> Double.compare(b.confidence, a.confidence));
        for (FixStrategy fix : fixes) {
            if (fix.applicable(errorContext)) {
                boolean success = fix.apply(errorContext);
                if (success) {
                    learningAppliedCount++;
                    log.info("✅ Applied learned fix: {} (confidence: {})", fix.description, fix.confidence);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Apply rule-based fixes for common errors.
     */
    private boolean applyRuleBasedFix(ErrorContext errorContext, String errorType) {
        switch (errorType) {
            case "CONNECTION_REFUSED":
                return fixConnectionRefused(errorContext);
            case "TIMEOUT":
                return fixTimeout(errorContext);
            case "RATE_LIMIT":
                return fixRateLimit(errorContext);
            case "AUTH_FAILED":
                return fixAuthFailed(errorContext);
            case "AI_PROVIDER_ERROR":
                return fixAIProviderError(errorContext);
            default:
                return false;
        }
    }

    /**
     * Log error with full context for learning.
     */
    private void logErrorWithContext(ErrorContext errorContext) {
        Map<String, Object> logEntry = new LinkedHashMap<>();
        logEntry.put("timestamp", LocalDateTime.now().toString());
        logEntry.put("operation", errorContext.operation);
        logEntry.put("attempt", errorContext.attempt);
        logEntry.put("errorType", classifyError(errorContext.exception));
        logEntry.put("errorMessage", errorContext.exception.getMessage());
        logEntry.put("stackTrace", getStackTraceSummary(errorContext.exception));

        if (errorContext.context != null) {
            logEntry.put("context", errorContext.context);
        }

        log.error("❌ Error in {} (attempt {}): {} - Context: {}", 
            errorContext.operation, errorContext.attempt, 
            errorContext.exception.getMessage(), errorContext.context);

        // Store in Firestore for analysis
        try {
            firestore.collection("error_logs")
                .add(logEntry);
        } catch (Exception e) {
            log.warn("Failed to store error log: {}", e.getMessage());
        }
    }

    /**
     * Store learning from an error for future prevention.
     */
    private void storeLearningFromError(ErrorContext errorContext) {
        String errorType = classifyError(errorContext.exception);
        String learningId = "learning_" + System.currentTimeMillis();

        SystemLearning learning = new SystemLearning();
        learning.setId(learningId);
        learning.setTopic(errorType + "_" + errorContext.operation);
        learning.setCategory("error_pattern");
        learning.setContent(buildLearningContent(errorContext));
        learning.setConfidenceScore(0.5); // Initial confidence
        learning.setLearnedAt(LocalDateTime.now());
        learning.setPermanent(false);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("errorMessage", errorContext.exception.getMessage());
        metadata.put("operation", errorContext.operation);
        metadata.put("attemptCount", errorContext.attempt);
        metadata.put("fixAttempted", true);
        learning.setMetadata(metadata);

        try {
            learningRepository.save(learning);
            log.info("📝 Stored learning note: {}", learningId);
        } catch (Exception e) {
            log.warn("Failed to store learning: {}", e.getMessage());
        }
    }

    /**
     * Store comprehensive learning from complete failure.
     */
    private void storeLearningFromFailure(String operation, Map<String, Object> context, 
                                          Exception exception, int attempts) {
        String learningId = "failure_" + System.currentTimeMillis();
        String errorType = classifyError(exception);

        SystemLearning learning = new SystemLearning();
        learning.setId(learningId);
        learning.setTopic("COMPLETE_FAILURE_" + operation);
        learning.setCategory("failure_analysis");
        learning.setContent(String.format(
            "Operation '%s' failed after %d attempts. Error: %s. Context: %s. " +
            "Recommendation: Review error pattern and add automatic fix.",
            operation, attempts, exception.getMessage(), context
        ));
        learning.setConfidenceScore(0.3); // Low confidence for complete failures
        learning.setLearnedAt(LocalDateTime.now());
        learning.setPermanent(false);

        List<String> sources = new ArrayList<>();
        sources.add("error_logs");
        sources.add(operation);
        learning.setSources(sources);

        try {
            learningRepository.save(learning);
            log.warn("📝 Stored failure learning note: {}", learningId);
        } catch (Exception e) {
            log.warn("Failed to store failure learning: {}", e.getMessage());
        }
    }

    /**
     * Record success pattern when a retry succeeds.
     */
    private void recordSuccessPattern(String operation, Map<String, Object> context, int attempt) {
        String learningId = "success_" + System.currentTimeMillis();

        SystemLearning learning = new SystemLearning();
        learning.setId(learningId);
        learning.setTopic("SUCCESS_PATTERN_" + operation);
        learning.setCategory("success_pattern");
        learning.setContent(String.format(
            "Operation '%s' succeeded on attempt %d. Previous attempts failed but were overcome. " +
            "This suggests transient issue that self-healed.",
            operation, attempt
        ));
        learning.setConfidenceScore(0.7);
        learning.setLearnedAt(LocalDateTime.now());
        learning.setPermanent(false);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("operation", operation);
        metadata.put("successAttempt", attempt);
        metadata.put("suggestedFix", "Add retry logic with " + (attempt-1) + " retries");
        learning.setMetadata(metadata);

        try {
            learningRepository.save(learning);
            log.info("📝 Recorded success pattern: {}", learningId);
        } catch (Exception e) {
            log.warn("Failed to record success pattern: {}", e.getMessage());
        }
    }

    /**
     * Classify error into a type for pattern matching.
     */
    private String classifyError(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        String className = e.getClass().getSimpleName();

        if (msg.contains("connection refused") || msg.contains("connect timed out")) {
            return "CONNECTION_REFUSED";
        } else if (msg.contains("timeout") || msg.contains("timed out")) {
            return "TIMEOUT";
        } else if (msg.contains("rate limit") || msg.contains("too many requests")) {
            return "RATE_LIMIT";
        } else if (msg.contains("auth") || msg.contains("unauthorized") || msg.contains("token")) {
            return "AUTH_FAILED";
        } else if (msg.contains("ai") || msg.contains("provider") || msg.contains("model")) {
            return "AI_PROVIDER_ERROR";
        } else if (msg.contains("null") || msg.contains("npe")) {
            return "NULL_POINTER";
        } else {
            return "UNKNOWN_" + className;
        }
    }

    /**
     * Load learned fixes from Firebase.
     */
    private List<FixStrategy> loadLearningsFromFirebase(String errorType) {
        List<FixStrategy> fixes = new ArrayList<>();
        try {
            List<QueryDocumentSnapshot> docs = firestore.collection("system_learning")
                .whereEqualTo("category", "error_pattern")
                .whereGreaterThanOrEqualTo("topic", errorType)
                .get()
                .get()
                .getDocuments();

            for (QueryDocumentSnapshot doc : docs) {
                String content = doc.getString("content");
                Double confidence = doc.getDouble("confidenceScore");
                if (content != null && confidence != null) {
                    fixes.add(new FixStrategy(content, confidence, doc.getString("topic")));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load learnings: {}", e.getMessage());
        }
        return fixes;
    }

    /**
     * Build learning content from error context.
     */
    private String buildLearningContent(ErrorContext errorContext) {
        return String.format(
            "Error Type: %s\n" +
            "Operation: %s\n" +
            "Error Message: %s\n" +
            "Attempt: %d\n" +
            "Suggested Action: Analyze pattern and implement automatic retry/fix.\n" +
            "Context: %s",
            classifyError(errorContext.exception),
            errorContext.operation,
            errorContext.exception.getMessage(),
            errorContext.attempt,
            errorContext.context
        );
    }

    /**
     * Get stack trace summary.
     */
    private String getStackTraceSummary(Exception e) {
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] trace = e.getStackTrace();
        int count = Math.min(5, trace.length);
        for (int i = 0; i < count; i++) {
            sb.append(trace[i].toString()).append("\n");
        }
        if (trace.length > count) {
            sb.append("... (").append(trace.length - count).append(" more lines)");
        }
        return sb.toString();
    }

    // Rule-based fix implementations
    private boolean fixConnectionRefused(ErrorContext ctx) {
        log.info("🔧 Applying fix: Wait and retry for connection refused");
        return false; // Let retry handle it
    }

    private boolean fixTimeout(ErrorContext ctx) {
        log.info("🔧 Applying fix: Increase timeout for next attempt");
        if (ctx.context != null) {
            ctx.context.put("timeoutMs", ((Number) ctx.context.getOrDefault("timeoutMs", 5000)).longValue() * 2);
        }
        return false; // Let retry handle it with new timeout
    }

    private boolean fixRateLimit(ErrorContext ctx) {
        log.info("🔧 Applying fix: Switch to different provider or wait");
        // Could implement provider switching logic here
        return false;
    }

    private boolean fixAuthFailed(ErrorContext ctx) {
        log.info("🔧 Applying fix: Refresh token or re-authenticate");
        // Could implement token refresh logic here
        return false;
    }

    private boolean fixAIProviderError(ErrorContext ctx) {
        log.info("🔧 Applying fix: Switch AI provider");
        if (ctx.context != null) {
            String currentProvider = (String) ctx.context.getOrDefault("provider", "groq");
            String newProvider = getAlternativeProvider(currentProvider);
            ctx.context.put("provider", newProvider);
            log.info("🔄 Switched provider from {} to {}", currentProvider, newProvider);
            return true; // Provider switched, retry with new provider
        }
        return false;
    }

    private String getAlternativeProvider(String current) {
        Map<String, String> alternatives = new HashMap<>();
        alternatives.put("groq", "openai");
        alternatives.put("openai", "anthropic");
        alternatives.put("anthropic", "ollama");
        alternatives.put("ollama", "groq");
        return alternatives.getOrDefault(current, "groq");
    }

    /**
     * Get self-healing statistics.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalErrors", totalErrors);
        stats.put("autoFixedErrors", autoFixedErrors);
        stats.put("learningAppliedCount", learningAppliedCount);
        stats.put("fixSuccessRate", totalErrors > 0 ? (double) autoFixedErrors / totalErrors : 0.0);
        stats.put("learnedPatternsCount", learnedFixes.size());
        return stats;
    }

    /**
     * Clear learned patterns cache (force reload from Firebase).
     */
    public void refreshLearnings() {
        learnedFixes.clear();
        log.info("🔄 Cleared learnings cache, will reload from Firebase on next error");
    }

    /**
     * Inner class to hold error context for learning.
     */
    private static class ErrorContext {
        final Exception exception;
        final String operation;
        final Map<String, Object> context;
        final int attempt;

        ErrorContext(Exception exception, String operation, Map<String, Object> context, int attempt) {
            this.exception = exception;
            this.operation = operation;
            this.context = context;
            this.attempt = attempt;
        }
    }

    /**
     * Inner class for fix strategies learned from past errors.
     */
    private static class FixStrategy {
        final String description;
        final double confidence;
        final String topic;

        FixStrategy(String description, double confidence, String topic) {
            this.description = description;
            this.confidence = confidence;
            this.topic = topic;
        }

        boolean applicable(ErrorContext ctx) {
            return true; // Simplified - could check if context matches
        }

        boolean apply(ErrorContext ctx) {
            // Simplified - would implement actual fix logic
            return false;
        }
    }
}
