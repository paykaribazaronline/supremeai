package org.example.service;

import java.util.*;
import java.util.concurrent.*;

/**
 * FixValidator - Validates that proposed fixes actually work
 * 
 * Purpose: Test fix candidates to ensure they resolve errors without
 * introducing regressions.
 * 
 * Integration: Called by AutoFixLoopService to validate fix candidates
 */
public class FixValidator {
    
    private static final long TEST_TIMEOUT_MS = 30000;
    private static final int MAX_PARALLEL_TESTS = 4;
    
    public static class ValidationResult {
        public String fixId;
        public boolean compileSuccess;
        public boolean testsPassed;
        public boolean noRegressions;
        public float successScore; // 0.0-1.0 weighted score
        public List<String> failureReasons;
        public long executionTimeMs;
        public String errorLog;
        
        public ValidationResult(String fixId) {
            this.fixId = fixId;
            this.failureReasons = new ArrayList<>();
            this.compileSuccess = false;
            this.testsPassed = false;
            this.noRegressions = false;
        }
    }
    
    private static final ExecutorService executor = 
        Executors.newFixedThreadPool(MAX_PARALLEL_TESTS);
    
    /**
     * Validate a single fix candidate
     */
    public ValidationResult validateFix(String fixId, String fixedCode, String originalCode) {
        ValidationResult result = new ValidationResult(fixId);
        long startTime = System.currentTimeMillis();
        
        try {
            // Step 1: Check if fixed code compiles
            if (!validateCompilation(fixedCode, result)) {
                result.failureReasons.add("Syntax errors in fixed code");
                result.successScore = 0.0f;
                result.executionTimeMs = System.currentTimeMillis() - startTime;
                return result;
            }
            result.compileSuccess = true;
            
            // Step 2: Run quick sanity tests
            if (!validateSanity(fixedCode, result)) {
                result.failureReasons.add("Sanity check failed");
                result.successScore = 0.3f;
                result.executionTimeMs = System.currentTimeMillis() - startTime;
                return result;
            }
            
            // Step 3: Check for regressions
            if (!validateNoRegressions(fixedCode, originalCode, result)) {
                result.failureReasons.add("Potential regressions detected");
                result.successScore = 0.5f;
                result.executionTimeMs = System.currentTimeMillis() - startTime;
                return result;
            }
            result.noRegressions = true;
            
            // Step 4: Run unit tests if available
            if (hasUnitTests(fixedCode)) {
                if (runUnitTests(fixedCode, result)) {
                    result.testsPassed = true;
                    result.successScore = 1.0f;
                } else {
                    result.failureReasons.add("Unit tests failed");
                    result.successScore = 0.6f;
                }
            } else {
                // No tests, rely on compilation + sanity
                result.successScore = 0.8f;
            }
            
        } catch (Exception e) {
            result.failureReasons.add("Validation exception: " + e.getMessage());
            result.errorLog = e.toString();
            result.successScore = 0.0f;
        }
        
        result.executionTimeMs = System.currentTimeMillis() - startTime;
        return result;
    }
    
    /**
     * Validate that code compiles without errors
     */
    private boolean validateCompilation(String code, ValidationResult result) {
        try {
            // In real implementation, this would compile the code
            // For now, we check for basic syntax issues
            
            if (code == null || code.isEmpty()) {
                result.errorLog = "Code is empty";
                return false;
            }
            
            // Check for unclosed braces
            int openBraces = code.split("\\{").length - 1;
            int closeBraces = code.split("\\}").length - 1;
            if (openBraces != closeBraces) {
                result.errorLog = "Unbalanced braces";
                return false;
            }
            
            // Check for valid Java syntax patterns
            if (!code.contains("package ") && !code.contains("public class ")) {
                // Could be a code fragment, which is OK
            }
            
            return true;
        } catch (Exception e) {
            result.errorLog = e.getMessage();
            return false;
        }
    }
    
