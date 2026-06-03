package com.supremeai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Quality Scoring Service - Plan 18 Phase 4
 * Automated quality scoring and validation for crowdsourced API patterns.
 * Implements validation framework, automated testing, and quality gates.
 */
@Service
public class QualityScoringService {
    public QualityScoringService(ReputationService reputationService) {
        this.reputationService = reputationService;
    }


    private static final Logger logger = LoggerFactory.getLogger(QualityScoringService.class);

    // Quality thresholds
    private static final int MIN_ACCEPTABLE_SCORE = 60;
    private static final int HIGH_QUALITY_THRESHOLD = 85;
    private static final int TRUSTED_THRESHOLD = 95;

    // Auto-testing configuration
    private static final int MAX_TEST_RETRIES = 3;
    private static final long TEST_TIMEOUT_MS = 30000;

    // In-memory storage
    private final Map<String, QualityAssessment> assessmentCache = new ConcurrentHashMap<>();
    private final Map<String, AutoTestResult> testResults = new ConcurrentHashMap<>();


    /**
     * Quality assessment result
     */
    public static class QualityAssessment {
        public String apiId;
        public int overallScore;
        public String qualityLevel;
        public boolean autoApproved;
        public boolean needsManualReview;
        public List<String> checksPassed;
        public List<String> checksFailed;
        public List<String> warnings;
        public List<String> recommendations;
        public long assessedAt;

        public QualityAssessment(String apiId) {
            this.apiId = apiId;
            this.checksPassed = new ArrayList<>();
            this.checksFailed = new ArrayList<>();
            this.warnings = new ArrayList<>();
            this.recommendations = new ArrayList<>();
            this.assessedAt = System.currentTimeMillis();
        }
    }

    /**
     * Automated test result
     */
    public static class AutoTestResult {
        public String testId;
        public String apiId;
        public boolean passed;
        public int testsRun;
        public int testsPassed;
        public List<String> failures;
        public long durationMs;
        public long executedAt;

        public AutoTestResult(String apiId) {
            this.testId = UUID.randomUUID().toString().substring(0, 12);
            this.apiId = apiId;
            this.failures = new ArrayList<>();
        }
    }

    /**
     * Run full quality assessment on an API pattern submission
     */
    public QualityAssessment assessQuality(String apiId, Map<String, Object> submission) {
        QualityAssessment assessment = new QualityAssessment(apiId);

        // Run all quality checks
        checkSchemaValidity(assessment, submission);
        checkEndpointDiscovery(assessment, submission);
        checkAuthHandling(assessment, submission);
        checkErrorHandling(assessment, submission);
        checkRateLimiting(assessment, submission);
        checkSecurityMeasures(assessment, submission);
        checkDocumentation(assessment, submission);
        checkTestCoverage(assessment, submission);
        checkCodeQuality(assessment, submission);
        checkCompatibility(assessment, submission);

        // Calculate overall score
        int passed = assessment.checksPassed.size();
        int total = passed + assessment.checksFailed.size();
        assessment.overallScore = total > 0 ? (passed * 100) / total : 0;

        // Determine quality level
        if (assessment.overallScore >= TRUSTED_THRESHOLD) {
            assessment.qualityLevel = "TRUSTED";
            assessment.autoApproved = true;
        } else if (assessment.overallScore >= HIGH_QUALITY_THRESHOLD) {
            assessment.qualityLevel = "HIGH";
            assessment.autoApproved = true;
        } else if (assessment.overallScore >= MIN_ACCEPTABLE_SCORE) {
            assessment.qualityLevel = "ACCEPTABLE";
            assessment.needsManualReview = true;
        } else {
            assessment.qualityLevel = "LOW";
            assessment.needsManualReview = true;
        }

        // Cache the assessment
        assessmentCache.put(apiId, assessment);

        logger.info("Quality assessment for {}: score={}, level={}, autoApproved={}",
                apiId, assessment.overallScore, assessment.qualityLevel, assessment.autoApproved);

        return assessment;
    }

