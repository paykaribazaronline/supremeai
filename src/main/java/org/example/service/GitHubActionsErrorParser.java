package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub Actions Error Parser
 * Extracts specific test failures and compilation errors from CI/CD logs
 * Provides structured error info to AI consensus system for better feedback
 */
@Service
public class GitHubActionsErrorParser {
    private static final Logger logger = LoggerFactory.getLogger(GitHubActionsErrorParser.class);
    
    /**
     * Parse GitHub Actions job output for errors
     */
    public Map<String, Object> parseJobOutput(String jobOutput) {
        logger.info("🔍 Parsing GitHub Actions job output for errors");
        
        Map<String, Object> analysis = new HashMap<>();
        
        // Detect error type
        String errorType = detectErrorType(jobOutput);
        analysis.put("errorType", errorType);
        
        // Extract error details based on type
        List<String> errors = new ArrayList<>();
        
        if (errorType.equals("COMPILATION_ERROR")) {
            errors.addAll(extractCompilationErrors(jobOutput));
        } else if (errorType.equals("TEST_FAILURE")) {
            errors.addAll(extractTestFailures(jobOutput));
        } else if (errorType.equals("RUNTIME_ERROR")) {
            errors.addAll(extractRuntimeErrors(jobOutput));
        } else if (errorType.equals("BUILD_FAILURE")) {
            errors.addAll(extractBuildErrors(jobOutput));
        }
        
        analysis.put("errors", errors);
        analysis.put("errorCount", errors.size());
        analysis.put("summary", generateErrorSummary(errorType, errors));
        
        return analysis;
    }
    
    /**
     * Detect the type of error from output
     */
    private String detectErrorType(String output) {
        if (output.contains("cannot find symbol") || output.contains("package ") || output.contains("error:")) {
            return "COMPILATION_ERROR";
        }
        if (output.contains("FAILED") || output.contains("passed") || output.contains("AssertionError")) {
            return "TEST_FAILURE";
        }
        if (output.contains("Exception") || output.contains("NullPointerException") || output.contains("RuntimeException")) {
            return "RUNTIME_ERROR";
        }
        if (output.contains("BUILD FAILED")) {
            return "BUILD_FAILURE";
        }
        return "UNKNOWN_ERROR";
    }
    
    /**
     * Extract compilation errors
     */
    private List<String> extractCompilationErrors(String output) {
        List<String> errors = new ArrayList<>();
        
        // Pattern: "error: message at File.java:LineNumber"
        Pattern pattern = Pattern.compile("error:[^\\n]+(at\\s+\\w+\\.java:\\d+)?");
        Matcher matcher = pattern.matcher(output);
        
        int count = 0;
        while (matcher.find() && count < 10) {
            String error = matcher.group().trim();
            errors.add(error);
            logger.debug("📍 Found compilation error: {}", error);
            count++;
        }
        
        // Extract "cannot find symbol" errors
        Pattern symbolPattern = Pattern.compile("cannot find symbol[^\\n]*(?:\\n[^\\n]*location[^\\n]*)?");
        Matcher symbolMatcher = symbolPattern.matcher(output);
        
        while (symbolMatcher.find() && count < 10) {
            String error = symbolMatcher.group().replace("\n", " ").trim();
            errors.add(error);
            logger.debug("📍 Found symbol error: {}", error);
            count++;
        }
        
        return errors;
    }
    
    /**
     * Extract test failures
     */
    private List<String> extractTestFailures(String output) {
        List<String> errors = new ArrayList<>();
        
        // Pattern: Test class failures
        Pattern testPattern = Pattern.compile(
            "(?:Test|FAILED)[^\\n]*(?:\\n[^\\n]*(?:AssertionError|Assertion|Expected|Actual)[^\\n]*)?"
        );
        Matcher matcher = testPattern.matcher(output);
        
        int count = 0;
        while (matcher.find() && count < 10) {
            String error = matcher.group().replace("\n", " ").trim();
            errors.add(error);
            logger.debug("🧪 Found test failure: {}", error);
            count++;
        }
        
        // Extract assertion failures
        if (output.contains("AssertionError")) {
            Pattern assertPattern = Pattern.compile("AssertionError[^\\n]*");
            Matcher assertMatcher = assertPattern.matcher(output);
            while (assertMatcher.find()) {
                errors.add(assertMatcher.group().trim());
                logger.debug("🧪 Found assertion: {}", assertMatcher.group());
            }
        }
        
        return errors;
    }
    
