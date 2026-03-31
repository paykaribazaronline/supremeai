package org.example.service;

import java.util.*;

/**
 * ErrorAnalyzer - Analyzes detected errors and assigns them to agents for fixing
 * 
 * Purpose: Categorize errors, estimate fix difficulty, assign severity levels,
 * and determine which agent is best suited to fix each error.
 * 
 * Integration: Called by AutoFixLoopService after ErrorDetector identifies errors
 */
public class ErrorAnalyzer {
    
    public enum ErrorSeverity {
        CRITICAL,  // Build fails, app crashes
        HIGH,      // Significant functionality broken
        MEDIUM,    // Feature degraded, workaround exists
        LOW        // Minor issue, no user impact
    }
    
    public static class AnalyzedError {
        public String errorId;
        public String type;
        public String message;
        public ErrorSeverity severity;
        public float fixDifficulty;  // 0.0 = trivial, 1.0 = very complex
        public String recommendedAgent; // Which agent to assign
        public List<String> fixStrategies;
        public int estimatedTimeMs;
        
        public AnalyzedError(String errorId, String type, String message) {
            this.errorId = errorId;
            this.type = type;
            this.message = message;
            this.fixStrategies = new ArrayList<>();
        }
    }
    
    /**
     * Analyze a list of detected errors
     */
    public List<AnalyzedError> analyzeErrors(List<ErrorDetector.DetectedError> detectedErrors) {
        List<AnalyzedError> analyzed = new ArrayList<>();
        
        for (ErrorDetector.DetectedError error : detectedErrors) {
            analyzed.add(analyzeError(error));
        }
        
        return analyzed;
    }
    
    /**
     * Analyze a single error
     */
    public AnalyzedError analyzeError(ErrorDetector.DetectedError detected) {
        AnalyzedError analyzed = new AnalyzedError(
            detected.errorId,
            detected.type,
            detected.message
        );
        
        // Determine severity based on error type
        if (detected.type.equals("COMPILATION")) {
            analyzed.severity = determineSeverityForCompilation(detected.message);
        } else if (detected.type.equals("RUNTIME")) {
            analyzed.severity = determineSeverityForRuntime(detected.message);
        } else if (detected.type.equals("SECURITY")) {
            analyzed.severity = ErrorSeverity.HIGH; // Security issues are always high priority
        } else if (detected.type.equals("CONFIG")) {
            analyzed.severity = ErrorSeverity.MEDIUM;
        } else {
            analyzed.severity = ErrorSeverity.MEDIUM;
        }
        
        // Estimate fix difficulty (inverse of fixability)
        analyzed.fixDifficulty = 1.0f - detected.fixability;
        
        // Assign fix strategies
        analyzed.fixStrategies.addAll(detected.suggestedFixes);
        
        // Recommend agent based on error type
        analyzed.recommendedAgent = assignAgent(detected.type, analyzed.severity);
        
        // Estimate time to fix
        analyzed.estimatedTimeMs = estimateFixTime(analyzed.severity, analyzed.fixDifficulty);
        
        return analyzed;
    }
    
    /**
     * Determine error severity for compilation errors
     */
    private ErrorSeverity determineSeverityForCompilation(String message) {
        if (message.contains("cannot find symbol")) {
            return ErrorSeverity.HIGH; // Blocks compilation
        }
        if (message.contains("incompatible types") || message.contains("type mismatch")) {
            return ErrorSeverity.HIGH;
        }
        if (message.contains("syntax") || message.contains("[error]")) {
            return ErrorSeverity.CRITICAL; // Blocks build
        }
        return ErrorSeverity.MEDIUM;
    }
    
    /**
     * Determine error severity for runtime errors
     */
    private ErrorSeverity determineSeverityForRuntime(String message) {
        if (message.contains("NullPointerException")) {
            return ErrorSeverity.CRITICAL; // App crashes
        }
        if (message.contains("OutOfMemoryError") || message.contains("StackOverflowError")) {
            return ErrorSeverity.CRITICAL;
        }
        if (message.contains("ArrayIndexOutOfBoundsException") || 
            message.contains("ClassNotFoundException")) {
            return ErrorSeverity.HIGH;
        }
        return ErrorSeverity.MEDIUM;
    }
    
    /**
     * Assign the best agent for fixing this error
     */
    private String assignAgent(String errorType, ErrorSeverity severity) {
        switch (errorType) {
            case "COMPILATION":
                return "Builder"; // Builder agent handles compilation
            case "RUNTIME":
                return "Reviewer"; // Reviewer validates runtime correctness
            case "SECURITY":
                return "Reviewer"; // Reviewer checks security implications
            case "CONFIG":
                if (severity == ErrorSeverity.CRITICAL) {
                    return "Architect"; // Architect handles architectural config
                } else {
                    return "Builder";
                }
            default:
                return "Builder";
        }
    }
    
    /**
     * Estimate time to fix based on severity and difficulty
     */
    private int estimateFixTime(ErrorSeverity severity, float fixDifficulty) {
        int basetime = 0;
        
        switch (severity) {
            case CRITICAL:
                basetime = 500;
                break;
            case HIGH:
                basetime = 800;
                break;
            case MEDIUM:
                basetime = 1200;
                break;
            case LOW:
                basetime = 2000;
                break;
        }
        
        // Apply difficulty multiplier (0.2 = trivial, 1.0 = complex)
        return (int) (basetime * (0.2f + fixDifficulty * 0.8f));
    }
    
    /**
     * Whether the error should be auto-fixed immediately
     */
    public boolean shouldAutoFix(AnalyzedError error, float confidenceThreshold) {
        // Auto-fix if:
        // 1. Severity is HIGH or CRITICAL
        // 2. Fix difficulty is low (< 0.5)
        // 3. Confidence that fix exists is above threshold (1.0 - fixDifficulty)
        
        float confidence = 1.0f - error.fixDifficulty;
        boolean hasSufficientConfidence = confidence >= confidenceThreshold;
        boolean isSeverus = error.severity == ErrorSeverity.CRITICAL || 
                           error.severity == ErrorSeverity.HIGH;
        boolean isEasyToFix = error.fixDifficulty < 0.5f;
        
        return hasSufficientConfidence && (isSeverus || isEasyToFix);
    }
    
    /**
     * Get human-readable error summary
     */
    public String getSummary(AnalyzedError error) {
        return String.format(
            "[%s] %s (Severity: %s, Assign to: %s, Est. Fix: %dms)",
            error.type,
            error.message,
            error.severity,
            error.recommendedAgent,
            error.estimatedTimeMs
        );
    }
}