    /**
     * Run automated tests on a connector
     */
    public AutoTestResult runAutoTests(String apiId, Map<String, Object> connectorConfig) {
        AutoTestResult result = new AutoTestResult(apiId);
        long startTime = System.currentTimeMillis();

        try {
            // Test 1: Syntax validation
            boolean syntaxOk = testSyntaxValidation(connectorConfig);
            result.testsRun++;
            if (syntaxOk) result.testsPassed++;
            else result.failures.add("Syntax validation failed");

            // Test 2: Authentication flow
            boolean authOk = testAuthentication(connectorConfig);
            result.testsRun++;
            if (authOk) result.testsPassed++;
            else result.failures.add("Authentication test failed");

            // Test 3: Endpoint connectivity
            boolean endpointOk = testEndpointConnectivity(connectorConfig);
            result.testsRun++;
            if (endpointOk) result.testsPassed++;
            else result.failures.add("Endpoint connectivity test failed");

            // Test 4: Response parsing
            boolean parsingOk = testResponseParsing(connectorConfig);
            result.testsRun++;
            if (parsingOk) result.testsPassed++;
            else result.failures.add("Response parsing test failed");

            // Test 5: Error handling
            boolean errorOk = testErrorHandling(connectorConfig);
            result.testsRun++;
            if (errorOk) result.testsPassed++;
            else result.failures.add("Error handling test failed");

            // Test 6: Rate limiting compliance
            boolean rateOk = testRateLimiting(connectorConfig);
            result.testsRun++;
            if (rateOk) result.testsPassed++;
            else result.failures.add("Rate limiting test failed");

            result.passed = result.testsPassed == result.testsRun;

        } catch (Exception e) {
            logger.error("Auto-test exception for {}: {}", apiId, e.getMessage());
            result.failures.add("Exception: " + e.getMessage());
            result.passed = false;
        }

        result.durationMs = System.currentTimeMillis() - startTime;
        result.executedAt = System.currentTimeMillis();

        testResults.put(apiId, result);

        logger.info("Auto-tests for {}: {}/{} passed in {}ms",
                apiId, result.testsPassed, result.testsRun, result.durationMs);

        // Update reputation if service available
        if (reputationService != null) {
            updateReputationBasedOnTests(apiId, result.passed);
        }

        return result;
    }

