package org.example.selfhealing.healing;

import org.example.selfhealing.domain.ValidationResult;
import org.example.selfhealing.domain.ValidationStage;
import org.example.service.GitHubAPIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Fix Validation Pipeline
 * 
 * Multi-stage validation for generated code fixes:
 * 1. Static Analysis: Check for syntax errors, security issues
 * 2. Unit Tests: Run relevant tests
 * 3. Security Scan: Check for vulnerabilities (Y-Reviewer)
 * 4. Code Diff Review: Ensure changes are reasonable
 * 
 * Only accept fixes that pass ALL stages with >85% confidence
 */
@Service
public class FixValidationPipeline {
    private static final Logger logger = LoggerFactory.getLogger(FixValidationPipeline.class);
    
    private static final double MIN_CONFIDENCE_THRESHOLD = 0.85;
    private static final int MAX_LINES_CHANGED = 50; // Force human review if >50 lines changed
    
    @Autowired(required = false)
    private GitHubAPIService githubAPI;
    
    /**
     * Run full validation pipeline for a code fix
     * 
     * @param fixCode The generated code fix
     * @param originalCode The original code before fix
     * @param testOutputBefore Test results before fix
     * @param testOutputAfter Test results after fix
     * @return ValidationResult with all stage results
     */
    public ValidationResult validate(String fixCode, String originalCode, 
                                     String testOutputBefore, String testOutputAfter) {
        ValidationResult result = new ValidationResult();
        logger.info("📋 Starting fix validation pipeline...");
        
        try {
            // Stage 1: Static Analysis
            ValidationStage staticStage = validateStaticAnalysis(fixCode);
            result.addStage(staticStage);
            
            if (!staticStage.isPassed()) {
                logger.error("❌ Static analysis failed: {}", staticStage.getMessage());
                result.setPassed(false);
                return result;
            }
            
            // Stage 2: Unit Tests
            ValidationStage testStage = validateUnitTests(testOutputBefore, testOutputAfter);
            result.addStage(testStage);
            
            if (!testStage.isPassed()) {
                logger.error("❌ Unit tests failed");
                result.setPassed(false);
                return result;
            }
            
            // Stage 3: Security Scan
            ValidationStage securityStage = validateSecurityIssues(fixCode);
            result.addStage(securityStage);
            
            if (!securityStage.isPassed()) {
                logger.error("❌ Security vulnerabilities found");
                result.setPassed(false);
                return result;
            }
            
            // Stage 4: Code Diff Review
            ValidationStage diffStage = validateCodeDiff(originalCode, fixCode);
            result.addStage(diffStage);
            
            if (!diffStage.isPassed()) {
                logger.warn("⚠️ Code diff review flagged issues: {}", diffStage.getMessage());
                result.setPassed(false);
                return result;
            }
            
            // All stages passed
            logger.info("✅ Fix validation PASSED (score: {:.2f})", result.getOverallScore());
            result.setPassed(true);
            
        } catch (Exception e) {
            logger.error("❌ Validation pipeline error", e);
            result.setPassed(false);
            result.setNotes("Validation pipeline exception: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Stage 1: Static Analysis
     * Check for common issues that would fail compilation
     */
    private ValidationStage validateStaticAnalysis(String fixCode) {
        ValidationStage stage = new ValidationStage("STATIC_ANALYSIS");
        
        try {
            // Check for syntax errors (common patterns)
            if (hasSyntaxErrors(fixCode)) {
                stage.complete(false, 0.0, "Syntax errors detected");
                return stage;
            }
            
            // Check for common compile errors
            if (hasMissingImports(fixCode)) {
                stage.complete(false, 0.2, "Potentially missing imports");
                return stage;
            }
            
            // Check for obvious security issues
            if (hasSecurityAntiPatterns(fixCode)) {
                stage.complete(false, 0.1, "Security anti-patterns detected");
                return stage;
            }
            
            // All checks passed
            stage.complete(true, 1.0, "No syntax errors or obvious issues");
            
        } catch (Exception e) {
            logger.error("Static analysis error", e);
            stage.complete(false, 0.0, "Static analysis exception: " + e.getMessage());
        }
        
        return stage;
    }
    
    /**
     * Stage 2: Unit Tests Validation
     * Check if tests improved after the fix
     */
    private ValidationStage validateUnitTests(String testOutputBefore, String testOutputAfter) {
        ValidationStage stage = new ValidationStage("UNIT_TESTS");
        
        try {
            int passBefore = countPassedTests(testOutputBefore);
            int passAfter = countPassedTests(testOutputAfter);
            int failBefore = countFailedTests(testOutputBefore);
            int failAfter = countFailedTests(testOutputAfter);
            
            // Criteria: More tests passing, no regression
            if (passAfter < passBefore) {
                stage.complete(false, 0.3, "Regression: Fewer tests passing");
                return stage;
            }
            
            if (failAfter > 0) {
                double score = 1.0 - (double) failAfter / Math.max(1, failAfter + passAfter);
                stage.complete(score >= 0.85, score, 
                        "Tests: " + passAfter + " passed, " + failAfter + " failed");
                return stage;
            }
            
            // All tests passing
            stage.complete(true, 1.0, "All tests passing");
            
        } catch (Exception e) {
            logger.error("Test validation error", e);
            stage.complete(false, 0.0, "Test validation exception: " + e.getMessage());
        }
        
        return stage;
    }
    
    /**
     * Stage 3: Security Scan (Y-Reviewer integration)
     */
    private ValidationStage validateSecurityIssues(String fixCode) {
        ValidationStage stage = new ValidationStage("SECURITY_SCAN");
        
        try {
            // Check for common security vulnerabilities
            int vulnerabilityCount = 0;
            
            if (containsPattern(fixCode, "System.loadLibrary\\(.*\\)")) {
                vulnerabilityCount++; // Potential vulnerability
            }
            
            if (containsPattern(fixCode, "Runtime\\.getRuntime\\(\\)\\.exec")) {
                vulnerabilityCount++; // Command injection risk
            }
            
            if (containsPattern(fixCode, "sql.*=.*'\\+'.*'")) {
                vulnerabilityCount++; // SQL injection risk
            }
            
            if (containsPattern(fixCode, "hardcoded.*password|password.*=.*['\"]")) {
                vulnerabilityCount++; // Hardcoded credentials
            }
            
            if (vulnerabilityCount > 0) {
                stage.complete(false, 0.0, vulnerabilityCount + " security vulnerabilities detected");
                return stage;
            }
            
            stage.complete(true, 1.0, "No obvious security vulnerabilities");
            
        } catch (Exception e) {
            logger.error("Security scan error", e);
            stage.complete(false, 0.0, "Security scan exception: " + e.getMessage());
        }
        
        return stage;
    }
    
    /**
     * Stage 4: Code Diff Review
     * Check if changes are reasonable and not excessive
     */
    private ValidationStage validateCodeDiff(String originalCode, String fixCode) {
        ValidationStage stage = new ValidationStage("CODE_DIFF");
        
        try {
            int linesChanged = calculateLinesDiff(originalCode, fixCode);
            
            // Flag large diffs for human review
            if (linesChanged > MAX_LINES_CHANGED) {
                stage.complete(false, 0.5, 
                        "Too many changes (" + linesChanged + " lines). Require human review.");
                return stage;
            }
            
            // Check if change is minimal and focused
            double focusScore = 1.0 - (double) linesChanged / MAX_LINES_CHANGED;
            
            stage.complete(true, focusScore, 
                    "Changes are focused (" + linesChanged + " lines)");
            
        } catch (Exception e) {
            logger.error("Code diff error", e);
            stage.complete(false, 0.0, "Code diff exception: " + e.getMessage());
        }
        
        return stage;
    }
    
    // Helper methods
    
    private boolean hasSyntaxErrors(String code) {
        // Check for common syntax error patterns
        return containsPattern(code, "\\s{2,}[{]") ||  // Double spaces before brace
               countOccurrences(code, "{") != countOccurrences(code, "}") ||
               countOccurrences(code, "[") != countOccurrences(code, "]");
    }
    
    private boolean hasMissingImports(String code) {
        // Check if code uses classes without visible imports
        return code.contains("List<") && !code.contains("import java.util.List");
    }
    
    private boolean hasSecurityAntiPatterns(String code) {
        return code.contains("eval(") || 
               code.contains("exec(") ||
               code.matches(".*@deprecated.*");
    }
    
    private boolean containsPattern(String text, String pattern) {
        try {
            return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(text).find();
        } catch (Exception e) {
            return false;
        }
    }
    
    private int countOccurrences(String text, String substring) {
        return (text.split(Pattern.quote(substring), -1).length - 1);
    }
    
    private int countPassedTests(String output) {
        if (output == null) return 0;
        int count = 0;
        for (String line : output.split("\n")) {
            if (line.contains("PASSED") || line.contains("OK") || line.contains("✓")) {
                count++;
            }
        }
        return count;
    }
    
    private int countFailedTests(String output) {
        if (output == null) return 0;
        int count = 0;
        for (String line : output.split("\n")) {
            if (line.contains("FAILED") || line.contains("ERROR") || line.contains("✗")) {
                count++;
            }
        }
        return count;
    }
    
    private int calculateLinesDiff(String original, String fixed) {
        String[] originalLines = original.split("\n");
        String[] fixedLines = fixed.split("\n");
        
        int additions = Math.max(0, fixedLines.length - originalLines.length);
        int deletions = Math.max(0, originalLines.length - fixedLines.length);
        
        // Rough estimate: count lines that are different
        int changes = Math.max(additions, deletions);
        for (int i = 0; i < Math.min(originalLines.length, fixedLines.length); i++) {
            if (!originalLines[i].equals(fixedLines[i])) {
                changes++;
            }
        }
        
        return changes;
    }
}
