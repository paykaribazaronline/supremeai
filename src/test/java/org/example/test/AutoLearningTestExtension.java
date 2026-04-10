package org.example.test;

import org.example.service.SystemLearningService;
import org.example.service.AIErrorSolvingService;
import org.junit.jupiter.api.extension.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.*;

/**
 * Auto-Learning Test Extension for JUnit5
 * 
 * Automatically captures test failures and:
 * 1. Records them in SystemLearningService
 * 2. Generates fixes via AIErrorSolvingService
 * 3. Learns patterns from repeated failures
 */
public class AutoLearningTestExtension implements TestWatcher {

    private static final Logger logger = LoggerFactory.getLogger(AutoLearningTestExtension.class);

    private SystemLearningService learningService;
    private AIErrorSolvingService errorSolvingService;

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        String testMethod = context.getDisplayName();
        String testClass = context.getTestClass().map(Class::getSimpleName).orElse("Unknown");
        
        logger.error("\n❌ TEST FAILED: {}.{}", testClass, testMethod);
        logger.error("   Cause: {}", cause.getClass().getSimpleName());
        logger.error("   Message: {}", cause.getMessage());

        // Try to get Spring services from context
        try {
            if (learningService == null) {
                learningService = SpringExtension.getApplicationContext(context)
                    .getBean(SystemLearningService.class);
                errorSolvingService = SpringExtension.getApplicationContext(context)
                    .getBean(AIErrorSolvingService.class);
            }
        } catch (Exception e) {
            logger.debug("⚠️  Spring services not available: {}", e.getMessage());
        }

        // If learning services available, auto-learn from failure
        if (learningService != null && errorSolvingService != null) {
            learnFromTestFailure(testClass, testMethod, cause);
        } else {
            logger.warn("⚠️  Learning services not initialized - skipping auto-learning");
        }
    }

    /**
     * Learn from test failure: record, analyze, solve
     */
    private void learnFromTestFailure(String testClass, String testMethod, Throwable cause) {
        try {
            String category = "TEST_FAILURE";
            String problem = testClass + "::" + testMethod;
            String errorMsg = cause.getMessage() != null ? cause.getMessage() : cause.toString();
            String rootCause = extractRootCause(cause);
            
            logger.info("\n🧠 AUTO-LEARNING FROM TEST FAILURE");
            logger.info("   Category: {}", category);
            logger.info("   Problem: {}", problem);
            logger.info("   Root Cause: {}", rootCause);

            // Step 1: Record the failure in SystemLearning
            learningService.recordError(
                category,
                errorMsg,
                cause instanceof Exception ? (Exception) cause : new Exception(cause),
                null
            );
            logger.info("   ✅ Error recorded in learning memory");

            // Step 2: Ask AI to solve the error
            String solveContext = String.format(
                "Test %s::%s failed with: %s. Root cause: %s",
                testClass, testMethod, errorMsg, rootCause
            );
            
            Map<String, Object> solution = errorSolvingService.solveError(
                "system-auto-learning",
                errorMsg,
                solveContext
            );
            
            if ("success".equals(solution.get("status"))) {
                String aiSolution = (String) solution.getOrDefault("aiSolution", "No solution generated");
                Double confidenceObj = (Double) solution.getOrDefault("confidenceScore", 0.0);
                double confidence = confidenceObj != null ? confidenceObj : 0.0;
                
                logger.info("   ✅ AI generated solution (confidence: {:.1f}%)", confidence * 100);
                logger.info("   Solution: {}", aiSolution.substring(0, Math.min(200, aiSolution.length())));

                // Step 3: Learn the fix
                learningService.learnFromIncident(
                    category,
                    problem,
                    rootCause,
                    aiSolution,
                    Arrays.asList(
                        "1. Check model training data quality",
                        "2. Verify test data matches production distribution",
                        "3. Review feature engineering for anomalies",
                        "4. Validate model threshold calibration",
                        "5. Compare against baseline performance"
                    ),
                    confidence,
                    Map.of(
                        "testClass", testClass,
                        "testMethod", testMethod,
                        "errorType", cause.getClass().getSimpleName(),
                        "autoFixed", false,
                        "requiresManualReview", confidence < 0.8
                    )
                );
                logger.info("   ✅ Solution learned and recorded");
            } else {
                logger.warn("   ⚠️  AI could not generate solution: {}", solution.get("message"));
            }

            logger.info("✅ AUTO-LEARNING COMPLETE\n");

        } catch (Exception e) {
            logger.error("❌ Failed to auto-learn from test failure: {}", e.getMessage(), e);
        }
    }

    private String extractRootCause(Throwable cause) {
        if (cause == null) return "Unknown";
        
        String msg = cause.getMessage();
        if (msg != null && msg.contains("expected: <") && msg.contains("but was:")) {
            // Extract assertion details
            int expectedIdx = msg.indexOf("expected: <");
            int butWasIdx = msg.indexOf("but was:");
            if (expectedIdx >= 0 && butWasIdx >= 0) {
                String expected = msg.substring(expectedIdx + 11, msg.indexOf(">", expectedIdx));
                String actual = msg.substring(butWasIdx + 8, msg.length());
                return String.format("Assertion failed: expected %s but got %s", expected, actual);
            }
        }
        
        return cause.getClass().getSimpleName() + ": " + (msg != null ? msg : "no message");
    }
}
