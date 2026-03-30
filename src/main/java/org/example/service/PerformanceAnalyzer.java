package org.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Phase 2: Performance Analysis and Learning Service
 * Analyzes execution patterns, learns from AI outputs, and suggests optimizations
 */
@Service
public class PerformanceAnalyzer {

    @Autowired(required = false)
    private MetricsService metricsService;

    @Autowired(required = false)
    private AIRankingService aiRankingService;

    private static class ExecutionPattern {
        public String frameWorkName;
        public int totalExecutions = 0;
        public int successfulExecutions = 0;
        public double avgExecutionTime = 0;
        public double avgCodeQuality = 0;
        public double avgComplexity = 0;
        public List<String> commonErrors = Collections.synchronizedList(new ArrayList<>());
        public long lastExecuted = System.currentTimeMillis();
    }

    private static class PerformanceTrend {
        public String metricName;
        public List<Double> values = new ArrayList<>();
        public long startTime = System.currentTimeMillis();
        public String trend = "STABLE";
        public double trendValue = 0;
    }

    private final Map<String, ExecutionPattern> frameworkPatterns = new ConcurrentHashMap<>();
    private final Map<String, PerformanceTrend> trends = new ConcurrentHashMap<>();
    private final AtomicLong totalAnalyzedExecutions = new AtomicLong(0);

    /**
     * Record execution for analysis
     */
    public void recordExecution(String framework, boolean success, double executionTime, 
                               double codeQuality, double complexity, String errorMessage) {
        ExecutionPattern pattern = frameworkPatterns.computeIfAbsent(framework, k -> {
            ExecutionPattern p = new ExecutionPattern();
            p.frameWorkName = framework;
            return p;
        });

        pattern.totalExecutions++;
        if (success) {
            pattern.successfulExecutions++;
            pattern.avgExecutionTime = (pattern.avgExecutionTime + executionTime) / 2;
            pattern.avgCodeQuality = (pattern.avgCodeQuality + codeQuality) / 2;
        } else {
            if (!errorMessage.isEmpty()) {
                pattern.commonErrors.add(errorMessage);
                if (pattern.commonErrors.size() > 100) {
                    pattern.commonErrors.remove(0);
                }
            }
        }
        pattern.avgComplexity = (pattern.avgComplexity + complexity) / 2;
        pattern.lastExecuted = System.currentTimeMillis();

        totalAnalyzedExecutions.incrementAndGet();
        updateTrends(framework, success, executionTime, codeQuality);
    }

    /**
     * Update performance trends
     */
    private void updateTrends(String framework, boolean success, double executionTime, double quality) {
        // Success rate trend
        String successKey = framework + ".successRate";
        PerformanceTrend successTrend = trends.computeIfAbsent(successKey, k -> new PerformanceTrend());
        successTrend.values.add(success ? 1.0 : 0.0);
        analyzeTrend(successTrend);

        // Execution time trend
        String timeKey = framework + ".executionTime";
        PerformanceTrend timeTrend = trends.computeIfAbsent(timeKey, k -> new PerformanceTrend());
        timeTrend.values.add(executionTime);
        analyzeTrend(timeTrend);

        // Quality trend
        String qualityKey = framework + ".quality";
        PerformanceTrend qualityTrend = trends.computeIfAbsent(qualityKey, k -> new PerformanceTrend());
        qualityTrend.values.add(quality);
        analyzeTrend(qualityTrend);

        // Limit to last 100 values
        if (successTrend.values.size() > 100) successTrend.values.remove(0);
        if (timeTrend.values.size() > 100) timeTrend.values.remove(0);
        if (qualityTrend.values.size() > 100) qualityTrend.values.remove(0);
    }

    /**
     * Analyze trend direction (upward, downward, stable)
     */
    private void analyzeTrend(PerformanceTrend trend) {
        if (trend.values.size() < 5) {
            trend.trend = "INSUFFICIENT_DATA";
            return;
        }

        List<Double> recent = trend.values.subList(Math.max(0, trend.values.size() - 10), trend.values.size());
        double avgRecent = recent.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double avgOlder = trend.values.stream()
                .limit(Math.max(1, trend.values.size() - 10))
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);

        trend.trendValue = avgRecent - avgOlder;

