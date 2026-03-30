package org.example.controller;

import org.example.service.PerformanceAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Phase 2 Intelligence: Performance Analysis Controller
 * REST API for framework performance analysis and optimization recommendations
 */
@RestController
@RequestMapping("/api/intelligence/performance")
public class PerformanceAnalysisController {

    @Autowired(required = false)
    private PerformanceAnalyzer performanceAnalyzer;

    /**
     * GET /api/intelligence/performance/framework/{name}
     * Analyze specific framework's performance
     */
    @GetMapping("/framework/{name}")
    public ResponseEntity<?> analyzeFramework(@PathVariable String name) {
        if (performanceAnalyzer == null) {
            return ResponseEntity.ok(Map.of("message", "PerformanceAnalyzer not available"));
        }
        return ResponseEntity.ok(performanceAnalyzer.analyzeFramework(name));
    }

    /**
     * GET /api/intelligence/performance/recommendations
     * Get optimization recommendations
     */
    @GetMapping("/recommendations")
    public ResponseEntity<?> getRecommendations() {
        if (performanceAnalyzer == null) {
            return ResponseEntity.ok(Map.of("message", "PerformanceAnalyzer not available"));
        }
        return ResponseEntity.ok(Map.of(
            "recommendations", performanceAnalyzer.getOptimizationRecommendations(),
            "count", performanceAnalyzer.getOptimizationRecommendations().size()
        ));
    }

    /**
     * GET /api/intelligence/performance/recommendations/critical
     * Get only critical recommendations
     */
    @GetMapping("/recommendations/critical")
    public ResponseEntity<?> getCriticalRecommendations() {
        if (performanceAnalyzer == null) {
            return ResponseEntity.ok(Map.of("message", "PerformanceAnalyzer not available"));
        }
        var critical = performanceAnalyzer.getOptimizationRecommendations().stream()
                .filter(r -> "CRITICAL".equals(r.get("severity")))
                .toList();
        return ResponseEntity.ok(Map.of(
            "criticalRecommendations", critical,
            "count", critical.size()
        ));
    }

    /**
     * GET /api/intelligence/performance/comparison
     * Compare all frameworks performance
     */
    @GetMapping("/comparison")
    public ResponseEntity<?> getComparison() {
        if (performanceAnalyzer == null) {
            return ResponseEntity.ok(Map.of("message", "PerformanceAnalyzer not available"));
        }
        return ResponseEntity.ok(Map.of(
            "frameworks", performanceAnalyzer.getComparativeAnalysis(),
            "count", performanceAnalyzer.getComparativeAnalysis().size()
        ));
    }

    /**
     * GET /api/intelligence/performance/insights
     * Get summary analytics insights
     */
    @GetMapping("/insights")
    public ResponseEntity<?> getInsights() {
        if (performanceAnalyzer == null) {
            return ResponseEntity.ok(Map.of("message", "PerformanceAnalyzer not available"));
        }
        return ResponseEntity.ok(performanceAnalyzer.getInsightsSummary());
    }

    /**
     * POST /api/intelligence/performance/record-execution
     * Record execution for analysis
     */
    @PostMapping("/record-execution")
    public ResponseEntity<?> recordExecution(@RequestBody Map<String, Object> request) {
        if (performanceAnalyzer == null) {
            return ResponseEntity.ok(Map.of("message", "PerformanceAnalyzer not available"));
        }

        String framework = (String) request.get("framework");
        boolean success = (boolean) request.get("success");
        double executionTime = ((Number) request.get("executionTime")).doubleValue();
        double codeQuality = ((Number) request.get("codeQuality")).doubleValue();
        double complexity = ((Number) request.get("complexity")).doubleValue();
        String errorMessage = (String) request.getOrDefault("errorMessage", "");

        performanceAnalyzer.recordExecution(framework, success, executionTime, codeQuality, complexity, errorMessage);

        return ResponseEntity.ok(Map.of(
            "status", "recorded",
            "framework", framework,
            "message", "Execution recorded for analysis"
        ));
    }

    /**
     * GET /api/intelligence/performance/best-framework
     * Get top-performing framework
     */
    @GetMapping("/best-framework")
    public ResponseEntity<?> getBestFramework() {
        if (performanceAnalyzer == null) {
            return ResponseEntity.ok(Map.of("message", "PerformanceAnalyzer not available"));
        }
        var comparison = performanceAnalyzer.getComparativeAnalysis();
        if (comparison.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "No performance data available"));
        }
        return ResponseEntity.ok(Map.of(
            "bestFramework", comparison.get(0).get("framework"),
            "successRate", comparison.get(0).get("successRate"),
            "speed", comparison.get(0).get("speed")
        ));
    }

    /**
     * GET /api/intelligence/performance/needs-improvement
     * Get frameworks that need optimization
     */
    @GetMapping("/needs-improvement")
    public ResponseEntity<?> getNeedsImprovement() {
        if (performanceAnalyzer == null) {
            return ResponseEntity.ok(Map.of("message", "PerformanceAnalyzer not available"));
        }
        var recommendations = performanceAnalyzer.getOptimizationRecommendations();
        var frameworksNeedingWork = recommendations.stream()
                .map(r -> r.get("framework"))
                .distinct()
                .toList();
        return ResponseEntity.ok(Map.of(
            "frameworksNeedingImprovement", frameworksNeedingWork,
            "totalIssues", recommendations.size()
        ));
    }
}
