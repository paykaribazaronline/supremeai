package org.example.service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * X-Builder Failure Pattern Database
 * Tracks code generation failures, patterns, and root causes
 * Enables X-Builder (code generation AI) to learn from past failures and improve
 * 
 * Stores:
 * - Failed generation attempts with full context
 * - Failure patterns (what types of code/frameworks fail most)
 * - Root cause analysis (syntax, logic, framework-specific issues)
 * - Success improvement tracking (which fixes work best)
 */
@Service
public class XBuilderFailurePatternService {

    public static class FailureRecord {
        public String failureId;
        public String projectId;
        public String componentName;
        public String framework;
        public String failureType; // SYNTAX, LOGIC, FRAMEWORK, DEPENDENCY, STRUCTURE
        public String failureReason;
        public String generatedCode;
        public String errorMessage;
        public LocalDateTime failureTime;
        public boolean wasFixed;
        public String fixApplied;
        public int fixAttemptCount;
        public double confidenceScore;
        public String selectedAgent; // Which AI agent generated this
        public Map<String, String> context; // Additional metadata

        public FailureRecord(String projectId, String componentName, String framework) {
            this.failureId = UUID.randomUUID().toString();
            this.projectId = projectId;
            this.componentName = componentName;
            this.framework = framework;
            this.failureTime = LocalDateTime.now();
            this.wasFixed = false;
            this.fixAttemptCount = 0;
            this.context = new ConcurrentHashMap<>();
        }
    }

    public static class FailurePattern {
        public String patternId;
        public String failureType;
        public String framework;
        public int occurrenceCount;
        public double successRateAfterFix;
        public LocalDateTime firstSeen;
        public LocalDateTime lastSeen;
        public List<String> commonRootCauses; // Top failure reasons
        public List<String> effectiveFixes; // Fixes that worked
        public double criticality; // How severe/blocking this pattern is

        public FailurePattern(String failureType, String framework) {
            this.patternId = failureType + "_" + framework;
            this.failureType = failureType;
            this.framework = framework;
            this.occurrenceCount = 0;
            this.successRateAfterFix = 0.0;
            this.firstSeen = LocalDateTime.now();
            this.lastSeen = LocalDateTime.now();
            this.commonRootCauses = new ArrayList<>();
            this.effectiveFixes = new ArrayList<>();
            this.criticality = 0.0;
        }
    }

    private final Map<String, FailureRecord> failureDatabase = new ConcurrentHashMap<>();
    private final Map<String, FailurePattern> patternDatabase = new ConcurrentHashMap<>();
    private final Map<String, Integer> failureTypeCounter = new ConcurrentHashMap<>();

    /**
     * Record a code generation failure
     */
    public String recordFailure(String projectId, String componentName, String framework,
                               String failureType, String failureReason, String generatedCode,
                               String errorMessage) {
        FailureRecord record = new FailureRecord(projectId, componentName, framework);
        record.failureType = failureType;
        record.failureReason = failureReason;
        record.generatedCode = generatedCode;
        record.errorMessage = errorMessage;

        failureDatabase.put(record.failureId, record);
        
        // Update pattern database
        updateFailurePattern(failureType, framework, failureReason);
        
        // Track failure type frequency
        String key = failureType + "_" + framework;
        failureTypeCounter.merge(key, 1, Integer::sum);

        System.out.println(String.format(
            "[X-Builder] Failure recorded: %s in %s (%s) - %s",
            componentName, framework, failureType, failureReason
        ));

        return record.failureId;
    }

    /**
     * Mark a failure as fixed and track the solution
     */
    public Map<String, Object> markFailureFixed(String failureId, String fixApplied, boolean success) {
        FailureRecord record = failureDatabase.get(failureId);
        if (record == null) {
            return Map.of("success", false, "error", "Failure record not found");
        }

        record.wasFixed = success;
        record.fixApplied = fixApplied;
        record.fixAttemptCount++;

        if (success) {
            // Track effective fix
            String patternKey = record.failureType + "_" + record.framework;
            FailurePattern pattern = patternDatabase.get(patternKey);
            if (pattern != null && !pattern.effectiveFixes.contains(fixApplied)) {
                pattern.effectiveFixes.add(fixApplied);
            }

            System.out.println(String.format(
                "[X-Builder] Failure %s FIXED after %d attempts: %s",
                failureId.substring(0, 8), record.fixAttemptCount, fixApplied
            ));
        }

        return Map.of(
            "success", true,
            "failureId", failureId,
            "fixed", success,
            "fixAttempts", record.fixAttemptCount
        );
    }

    /**
     * Get failure statistics by framework
     */
    public Map<String, Object> getFailureStatsByFramework(String framework) {
        List<FailureRecord> frameworkFailures = failureDatabase.values().stream()
            .filter(f -> framework.equalsIgnoreCase(f.framework))
            .collect(Collectors.toList());

        long fixedCount = frameworkFailures.stream().filter(f -> f.wasFixed).count();
        long totalCount = frameworkFailures.size();
        double fixRate = totalCount > 0 ? (fixedCount * 100.0) / totalCount : 0.0;

        Map<String, Long> typeBreakdown = frameworkFailures.stream()
            .collect(Collectors.groupingBy(f -> f.failureType, Collectors.counting()));

        return Map.of(
            "framework", framework,
            "totalFailures", totalCount,
            "fixedFailures", fixedCount,
            "fixRate", String.format("%.1f%%", fixRate),
            "failureTypeBreakdown", typeBreakdown,
            "lastFailure", frameworkFailures.stream()
                .map(f -> f.failureTime)
                .max(LocalDateTime::compareTo)
                .map(LocalDateTime::toString)
                .orElse("none")
        );
    }

