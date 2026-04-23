package com.supremeai.service;

import com.supremeai.learning.knowledge.GlobalKnowledgeBase;
import com.supremeai.learning.knowledge.SolutionMemory;
import com.supremeai.ml.EnhancedRandomForestPredictor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Root Cause Analysis and Automatic Self-Correction Service.
 * Analyzes failures to identify root causes and automatically applies fixes.
 */
@Service
public class RootCauseAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(RootCauseAnalysisService.class);

    @Autowired
    private GlobalKnowledgeBase globalKnowledgeBase;

    @Autowired
    private EnhancedRandomForestPredictor failurePredictor;

    // Root cause patterns and their fixes
    private final Map<String, RootCausePattern> rootCausePatterns = new ConcurrentHashMap<>();
    private final Map<String, CorrectionRecord> correctionHistory = new ConcurrentHashMap<>();

    // Error signature extraction patterns
    private static final Pattern STACK_TRACE_PATTERN = Pattern.compile("([a-zA-Z0-9_$]+)\\.([a-zA-Z0-9_$]+)\\(([^:]+):(\\d+)\\)");
    private static final Pattern ERROR_TYPE_PATTERN = Pattern.compile("([a-zA-Z]+Error|[a-zA-Z]+Exception):\\s*(.+)");
    private static final Pattern NULL_POINTER_PATTERN = Pattern.compile("NullPointer|null");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("cannot find symbol|unresolved symbol|Import.*not found");

    public RootCauseAnalysisService() {
        initializeRootCausePatterns();
    }

    /**
     * Initialize common root cause patterns.
     */
    private void initializeRootCausePatterns() {
        // Null Pointer Issues
        rootCausePatterns.put("null_pointer", new RootCausePattern(
            "null_pointer",
            "Null Pointer Exception",
            Pattern.compile("NullPointer|null"),
            CorrectionAction.AUTO_FIX_NULL,
            0.9
        ));

        // Missing Import/Dependency
        rootCausePatterns.put("missing_import", new RootCausePattern(
            "missing_import",
            "Missing Import or Dependency",
            IMPORT_PATTERN,
            CorrectionAction.ADD_IMPORT,
            0.85
        ));

        // Syntax Errors
        rootCausePatterns.put("syntax_error", new RootCausePattern(
            "syntax_error",
            "Syntax Error",
            Pattern.compile("syntax error|unexpected token|missing semicolon"),
            CorrectionAction.FIX_SYNTAX,
            0.95
        ));

        // Type Mismatch
        rootCausePatterns.put("type_mismatch", new RootCausePattern(
            "type_mismatch",
            "Type Mismatch",
            Pattern.compile("incompatible types|cannot be applied to|type mismatch"),
            CorrectionAction.FIX_TYPE,
            0.8
        ));

        // Division by Zero
        rootCausePatterns.put("division_by_zero", new RootCausePattern(
            "division_by_zero",
            "Division by Zero",
            Pattern.compile("/ 0|division by zero|ArithmeticException"),
            CorrectionAction.ADD_ZERO_CHECK,
            0.98
        ));

        // Array Out of Bounds
        rootCausePatterns.put("array_out_of_bounds", new RootCausePattern(
            "array_out_of_bounds",
            "Array Index Out of Bounds",
            Pattern.compile("ArrayIndexOutOfBounds|index out of range"),
            CorrectionAction.ADD_BOUNDS_CHECK,
            0.95
        ));
    }

    /**
     * Analyze an error and identify root cause.
     */
    public RootCauseAnalysis analyzeError(String errorSignature, String errorMessage, String codeContext) {
        log.info("Analyzing error: {}", errorSignature);

        // Extract error features
        Map<EnhancedRandomForestPredictor.FeatureType, Double> features = extractErrorFeatures(errorMessage, codeContext);

        // Predict if this will be a failure
        EnhancedRandomForestPredictor.FailurePrediction prediction = failurePredictor.predict(errorSignature, features);

        // Identify root cause
        RootCausePattern rootCause = identifyRootCause(errorMessage, codeContext);

        // Generate correction
        CorrectionAction correction = rootCause != null ? rootCause.suggestedAction : CorrectionAction.MANUAL_REVIEW;
        String correctedCode = null;
        boolean canAutoFix = false;

        if (rootCause != null && rootCause.confidence > 0.8) {
            correctedCode = applyAutoCorrection(codeContext, rootCause, errorMessage);
            canAutoFix = correctedCode != null && !correctedCode.equals(codeContext);
        }

        // Record analysis
        RootCauseAnalysis analysis = new RootCauseAnalysis(
            errorSignature,
            rootCause != null ? rootCause.name : "unknown",
            rootCause != null ? rootCause.description : "Could not identify root cause",
            rootCause != null ? rootCause.confidence : 0.0,
            prediction.probability,
            correction,
            correctedCode,
            canAutoFix,
            LocalDateTime.now()
        );

        // Store for learning
        correctionHistory.put(errorSignature, new CorrectionRecord(
            errorSignature,
            rootCause != null ? rootCause.id : "unknown",
            correction,
            canAutoFix,
            false,
            System.currentTimeMillis()
        ));

        return analysis;
    }

    /**
     * Extract features from error for ML prediction.
     */
    private Map<EnhancedRandomForestPredictor.FeatureType, Double> extractErrorFeatures(String errorMessage, String codeContext) {
        Map<EnhancedRandomForestPredictor.FeatureType, Double> features = new HashMap<>();

        // ERROR_FREQUENCY - how often this error occurred
        CorrectionRecord history = correctionHistory.get(errorMessage.hashCode() + "");
        features.put(EnhancedRandomForestPredictor.FeatureType.ERROR_FREQUENCY,
            history != null ? 1.0 : 0.0);

        // TIME_SINCE_LAST_ERROR
        features.put(EnhancedRandomForestPredictor.FeatureType.TIME_SINCE_LAST_ERROR, 1.0);

        // ERROR_MESSAGE_LENGTH
        features.put(EnhancedRandomForestPredictor.FeatureType.ERROR_MESSAGE_LENGTH,
            (double) errorMessage.length() / 1000.0); // Normalize

        // STACK_TRACE_DEPTH
        Matcher matcher = STACK_TRACE_PATTERN.matcher(errorMessage);
        int depth = 0;
        while (matcher.find()) depth++;
        features.put(EnhancedRandomForestPredictor.FeatureType.STACK_TRACE_DEPTH, (double) depth);

        // CODE_COMPLEXITY (simple heuristic: lines of code)
        if (codeContext != null) {
            int lines = codeContext.split("\n").length;
            features.put(EnhancedRandomForestPredictor.FeatureType.CODE_COMPLEXITY, lines / 100.0);
        } else {
            features.put(EnhancedRandomForestPredictor.FeatureType.CODE_COMPLEXITY, 0.0);
        }

        // Default values for other features
        features.put(EnhancedRandomForestPredictor.FeatureType.PROVIDER_SUCCESS_RATE, 0.8);
        features.put(EnhancedRandomForestPredictor.FeatureType.USER_EXPERIENCE_LEVEL, 0.5);
        features.put(EnhancedRandomForestPredictor.FeatureType.SYSTEM_LOAD, 0.3);
        features.put(EnhancedRandomForestPredictor.FeatureType.API_RESPONSE_TIME, 0.2);
        features.put(EnhancedRandomForestPredictor.FeatureType.MEMORY_USAGE, 0.4);

        return features;
    }

    /**
     * Identify root cause from error message and code context.
     */
    private RootCausePattern identifyRootCause(String errorMessage, String codeContext) {
        String lowerError = errorMessage.toLowerCase(Locale.ROOT);

        double bestConfidence = 0.0;
        RootCausePattern bestMatch = null;

        for (RootCausePattern pattern : rootCausePatterns.values()) {
            Matcher matcher = pattern.pattern.matcher(errorMessage);
            if (matcher.find()) {
                if (pattern.confidence > bestConfidence) {
                    bestConfidence = pattern.confidence;
                    bestMatch = pattern;
                }
            }
        }

        // Check for null pointer in code context
        if (bestMatch == null && codeContext != null) {
            if (NULL_POINTER_PATTERN.matcher(lowerError).find()) {
                bestMatch = rootCausePatterns.get("null_pointer");
            }
        }

        return bestMatch;
    }

    /**
     * Apply automatic correction based on root cause.
     */
    private String applyAutoCorrection(String code, RootCausePattern rootCause, String errorMessage) {
        if (code == null || code.isEmpty()) return null;

        try {
            switch (rootCause.suggestedAction) {
                case ADD_IMPORT:
                    return fixMissingImport(code, errorMessage);
                case FIX_SYNTAX:
                    return fixSyntaxError(code, errorMessage);
                case FIX_TYPE:
                    return fixTypeMismatch(code, errorMessage);
                case ADD_ZERO_CHECK:
                    return addZeroCheck(code);
                case ADD_BOUNDS_CHECK:
                    return addBoundsCheck(code);
                case AUTO_FIX_NULL:
                    return fixNullPointer(code);
                default:
                    return code;
            }
        } catch (Exception e) {
            log.error("Failed to apply auto-correction for action {}: {}", rootCause.suggestedAction, e.getMessage(), e);
            return code;
        }
    }

    /**
     * Fix missing import.
     */
    private String fixMissingImport(String code, String errorMessage) {
        // Extract the missing class/package from error
        Pattern missingClass = Pattern.compile("cannot find symbol.*?([a-zA-Z_][a-zA-Z0-9_.]*)");
        Matcher matcher = missingClass.matcher(errorMessage);
        if (matcher.find()) {
            String className = matcher.group(1);
            String packageName = inferPackageName(className);
            if (packageName != null) {
                String importStatement = "import " + packageName + "." + className + ";";
                if (!code.contains(importStatement)) {
                    return importStatement + "\n" + code;
                }
            }
        }
        return code;
    }

    /**
     * Fix syntax error (basic).
     */
    private String fixSyntaxError(String code, String errorMessage) {
        // Simple fixes for common syntax errors
        String fixed = code;

        // Add missing semicolons at end of lines
        fixed = fixed.replaceAll("([^;{}\\s])\n", "$1;\n");

        // Fix missing closing braces
        int openBraces = fixed.split("\\{", -1).length - 1;
        int closeBraces = fixed.split("}", -1).length - 1;
        while (closeBraces < openBraces) {
            fixed += "\n}";
            closeBraces++;
        }

        return fixed;
    }

    /**
     * Fix type mismatch.
     */
    private String fixTypeMismatch(String code, String errorMessage) {
        // This would require more sophisticated analysis
        // For now, return code as-is
        return code;
    }

    /**
     * Add zero check before division.
     */
    private String addZeroCheck(String code) {
        // Find division operations and add checks
        Pattern divPattern = Pattern.compile("([a-zA-Z0-9_]+)\\s*/\\s*([a-zA-Z0-9_]+)");
        Matcher matcher = divPattern.matcher(code);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String denominator = matcher.group(2);
            String replacement = "(((" + denominator + ") != 0) ? (" +
                matcher.group(1) + " / " + denominator + ") : 0)";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Add bounds check for array access.
     */
    private String addBoundsCheck(String code) {
        // Find array access patterns
        Pattern arrayPattern = Pattern.compile("([a-zA-Z0-9_]+)\\[([^\\]]+)\\]");
        Matcher matcher = arrayPattern.matcher(code);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String array = matcher.group(1);
            String index = matcher.group(2);
            String replacement = "((" + index + " >= 0 && " + index + " < " + array +
                ".length) ? " + matcher.group(0) + " : null)";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Fix null pointer issues.
     */
    private String fixNullPointer(String code) {
        // Add null checks before method calls
        Pattern methodCall = Pattern.compile("([a-zA-Z0-9_]+)\\.([a-zA-Z0-9_]+\\([^)]*\\))");
        Matcher matcher = methodCall.matcher(code);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String obj = matcher.group(1);
            String method = matcher.group(2);
            String replacement = "(" + obj + " != null ? " + obj + "." + method + " : null)";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Infer package name from class name (simplified).
     */
    private String inferPackageName(String className) {
        // Common Java packages
        Map<String, String> commonClasses = Map.of(
            "ArrayList", "java.util",
            "HashMap", "java.util",
            "List", "java.util",
            "Map", "java.util",
            "Scanner", "java.util",
            "File", "java.io",
            "IOException", "java.io",
            "LocalDateTime", "java.time"
        );
        return commonClasses.get(className);
    }

    /**
     * Learn from successful correction to improve future auto-fixes.
     */
    public void recordSuccessfulCorrection(String errorSignature, String correctedCode) {
        CorrectionRecord record = correctionHistory.get(errorSignature);
        if (record != null) {
            record.wasSuccessful = true;
            log.info("Recorded successful auto-correction for: {}", errorSignature);

            // Update Global Knowledge Base
            globalKnowledgeBase.recordSuccessWithPermission(
                errorSignature,
                correctedCode,
                "RootCauseAnalysisService",
                0,
                0.9
            );
        }
    }

    /**
     * Get analysis statistics.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAnalyses", correctionHistory.size());
        stats.put("autoFixablePatterns", rootCausePatterns.size());
        stats.put("successfulCorrections", correctionHistory.values().stream()
            .filter(r -> r.wasSuccessful).count());
        return stats;
    }

    // ── Data Classes ──────────────────────────────────────────────────────────

    public static class RootCauseAnalysis {
        public final String errorSignature;
        public final String rootCauseType;
        public final String rootCauseDescription;
        public final double rootCauseConfidence;
        public final double failureProbability;
        public final CorrectionAction suggestedAction;
        public final String correctedCode;
        public final boolean canAutoFix;
        public final LocalDateTime timestamp;

        public RootCauseAnalysis(String errorSignature, String rootCauseType,
                               String rootCauseDescription, double rootCauseConfidence,
                               double failureProbability, CorrectionAction suggestedAction,
                               String correctedCode, boolean canAutoFix, LocalDateTime timestamp) {
            this.errorSignature = errorSignature;
            this.rootCauseType = rootCauseType;
            this.rootCauseDescription = rootCauseDescription;
            this.rootCauseConfidence = rootCauseConfidence;
            this.failureProbability = failureProbability;
            this.suggestedAction = suggestedAction;
            this.correctedCode = correctedCode;
            this.canAutoFix = canAutoFix;
            this.timestamp = timestamp;
        }
    }

    private static class RootCausePattern {
        String id;
        String name;
        String description;
        Pattern pattern;
        CorrectionAction suggestedAction;
        double confidence;

        RootCausePattern(String id, String name, Pattern pattern, CorrectionAction action, double confidence) {
            this.id = id;
            this.name = name;
            this.description = name;
            this.pattern = pattern;
            this.suggestedAction = action;
            this.confidence = confidence;
        }
    }

    private static class CorrectionRecord {
        String errorSignature;
        String rootCauseId;
        CorrectionAction action;
        boolean canAutoFix;
        boolean wasSuccessful;
        long timestamp;

        CorrectionRecord(String errorSignature, String rootCauseId, CorrectionAction action,
                       boolean canAutoFix, boolean wasSuccessful, long timestamp) {
            this.errorSignature = errorSignature;
            this.rootCauseId = rootCauseId;
            this.action = action;
            this.canAutoFix = canAutoFix;
            this.wasSuccessful = wasSuccessful;
            this.timestamp = timestamp;
        }
    }

    public enum CorrectionAction {
        AUTO_FIX_NULL,
        ADD_IMPORT,
        FIX_SYNTAX,
        FIX_TYPE,
        ADD_ZERO_CHECK,
        ADD_BOUNDS_CHECK,
        MANUAL_REVIEW
    }
}
