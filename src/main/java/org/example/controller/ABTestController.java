package org.example.controller;

import org.example.model.FixVariant;
import org.example.service.ABTestingAgent;
import org.example.service.FixVariantComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phase 6 Week 7-8: A/B Testing Controller
 * REST API for variant testing and comparison
 * 
 * Endpoints:
 * - POST /api/v1/ab-test/run - Run A/B test on variants
 * - POST /api/v1/ab-test/compare - Compare two variants
 * - GET /api/v1/ab-test/analysis - Analyze variants
 * - POST /api/v1/ab-test/recommend - Get recommendation
 */
@RestController
@RequestMapping("/api/v1/ab-test")
@CrossOrigin(origins = "*")
public class ABTestController {

    @Autowired(required = false)
    private ABTestingAgent abTestingAgent;

    @Autowired
    private FixVariantComparator comparator;

    private static final Map<String, ABTestingAgent.ABTestResult> testResults = new ConcurrentHashMap<>();

    // ==================== A/B Testing Endpoints ====================

    /**
     * POST /api/v1/ab-test/run
     * Run A/B test on fix variants
     */
    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runABTest(
            @RequestParam String fixId,
            @RequestBody List<FixVariant> variants) {
        
        if (abTestingAgent == null) {
            return ResponseEntity.status(501).body(errorResponse("ABTestingAgent not available"));
        }

        try {
            ABTestingAgent.ABTestResult result = abTestingAgent.runABTest(fixId, variants);
            testResults.put(result.testId, result);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("testId", result.testId);
            response.put("testStatus", result.testStatus);
            response.put("winnerVariantId", result.winnerVariantId);
            response.put("winnerScore", result.winnerScore);
            response.put("variantsTestedCount", result.variantsTestedCount);
            response.put("testDurationMs", result.testDurationMs);
            response.put("timestamp", System.currentTimeMillis());

            if (result.performanceAnalysis != null) {
                response.put("performanceAnalysis", result.performanceAnalysis.toMap());
            }

            if (result.recommendation != null) {
                response.put("recommendation", result.recommendation.toMap());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(errorResponse(e.getMessage()));
        }
    }

    /**
     * GET /api/v1/ab-test/result/{testId}
     * Get A/B test result
     */
    @GetMapping("/result/{testId}")
    public ResponseEntity<Map<String, Object>> getTestResult(
            @PathVariable String testId) {
        
        ABTestingAgent.ABTestResult result = testResults.get(testId);
        
        if (result == null) {
            return ResponseEntity.status(404).body(errorResponse("Test result not found: " + testId));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("testId", testId);
        response.put("testStatus", result.testStatus);
        response.put("winnerVariantId", result.winnerVariantId);
        response.put("winnerScore", result.winnerScore);
        response.put("variantsTestedCount", result.variantsTestedCount);
        response.put("testDurationMs", result.testDurationMs);
        response.put("timestamp", System.currentTimeMillis());

        if (result.performanceAnalysis != null) {
            response.put("performanceAnalysis", result.performanceAnalysis.toMap());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/ab-test/compare
     * Compare two variants head-to-head
     */
    @PostMapping("/compare")
    public ResponseEntity<Map<String, Object>> compareVariants(
            @RequestBody Map<String, Object> request) {
        
        if (comparator == null) {
            return ResponseEntity.status(501).body(errorResponse("Comparator not available"));
        }

        try {
            FixVariant variant1 = parseVariant((Map<String, Object>) request.get("variant1"));
            FixVariant variant2 = parseVariant((Map<String, Object>) request.get("variant2"));

            FixVariantComparator.ComparisonResult result = comparator.compare(variant1, variant2);

            Map<String, Object> response = new HashMap<>();
            response.put("variant1Id", result.variant1Id);
            response.put("variant2Id", result.variant2Id);
            response.put("variant1Score", result.variant1Score);
            response.put("variant2Score", result.variant2Score);
            response.put("scoreDifference", result.scoreDifference);
            response.put("winner", result.winner);
            response.put("confidence", result.confidence);
            response.put("advantages", result.advantages);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(errorResponse("Invalid variant format: " + e.getMessage()));
        }
    }

    /**
     * POST /api/v1/ab-test/analyze
     * Analyze multiple variants
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeVariants(
            @RequestBody List<Map<String, Object>> variantMaps) {
        
        if (comparator == null) {
            return ResponseEntity.status(501).body(errorResponse("Comparator not available"));
        }

        try {
            List<FixVariant> variants = variantMaps.stream()
                .map(this::parseVariant)
                .toList();

            FixVariantComparator.VariantAnalysis analysis = comparator.analyze(variants);

            Map<String, Object> response = new HashMap<>();
            response.put("analysisId", UUID.randomUUID().toString());
            response.put("totalVariants", analysis.totalVariants);
            response.put("avgScore", analysis.avgScore);
            response.put("avgSuccessRate", analysis.avgSuccessRate);
            response.put("avgExecutionTime", analysis.avgExecutionTime);
            response.put("avgRegressionRate", analysis.avgRegressionRate);
            response.put("stdDev", analysis.stdDev);
            response.put("bestVariantId", analysis.bestVariant != null ? analysis.bestVariant.getVariantId() : null);
            response.put("worstVariantId", analysis.worstVariant != null ? analysis.worstVariant.getVariantId() : null);
            response.put("strategyDistribution", analysis.strategyDistribution);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(errorResponse("Invalid variants: " + e.getMessage()));
        }
    }

    /**
     * POST /api/v1/ab-test/recommend
     * Get recommendation for best variant
     */
    @PostMapping("/recommend")
    public ResponseEntity<Map<String, Object>> getRecommendation(
            @RequestBody List<Map<String, Object>> variantMaps) {
        
        if (comparator == null) {
            return ResponseEntity.status(501).body(errorResponse("Comparator not available"));
        }

        try {
            List<FixVariant> variants = variantMaps.stream()
                .map(this::parseVariant)
                .toList();

            FixVariantComparator.Recommendation recommendation = comparator.getRecommendation(variants);

            Map<String, Object> response = new HashMap<>();
            response.put("recommendation", recommendation.toMap());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(errorResponse("Invalid variants: " + e.getMessage()));
        }
    }

    /**
     * GET /api/v1/ab-test/history
     * Get A/B test history
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getTestHistory(
            @RequestParam(defaultValue = "50") int limit) {
        
        List<Map<String, Object>> history = testResults.values().stream()
            .sorted((a, b) -> Long.compare(b.testEndTime, a.testEndTime))
            .limit(limit)
            .map(result -> {
                Map<String, Object> item = new HashMap<>();
                item.put("testId", result.testId);
                item.put("fixId", result.fixId);
                item.put("status", result.testStatus);
                item.put("winner", result.winnerVariantId);
                item.put("score", result.winnerScore);
                item.put("variantsCount", result.variantsTestedCount);
                item.put("duration", result.testDurationMs);
                return item;
            })
            .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("totalResults", history.size());
        response.put("history", history);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/ab-test/stats
     * Get A/B testing statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("totalTests", testResults.size());
        response.put("successfulTests", testResults.values().stream()
            .filter(r -> "COMPLETE".equals(r.testStatus))
            .count());
        response.put("failedTests", testResults.values().stream()
            .filter(r -> "FAILED".equals(r.testStatus))
            .count());

        double avgWinnerScore = testResults.values().stream()
            .mapToDouble(r -> r.winnerScore)
            .average()
            .orElse(0);
        response.put("avgWinnerScore", avgWinnerScore);

        long totalVariantsTested = testResults.values().stream()
            .mapToLong(r -> r.variantsTestedCount)
            .sum();
        response.put("totalVariantsTested", totalVariantsTested);

        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    // ==================== Helper Methods ====================

    /**
     * Parse variant from map
     */
    private FixVariant parseVariant(Map<String, Object> map) {
        FixVariant variant = new FixVariant();

        if (map.containsKey("variantId")) {
            variant.setVariantId((String) map.get("variantId"));
        }
        if (map.containsKey("fixId")) {
            variant.setFixId((String) map.get("fixId"));
        }
        if (map.containsKey("strategy")) {
            variant.setStrategy((String) map.get("strategy"));
        }
        if (map.containsKey("implementation")) {
            variant.setImplementation((String) map.get("implementation"));
        }
        if (map.containsKey("executionTime")) {
            variant.setExecutionTime(((Number) map.get("executionTime")).floatValue());
        }
        if (map.containsKey("successRate")) {
            variant.setSuccessRate(((Number) map.get("successRate")).floatValue());
        }
        if (map.containsKey("regressionDetected")) {
            variant.setRegressionDetected(((Number) map.get("regressionDetected")).floatValue());
        }
        if (map.containsKey("linesChanged")) {
            variant.setLinesChanged(((Number) map.get("linesChanged")).intValue());
        }

        return variant;
    }

    /**
     * Generate error response
     */
    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}