    /**
     * Get failure patterns (high-level insights)
     */
    public List<Map<String, Object>> getFailurePatterns() {
        return patternDatabase.values().stream()
            .map(pattern -> Map.<String, Object>of(
                "patternId", pattern.patternId,
                "failureType", pattern.failureType,
                "framework", pattern.framework,
                "occurrences", pattern.occurrenceCount,
                "successRateAfterFix", String.format("%.1f%%", pattern.successRateAfterFix),
                "commonRootCauses", pattern.commonRootCauses,
                "effectiveFixes", pattern.effectiveFixes,
                "criticality", String.format("%.2f", pattern.criticality),
                "firstSeen", pattern.firstSeen.toString(),
                "lastSeen", pattern.lastSeen.toString()
            ))
            .sorted((a, b) -> Double.compare(
                (Double) b.get("criticality"),
                (Double) a.get("criticality")
            ))
            .collect(Collectors.toList());
    }

    /**
     * Get most critical failure patterns (top blockers)
     */
    public List<Map<String, Object>> getCriticalPatterns(int limit) {
        return getFailurePatterns().stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Get failure record details
     */
    public Map<String, Object> getFailureDetails(String failureId) {
        FailureRecord record = failureDatabase.get(failureId);
        if (record == null) {
            return Map.of("success", false, "error", "Failure record not found");
        }

        return Map.of(
            "failureId", failureId,
            "projectId", record.projectId,
            "componentName", record.componentName,
            "framework", record.framework,
            "failureType", record.failureType,
            "failureReason", record.failureReason,
            "errorMessage", record.errorMessage,
            "failureTime", record.failureTime.toString(),
            "wasFixed", record.wasFixed,
            "fixApplied", record.fixApplied != null ? record.fixApplied : "not fixed",
            "fixAttempts", record.fixAttemptCount,
            "confidenceScore", String.format("%.2f%%", record.confidenceScore * 100),
            "selectedAgent", record.selectedAgent != null ? record.selectedAgent : "unknown"
        );
    }

    /**
     * Get recommendations based on failure patterns
     */
    public Map<String, Object> getFailureRecommendations(String framework) {
        List<FailurePattern> patterns = patternDatabase.values().stream()
            .filter(p -> framework.equalsIgnoreCase(p.framework))
            .sorted((a, b) -> Double.compare(b.criticality, a.criticality))
            .collect(Collectors.toList());

        return Map.of(
            "framework", framework,
            "recommendations", patterns.stream()
                .limit(5)
                .map(p -> Map.of(
                    "avoidPattern", p.failureType,
                    "reason", String.format("Occurred %d times, success rate after fix: %.1f%%",
                        p.occurrenceCount, p.successRateAfterFix),
                    "tryTheseFixes", p.effectiveFixes,
                    "rootCauses", p.commonRootCauses
                ))
                .collect(Collectors.toList())
        );
    }

    /**
     * Get X-Builder health score (0-100)
     * Based on fix rate, pattern severity, and recent performance
     */
    public Map<String, Object> getXBuilderHealthScore() {
        long totalFailures = failureDatabase.size();
        long fixedFailures = failureDatabase.values().stream()
            .filter(f -> f.wasFixed)
            .count();

        double fixRate = totalFailures > 0 ? (fixedFailures * 100.0) / totalFailures : 100.0;
        
        // Health score = 100 - (critical patterns impact) - (recent failures impact)
        double criticalityImpact = patternDatabase.values().stream()
            .mapToDouble(p -> p.criticality)
            .sum() / Math.max(1, patternDatabase.size());
        
        double healthScore = Math.max(0, 100 - (criticalityImpact * 10) - (100 - fixRate) / 2);

        return Map.of(
            "healthScore", String.format("%.1f", healthScore),
            "overallFixRate", String.format("%.1f%%", fixRate),
            "totalFailures", totalFailures,
            "fixedFailures", fixedFailures,
            "activePatternsCount", patternDatabase.size(),
            "criticalPatternsCount", patternDatabase.values().stream()
                .filter(p -> p.criticality > 0.7)
                .count(),
            "status", healthScore >= 80 ? "HEALTHY" : healthScore >= 60 ? "DEGRADED" : "CRITICAL"
        );
    }

    /**
     * Update failure pattern with new occurrence
     */
    private void updateFailurePattern(String failureType, String framework, String failureReason) {
        String patternKey = failureType + "_" + framework;
        FailurePattern pattern = patternDatabase.computeIfAbsent(patternKey,
            k -> new FailurePattern(failureType, framework));

        pattern.occurrenceCount++;
        pattern.lastSeen = LocalDateTime.now();

        // Track common root causes
        if (!pattern.commonRootCauses.contains(failureReason) && 
            pattern.commonRootCauses.size() < 5) {
            pattern.commonRootCauses.add(failureReason);
        }

        // Update criticality (higher = more frequent)
        pattern.criticality = Math.min(1.0, pattern.occurrenceCount / 20.0);
    }

    /**
     * Clear old failure records (keep last N days)
     */
    public void cleanupOldRecords(int daysToKeep) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysToKeep);
        failureDatabase.values().removeIf(r -> r.failureTime.isBefore(cutoff));
    }

    /**
     * Get database stats
     */
    public Map<String, Object> getDatabaseStats() {
        return Map.of(
            "totalFailureRecords", failureDatabase.size(),
            "uniquePatterns", patternDatabase.size(),
            "failureTypeCount", failureTypeCounter.size(),
            "mostCommonFailureType", failureTypeCounter.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("none"),
            "healthScore", getXBuilderHealthScore()
        );
    }
}