    /**
     * Retry failed tests with exponential backoff
     */
    public AutoTestResult retryFailedTests(String apiId, Map<String, Object> connectorConfig) {
        AutoTestResult bestResult = null;

        for (int attempt = 1; attempt <= MAX_TEST_RETRIES; attempt++) {
            logger.info("Test retry attempt {}/{} for {}", attempt, MAX_TEST_RETRIES, apiId);

            try {
                Thread.sleep((long) (1000 * Math.pow(2, attempt - 1))); // Exponential backoff
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            bestResult = runAutoTests(apiId, connectorConfig);
            if (bestResult.passed) {
                logger.info("Tests passed on retry attempt {}", attempt);
                break;
            }
        }

        return bestResult;
    }

    /**
     * Get quality assessment for an API
     */
    public QualityAssessment getAssessment(String apiId) {
        return assessmentCache.get(apiId);
    }

    /**
     * Get test results for an API
     */
    public AutoTestResult getTestResults(String apiId) {
        return testResults.get(apiId);
    }

    /**
     * Validate that submission meets minimum requirements for review
     */
    public boolean validateSubmission(Map<String, Object> submission) {
        if (submission == null || submission.isEmpty()) return false;

        // Must have endpoints
        Object endpoints = submission.get("endpoints");
        if (!(endpoints instanceof List) || ((List<?>) endpoints).isEmpty()) return false;

        // Must have auth type
        if (!submission.containsKey("authType")) return false;

        // Must have request format
        if (!submission.containsKey("requestFormat")) return false;

        return true;
    }

    /**
     * Get all assessments below threshold (needs review)
     */
    public List<QualityAssessment> getAssessmentsNeedingReview() {
        return assessmentCache.values().stream()
                .filter(a -> !a.autoApproved)
                .sorted((a, b) -> Integer.compare(a.overallScore, b.overallScore))
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────────────────
    // Individual Quality Checks
    // ──────────────────────────────────────────────────────────────────────

    private void checkSchemaValidity(QualityAssessment assessment, Map<String, Object> submission) {
        if (submission.containsKey("schema") && submission.get("schema") != null) {
            assessment.checksPassed.add("SCHEMA_VALID");
        } else {
            assessment.checksFailed.add("SCHEMA_MISSING");
            assessment.recommendations.add("Provide a JSON schema for the API");
        }
    }

    private void checkEndpointDiscovery(QualityAssessment assessment, Map<String, Object> submission) {
        Object endpoints = submission.get("endpoints");
        if (endpoints instanceof List && ((List<?>) endpoints).size() >= 1) {
            assessment.checksPassed.add("ENDPOINTS_DISCOVERED");
        } else {
            assessment.checksFailed.add("ENDPOINTS_INCOMPLETE");
            assessment.recommendations.add("Verify all endpoints are discovered");
        }
    }

    private void checkAuthHandling(QualityAssessment assessment, Map<String, Object> submission) {
        if (submission.containsKey("authType") && submission.containsKey("authConfig")) {
            assessment.checksPassed.add("AUTH_HANDLED");
        } else {
            assessment.checksFailed.add("AUTH_MISSING");
            assessment.warnings.add("Authentication handling incomplete");
        }
    }

    private void checkErrorHandling(QualityAssessment assessment, Map<String, Object> submission) {
        if (submission.containsKey("errorHandling") && Boolean.TRUE.equals(submission.get("errorHandling"))) {
            assessment.checksPassed.add("ERROR_HANDLING");
        } else {
            assessment.checksFailed.add("ERROR_HANDLING_MISSING");
            assessment.recommendations.add("Implement proper error handling and retry logic");
        }
    }

    private void checkRateLimiting(QualityAssessment assessment, Map<String, Object> submission) {
        if (Boolean.TRUE.equals(submission.get("rateLimited"))) {
            assessment.checksPassed.add("RATE_LIMITED");
        } else {
            assessment.checksFailed.add("RATE_LIMIT_MISSING");
            assessment.warnings.add("Rate limiting not implemented");
        }
    }

    private void checkSecurityMeasures(QualityAssessment assessment, Map<String, Object> submission) {
        int securityScore = 0;
        if (Boolean.TRUE.equals(submission.get("usesEncryption"))) securityScore += 2;
        if (Boolean.TRUE.equals(submission.get("validatesInput"))) securityScore += 2;
        if (Boolean.TRUE.equals(submission.get("hasCsrfProtection"))) securityScore++;
        if (Boolean.TRUE.equals(submission.get("validatesHeaders"))) securityScore++;

        if (securityScore >= 3) {
            assessment.checksPassed.add("SECURITY_ADEQUATE");
        } else {
            assessment.checksFailed.add("SECURITY_WEAK");
            assessment.warnings.add("Review security measures");
        }
    }

    private void checkDocumentation(QualityAssessment assessment, Map<String, Object> submission) {
        if (submission.containsKey("description") && ((String) submission.getOrDefault("description", "")).length() >= 10) {
            assessment.checksPassed.add("HAS_DESCRIPTION");
        } else {
            assessment.checksFailed.add("DESCRIPTION_MISSING");
            assessment.recommendations.add("Add a detailed description");
        }

        if (Boolean.TRUE.equals(submission.get("hasApiDocs"))) {
            assessment.checksPassed.add("API_DOCS");
        }
    }

    private void checkTestCoverage(QualityAssessment assessment, Map<String, Object> submission) {
        Object testCount = submission.get("testCount");
        if (testCount instanceof Integer && (Integer) testCount >= 5) {
            assessment.checksPassed.add("TEST_COVERAGE");
        } else {
            assessment.checksFailed.add("TESTS_INSUFFICIENT");
            assessment.recommendations.add("Add unit tests (minimum 5)");
        }
    }

    private void checkCodeQuality(QualityAssessment assessment, Map<String, Object> submission) {
        if (submission.containsKey("codeQualityScore")) {
            double qualityScore = ((Number) submission.get("codeQualityScore")).doubleValue();
            if (qualityScore >= 70) {
                assessment.checksPassed.add("CODE_QUALITY_ACCEPTABLE");
            } else {
                assessment.checksFailed.add("CODE_QUALITY_LOW");
            }
        }
    }

    private void checkCompatibility(QualityAssessment assessment, Map<String, Object> submission) {
        // Check platform compatibility
        Object platforms = submission.get("platforms");
        if (platforms instanceof List && ((List<?>) platforms).size() >= 2) {
            assessment.checksPassed.add("MULTI_PLATFORM");
        }
    }

    // Stub methods for automated test execution
    private boolean testSyntaxValidation(Map<String, Object> config) {
        if (config == null || config.isEmpty()) {
            logger.warn("[QUALITY] Syntax validation failed: config is null or empty");
            return false;
        }
        // Verify connector has a non-empty identifier
        Object id = config.get("apiId");
        if (id == null || id.toString().isBlank()) {
            logger.warn("[QUALITY] Syntax validation failed: apiId missing or blank");
            return false;
        }
        // Verify at least one defined section exists
        boolean hasAnySection = config.containsKey("endpoints")
                || config.containsKey("connectorConfig")
                || config.containsKey("requestFormat")
                || config.containsKey("schema");
        if (!hasAnySection) {
            logger.warn("[QUALITY] Syntax validation failed: no known config section found");
            return false;
        }
        return true;
    }

    private boolean testAuthentication(Map<String, Object> config) {
        if (config == null || config.isEmpty()) return false;
        String authType = (String) config.getOrDefault("authType", "");
        Object authConfig = config.get("authConfig");
        if (authType.isBlank()) {
            logger.warn("[QUALITY] Authentication test failed: authType not specified");
            return false;
        }
        if (authConfig == null || (!(authConfig instanceof Map<?,?>)
                && !(authConfig instanceof String))) {
            logger.warn("[QUALITY] Authentication test failed: authConfig missing or wrong type");
            return false;
        }
        return true;
    }

    private boolean testEndpointConnectivity(Map<String, Object> config) {
        if (config == null) return false;
        Object endpoints = config.get("endpoints");
        if (!(endpoints instanceof List<?>) || ((List<?>) endpoints).isEmpty()) {
            logger.warn("[QUALITY] Endpoint connectivity test failed: endpoints missing or empty list");
            return false;
        }
        return true;
    }

    private boolean testResponseParsing(Map<String, Object> config) {
        if (config == null) return false;
        String responseFormat = (String) config.getOrDefault("responseFormat",
                config.get("responseType") != null ? config.get("responseType").toString() : "");
        if (responseFormat.isBlank()) {
            logger.warn("[QUALITY] Response parsing test failed: responseFormat not specified");
            return false;
        }
        return true;
    }

    private boolean testErrorHandling(Map<String, Object> config) {
        if (config == null) return false;
        boolean hasErrorFlag = Boolean.TRUE.equals(config.get("errorHandling"));
        boolean hasRetryPolicy = config.containsKey("retryPolicy")
                && config.get("retryPolicy") != null;
        if (!hasErrorFlag && !hasRetryPolicy) {
            logger.warn("[QUALITY] Error handling test failed: "
                    + "neither errorHandling=true nor retryPolicy configured");
            return false;
        }
        return true;
    }

    private boolean testRateLimiting(Map<String, Object> config) {
        if (config == null) return false;
        boolean explicitRateLimit = Boolean.TRUE.equals(config.get("rateLimited"));
        Object limitMs = config.get("rateLimitMs");
        boolean hasRateLimitMs = limitMs instanceof Number
                && ((Number) limitMs).intValue() > 0;
        if (!explicitRateLimit && !hasRateLimitMs) {
            logger.warn("[QUALITY] Rate limiting test failed: "
                    + "rateLimited not true and rateLimitMs not set");
            return false;
        }
        return true;
    }

    private void updateReputationBasedOnTests(String apiId, boolean passed) {
        // Placeholder - in production would update contributor reputation
        logger.debug("Reputation update for {}: tests {}", apiId, passed ? "PASSED" : "FAILED");
    }
}