        if (Math.abs(trend.trendValue) < 0.05) {
            trend.trend = "STABLE";
        } else if (trend.trendValue > 0) {
            trend.trend = "IMPROVING";
        } else {
            trend.trend = "DEGRADING";
        }
    }

    /**
     * Get framework analysis
     */
    public Map<String, Object> analyzeFramework(String framework) {
        ExecutionPattern pattern = frameworkPatterns.get(framework);
        if (pattern == null) {
            return Map.of("framework", framework, "status", "NO_DATA");
        }

        Map<String, Object> analysis = new HashMap<>();
        analysis.put("framework", framework);
        analysis.put("totalExecutions", pattern.totalExecutions);
        analysis.put("successfulExecutions", pattern.successfulExecutions);
        analysis.put("successRate", pattern.totalExecutions == 0 ? 0 : 
            ((double) pattern.successfulExecutions / pattern.totalExecutions) * 100);
        analysis.put("avgExecutionTime", String.format("%.2f ms", pattern.avgExecutionTime));
        analysis.put("avgCodeQuality", String.format("%.2f/10", pattern.avgCodeQuality));
        analysis.put("avgComplexity", String.format("%.2f", pattern.avgComplexity));
        analysis.put("commonErrors", pattern.commonErrors.stream().limit(5).toList());
        analysis.put("lastExecuted", new Date(pattern.lastExecuted));

        // Add trend information
        String successKey = framework + ".successRate";
        PerformanceTrend successTrend = trends.get(successKey);
        if (successTrend != null) {
            analysis.put("successTrend", successTrend.trend);
            analysis.put("successTrendValue", String.format("%.2f%%", successTrend.trendValue * 100));
        }

        return analysis;
    }

    /**
     * Get recommendations for optimization
     */
    public List<Map<String, Object>> getOptimizationRecommendations() {
        List<Map<String, Object>> recommendations = new ArrayList<>();

        for (ExecutionPattern pattern : frameworkPatterns.values()) {
            double successRate = pattern.totalExecutions == 0 ? 0 : 
                ((double) pattern.successfulExecutions / pattern.totalExecutions);

            // Recommendation 1: Low success rate
            if (successRate < 0.7) {
                recommendations.add(Map.of(
                    "framework", pattern.frameWorkName,
                    "issue", "Low Success Rate",
                    "severity", "HIGH",
                    "description", String.format("Success rate is %.1f%%. Consider debugging common errors.", successRate * 100),
                    "affectedRate", String.format("%.1f%%", (1 - successRate) * 100)
                ));
            }

            // Recommendation 2: High execution time
            if (pattern.avgExecutionTime > 1000) { // > 1 second
                recommendations.add(Map.of(
                    "framework", pattern.frameWorkName,
                    "issue", "Slow Execution",
                    "severity", "MEDIUM",
                    "description", String.format("Avg execution time is %.0f ms. Optimize for better performance.", pattern.avgExecutionTime),
                    "currentTime", String.format("%.0f ms", pattern.avgExecutionTime)
                ));
            }

            // Recommendation 3: Low code quality
            if (pattern.avgCodeQuality < 6) {
                recommendations.add(Map.of(
                    "framework", pattern.frameWorkName,
                    "issue", "Code Quality Issues",
                    "severity", "MEDIUM",
                    "description", String.format("Avg code quality is %.1f/10. Implement quality checks.", pattern.avgCodeQuality),
                    "currentQuality", String.format("%.1f/10", pattern.avgCodeQuality)
                ));
            }

            // Recommendation 4: High complexity
            if (pattern.avgComplexity > 7) {
                recommendations.add(Map.of(
                    "framework", pattern.frameWorkName,
                    "issue", "High Complexity",
                    "severity", "LOW",
                    "description", String.format("Avg complexity is %.1f. Consider simplification strategies.", pattern.avgComplexity),
                    "currentComplexity", String.format("%.1f", pattern.avgComplexity)
                ));
            }

            // Recommendation 5: Degrading trend
            String successKey = pattern.frameWorkName + ".successRate";
            PerformanceTrend trend = trends.get(successKey);
            if (trend != null && "DEGRADING".equals(trend.trend)) {
                recommendations.add(Map.of(
                    "framework", pattern.frameWorkName,
                    "issue", "Performance Degradation",
                    "severity", "CRITICAL",
                    "description", "Success rate is degrading. Investigate recent changes.",
                    "trendDirection", trend.trend
                ));
            }
        }

        return recommendations;
    }

    /**
     * Get comparative analysis of all frameworks
     */
    public List<Map<String, Object>> getComparativeAnalysis() {
        return frameworkPatterns.values().stream()
                .map(pattern -> {
                    Map<String, Object> comp = new HashMap<>();
                    comp.put("framework", pattern.frameWorkName);
                    comp.put("executions", pattern.totalExecutions);
                    comp.put("successRate", pattern.totalExecutions == 0 ? 0 : 
                        ((double) pattern.successfulExecutions / pattern.totalExecutions) * 100);
                    comp.put("speed", String.format("%.0f ms", pattern.avgExecutionTime));
                    comp.put("quality", String.format("%.1f/10", pattern.avgCodeQuality));
                    comp.put("complexity", String.format("%.1f", pattern.avgComplexity));
                    return comp;
                })
                .sorted((a, b) -> Double.compare((double) b.get("successRate"), (double) a.get("successRate")))
                .toList();
    }

    /**
     * Get summary insights
     */
    public Map<String, Object> getInsightsSummary() {
        Map<String, Object> summary = new HashMap<>();

        summary.put("totalAnalyzedExecutions", totalAnalyzedExecutions.get());
        summary.put("frameworksTracked", frameworkPatterns.size());
        summary.put("trendsMonitored", trends.size());

        // Best performer
        frameworkPatterns.values().stream()
                .max(Comparator.comparingDouble(p -> p.totalExecutions == 0 ? 0 : 
                    ((double) p.successfulExecutions / p.totalExecutions)))
                .ifPresent(best -> {
                    summary.put("bestPerformer", best.frameWorkName);
                    summary.put("bestSuccessRate", 
                        String.format("%.1f%%", ((double) best.successfulExecutions / best.totalExecutions) * 100));
                });

        // Needs improvement
        frameworkPatterns.values().stream()
                .filter(p -> p.totalExecutions > 0)
                .min(Comparator.comparingDouble(p -> (double) p.successfulExecutions / p.totalExecutions))
                .ifPresent(worst -> {
                    summary.put("needsImprovement", worst.frameWorkName);
                    summary.put("worstSuccessRate", 
                        String.format("%.1f%%", ((double) worst.successfulExecutions / worst.totalExecutions) * 100));
                });

        // Degrading trends
        long degrading = trends.values().stream()
                .filter(t -> "DEGRADING".equals(t.trend))
                .count();
        summary.put("degradingTrends", degrading);

        // Improving trends
        long improving = trends.values().stream()
                .filter(t -> "IMPROVING".equals(t.trend))
                .count();
        summary.put("improvingTrends", improving);

        return summary;
    }

    /**
     * Reset analytics (for testing)
     */
    public void reset() {
        frameworkPatterns.clear();
        trends.clear();
        totalAnalyzedExecutions.set(0);
    }
}