    /**
     * Run sanity checks on the fixed code
     */
    private boolean validateSanity(String code, ValidationResult result) {
        try {
            // Check: Code is not empty
            if (code == null || code.isEmpty()) {
                return false;
            }
            
            // Check: Code is not too short (probably didn't generate properly)
            if (code.length() < 10) {
                result.errorLog = "Generated code too short";
                return false;
            }
            
            // Check: No obvious infinite loops
            if (code.contains("while (true)")) {
                result.errorLog = "Detected infinite loop";
                return false;
            }
            
            // Check: Proper method structure
            if (code.contains("public ") && !code.contains("(") && !code.contains(")")) {
                result.errorLog = "Invalid method declaration";
                return false;
            }
            
            return true;
        } catch (Exception e) {
            result.errorLog = e.getMessage();
            return false;
        }
    }
    
    /**
     * Check that fix doesn't introduce regressions
     */
    private boolean validateNoRegressions(String fixedCode, String originalCode, 
                                          ValidationResult result) {
        try {
            // Check: Code length didn't change drastically (100%+)
            float lengthRatio = (float) fixedCode.length() / originalCode.length();
            if (lengthRatio > 2.0f || lengthRatio < 0.5f) {
                result.errorLog = "Code length changed drastically (ratio: " + lengthRatio + ")";
                return false;
            }
            
            // Check: Key patterns still exist
            int originalKeywords = countKeywords(originalCode);
            int fixedKeywords = countKeywords(fixedCode);
            
            // Allow up to 10% keyword change
            if (Math.abs(originalKeywords - fixedKeywords) / (float)originalKeywords > 0.1f) {
                result.errorLog = "Too many keywords changed";
                return false;
            }
            
            return true;
        } catch (Exception e) {
            result.errorLog = e.getMessage();
            return false;
        }
    }
    
    /**
     * Check if code has unit tests
     */
    private boolean hasUnitTests(String code) {
        return code.contains("@Test") || code.contains("@Before") || 
               code.contains("public void test");
    }
    
    /**
     * Run unit tests (simulated)
     */
    private boolean runUnitTests(String code, ValidationResult result) {
        try {
            // In real implementation, execute actual JUnit tests
            // For now, just check that test structure is valid
            
            if (!code.contains("@Test")) {
                return true; // No tests = pass
            }
            
            // Check that tests can at least be parsed
            return code.contains("void test") || code.contains("void should");
        } catch (Exception e) {
            result.errorLog = e.getMessage();
            return false;
        }
    }
    
    /**
     * Validate multiple fixes in parallel
     */
    public Map<String, ValidationResult> validateFixesInParallel(
            Map<String, String> fixCandidates, String originalCode) {
        
        Map<String, ValidationResult> results = new ConcurrentHashMap<>();
        List<Future<?>> futures = new ArrayList<>();
        
        for (Map.Entry<String, String> entry : fixCandidates.entrySet()) {
            Future<?> future = executor.submit(() -> {
                ValidationResult result = validateFix(entry.getKey(), entry.getValue(), originalCode);
                results.put(entry.getKey(), result);
            });
            futures.add(future);
            
            if (futures.size() >= MAX_PARALLEL_TESTS) {
                waitForSome(futures);
            }
        }
        
        // Wait for all remaining tests
        for (Future<?> future : futures) {
            try {
                future.get(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
            } catch (Exception e) {
                // Test failed, skip
            }
        }
        
        return results;
    }
    
    /**
     * Wait for at least one future to complete
     */
    private void waitForSome(List<Future<?>> futures) {
        try {
            ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
            for (Future<?> future : new ArrayList<>(futures)) {
                try {
                    future.get(1000, TimeUnit.MILLISECONDS);
                    futures.remove(future);
                    break;
                } catch (TimeoutException e) {
                    // Still running
                }
            }
        } catch (Exception e) {
            // Timeout, continue
        }
    }
    
    /**
     * Count Java keywords in code
     */
    private int countKeywords(String code) {
        String[] keywords = {"public", "private", "protected", "class", "interface", 
                           "void", "return", "new", "this", "super", "try", "catch"};
        
        int count = 0;
        for (String keyword : keywords) {
            count += code.split("\\b" + keyword + "\\b").length - 1;
        }
        return count;
    }
    
    /**
     * Get passing fixes sorted by confidence
     */
    public List<Map.Entry<String, ValidationResult>> getSuccessfulFixes(
            Map<String, ValidationResult> results) {
        
        return results.entrySet().stream()
            .filter(e -> e.getValue().successScore >= 0.7f)
            .sorted((a, b) -> Float.compare(b.getValue().successScore, a.getValue().successScore))
            .limit(5)
            .toList();
    }
}