    /**
     * Extract runtime errors
     */
    private List<String> extractRuntimeErrors(String output) {
        List<String> errors = new ArrayList<>();
        
        // Pattern: Exception types with message
        Pattern exceptionPattern = Pattern.compile(
            "(?:java\\.[a-zA-Z.]*)?(?:Exception|Error)[^\\n]*"
        );
        Matcher matcher = exceptionPattern.matcher(output);
        
        int count = 0;
        while (matcher.find() && count < 10) {
            String error = matcher.group().trim();
            // Skip if too generic
            if (!error.equals("Exception") && !error.equals("Error")) {
                errors.add(error);
                logger.debug("⚠️ Found runtime error: {}", error);
                count++;
            }
        }
        
        // Extract stack trace key lines
        Pattern stackPattern = Pattern.compile("at [a-zA-Z0-9.$]+\\([^)]+:[0-9]+\\)");
        Matcher stackMatcher = stackPattern.matcher(output);
        
        while (stackMatcher.find() && count < 10) {
            errors.add("at " + stackMatcher.group().replace("at ", ""));
            count++;
        }
        
        return errors;
    }
    
    /**
     * Extract general build errors
     */
    private List<String> extractBuildErrors(String output) {
        List<String> errors = new ArrayList<>();
        
        // Pattern: Error messages after "FAILED" or "Error"
        Pattern pattern = Pattern.compile("(?:FAILED|Error|ERROR)[^\\n]*");
        Matcher matcher = pattern.matcher(output);
        
        int count = 0;
        while (matcher.find() && count < 15) {
            String error = matcher.group().trim();
            if (!error.isEmpty()) {
                errors.add(error);
                logger.debug("❌ Found build error: {}", error);
                count++;
            }
        }
        
        return errors;
    }
    
    /**
     * Generate human-readable error summary
     */
    private String generateErrorSummary(String errorType, List<String> errors) {
        if (errors.isEmpty()) {
            return "No specific errors detected";
        }
        
        switch (errorType) {
            case "COMPILATION_ERROR":
                return "Compilation failed with " + errors.size() + " error(s). " +
                       "First error: " + errors.get(0);
            case "TEST_FAILURE":
                return "Tests failed with " + errors.size() + " failure(s). " +
                       "First failure: " + errors.get(0);
            case "RUNTIME_ERROR":
                return "Runtime error occurred: " + errors.get(0);
            case "BUILD_FAILURE":
                return "Build process failed with " + errors.size() + " issue(s)";
            default:
                return "Unknown error type with " + errors.size() + " occurrence(s)";
        }
    }
    
    /**
     * Get detailed error report for feedback to AI consensus
     */
    public Map<String, Object> getDetailedErrorReport(String jobOutput) {
        Map<String, Object> analysis = parseJobOutput(jobOutput);
        
        Map<String, Object> report = new HashMap<>();
        report.put("analysis", analysis);
        report.put("recommendation", getAIRecommendation(analysis));
        report.put("timestamp", new Date().toString());
        
        return report;
    }
    
    /**
     * Get recommendation for AI consensus to fix the error
     */
    private String getAIRecommendation(Map<String, Object> analysis) {
        String errorType = (String) analysis.get("errorType");
        
        switch (errorType) {
            case "COMPILATION_ERROR":
                return "Fix Java compilation errors - check imports, method signatures, and class definitions";
            case "TEST_FAILURE":
                return "Fix failing tests - ensure generated code matches test expectations";
            case "RUNTIME_ERROR":
                return "Fix runtime exceptions - add null checks, proper error handling, and resource management";
            case "BUILD_FAILURE":
                return "Fix build process - check dependencies, configurations, and build scripts";
            default:
                return "Analyze the error and provide a fix";
        }
    }
}
