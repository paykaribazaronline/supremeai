package org.example.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for analyzing patterns in historical decisions
 * Extracts insights from past decisions to guide future fixes
 */
@Service
public class DecisionPatternAnalyzer {
    
    private final AgentDecisionLogger decisionLogger;
    
    public DecisionPatternAnalyzer(AgentDecisionLogger decisionLogger) {
        this.decisionLogger = decisionLogger;
    }
    
    /**
     * Get pattern analysis for a specific error type
     */
    public ErrorPatternAnalysis analyzeErrorTypePattern(String errorType) {
        List<AgentDecisionLogger.AgentDecision> decisions = decisionLogger.getAllDecisions().stream()
            .filter(d -> errorType.equals(d.decision))
            .collect(Collectors.toList());
        
        ErrorPatternAnalysis analysis = new ErrorPatternAnalysis(errorType);
        
        if (decisions.isEmpty()) {
            return analysis;
        }
        
        // Analyze success rate
        long successCount = decisions.stream()
            .filter(d -> "SUCCESS".equals(d.outcome)).count();
        analysis.successRate = (double) successCount / decisions.size();
        analysis.totalOccurrences = decisions.size();
        
        // Analyze most used strategies
        Map<String, Long> strategyUsage = decisions.stream()
            .collect(Collectors.groupingBy(d -> d.decision, Collectors.counting()));
        analysis.preferredStrategies = strategyUsage.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .limit(3)
            .map(e -> new StrategyFrequency(e.getKey(), e.getValue().intValue()))
            .collect(Collectors.toList());
        
        // Analyze average confidence
        analysis.averageConfidence = decisions.stream()
            .mapToDouble(d -> d.confidence)
            .average()
            .orElse(0.5);
        
        return analysis;
    }
    
    /**
     * Get pattern analysis for a specific strategy
     */
    public StrategyPatternAnalysis analyzeStrategyPattern(String strategy) {
        List<AgentDecisionLogger.AgentDecision> decisions = decisionLogger.getAllDecisions().stream()
            .filter(d -> strategy.equals(d.decision))
            .collect(Collectors.toList());
        
        StrategyPatternAnalysis analysis = new StrategyPatternAnalysis(strategy);
        
        if (decisions.isEmpty()) {
            return analysis;
        }
        
        // Success rate by strategy
        long successCount = decisions.stream()
            .filter(d -> "SUCCESS".equals(d.outcome)).count();
        analysis.successRate = (double) successCount / decisions.size();
        analysis.totalApplications = decisions.size();
        
        // Average execution time
        analysis.averageExecutionTime = (long) decisions.stream()
            .mapToLong(d -> Long.parseLong(d.timestamp))
            .average()
            .orElse(0);
        
        // Most common error types for this strategy
        Map<String, Long> errorTypeFreq = decisions.stream()
            .collect(Collectors.groupingBy(d -> d.decision, Collectors.counting()));
        analysis.commonErrorTypes = errorTypeFreq.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .limit(3)
            .map(e -> new ErrorTypeFrequency(e.getKey(), e.getValue().intValue()))
            .collect(Collectors.toList());
        
        return analysis;
    }
    
    /**
     * Find which error types are most problematic (lowest success rates)
     */
    public List<ProblematicErrorType> identifyProblematicErrorTypes(int limit) {
        Map<String, List<AgentDecisionLogger.AgentDecision>> byErrorType = decisionLogger.getAllDecisions().stream()
            .collect(Collectors.groupingBy(d -> d.decision));
        
        return byErrorType.entrySet().stream()
            .map(entry -> {
                List<AgentDecisionLogger.AgentDecision> decisions = entry.getValue();
                long successCount = decisions.stream()
                    .filter(d -> "SUCCESS".equals(d.outcome)).count();
                double failureRate = 1.0 - ((double) successCount / decisions.size());
                
                return new ProblematicErrorType(
                    entry.getKey(),
                    decisions.size(),
                    failureRate
                );
            })
            .filter(e -> e.totalOccurrences >= 3) // Only errors with at least 3 occurrences
            .sorted((a, b) -> Double.compare(b.failureRate, a.failureRate))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Get success patterns - combinations of strategy + error type that work well
     */
    public List<SuccessPattern> discoverSuccessPatterns(int limit) {
        Map<String, List<AgentDecisionLogger.AgentDecision>> patterns = new HashMap<>();
        
        for (AgentDecisionLogger.AgentDecision decision : decisionLogger.getAllDecisions()) {
            if ("SUCCESS".equals(decision.outcome)) {
                String key = decision.decision + ":" + decision.taskType;
                patterns.computeIfAbsent(key, k -> new ArrayList<>()).add(decision);
            }
        }
        
        return patterns.entrySet().stream()
            .filter(e -> e.getValue().size() >= 2) // At least 2 successful instances
            .map(entry -> {
                String[] parts = entry.getKey().split(":");
                return new SuccessPattern(
                    parts[0],
                    parts[1],
                    entry.getValue().size(),
                    entry.getValue().stream()
                        .mapToDouble(d -> d.confidence)
                        .average()
                        .orElse(0.5)
                );
            })
            .sorted((a, b) -> Integer.compare(b.successCount, a.successCount))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Get aggregate statistics across all decisions
     */
    public AggregatePatternStats getAggregateStats() {
        List<AgentDecisionLogger.AgentDecision> allDecisions = decisionLogger.getAllDecisions();
        
        AggregatePatternStats stats = new AggregatePatternStats();
        
        if (allDecisions.isEmpty()) {
            return stats;
        }
        
        stats.totalDecisions = allDecisions.size();
        stats.totalSuccessful = (int) allDecisions.stream()
            .filter(d -> "SUCCESS".equals(d.outcome)).count();
        stats.totalFailed = (int) allDecisions.stream()
            .filter(d -> "FAILURE".equals(d.outcome)).count();
        stats.totalPartial = (int) allDecisions.stream()
            .filter(d -> "PARTIAL".equals(d.outcome)).count();
        
        stats.globalSuccessRate = (double) stats.totalSuccessful / stats.totalDecisions;
        stats.averageConfidence = allDecisions.stream()
            .mapToDouble(d -> d.confidence)
            .average()
            .orElse(0.5);
        
        stats.uniqueErrorTypes = allDecisions.stream()
            .map(d -> d.taskType)
            .distinct()
            .count();
        stats.uniqueStrategies = allDecisions.stream()
            .map(d -> d.decision)
            .distinct()
            .count();
        
        return stats;
    }
    
    /**
     * Get recommendation for a new error
     */
    public RecommendationSuggestion getRecommendationForError(String errorType) {
        RecommendationSuggestion suggestion = new RecommendationSuggestion(errorType);
        
        // Get analysis for error type
        ErrorPatternAnalysis errorAnalysis = analyzeErrorTypePattern(errorType);
        suggestion.successRate = errorAnalysis.successRate;
        
        // Get best strategy for this error type
        if (!errorAnalysis.preferredStrategies.isEmpty()) {
            String bestStrategy = errorAnalysis.preferredStrategies.get(0).strategy;
            suggestion.suggestedStrategy = bestStrategy;
            suggestion.strategySuccessRate = errorAnalysis.successRate;
            
            // Get pattern for this strategy
            StrategyPatternAnalysis strategyAnalysis = analyzeStrategyPattern(bestStrategy);
            suggestion.strategySuccessRate = strategyAnalysis.successRate;
        }
        
        // Check if manual review is needed
        suggestion.requiresManualReview = suggestion.successRate < 0.6;
        
        return suggestion;
    }
    
    // Inner result classes
    
    public static class ErrorPatternAnalysis {
        public String errorType;
        public double successRate;
        public int totalOccurrences;
        public List<StrategyFrequency> preferredStrategies;
        public double averageConfidence;
        
        public ErrorPatternAnalysis(String errorType) {
            this.errorType = errorType;
            this.preferredStrategies = new ArrayList<>();
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("errorType", errorType);
            map.put("successRate", successRate);
            map.put("totalOccurrences", totalOccurrences);
            map.put("averageConfidence", averageConfidence);
            map.put("preferredStrategies", preferredStrategies.stream()
                .map(StrategyFrequency::toMap).collect(Collectors.toList()));
            return map;
        }
    }
    
    public static class StrategyFrequency {
        public String strategy;
        public int frequency;
        
        public StrategyFrequency(String strategy, int frequency) {
            this.strategy = strategy;
            this.frequency = frequency;
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("strategy", strategy);
            map.put("frequency", frequency);
            return map;
        }
    }
    
    public static class StrategyPatternAnalysis {
        public String strategy;
        public double successRate;
        public int totalApplications;
        public long averageExecutionTime;
        public List<ErrorTypeFrequency> commonErrorTypes;
        
        public StrategyPatternAnalysis(String strategy) {
            this.strategy = strategy;
            this.commonErrorTypes = new ArrayList<>();
        }
    }
    
    public static class ErrorTypeFrequency {
        public String errorType;
        public int frequency;
        
        public ErrorTypeFrequency(String errorType, int frequency) {
            this.errorType = errorType;
            this.frequency = frequency;
        }
    }
    
    public static class ProblematicErrorType {
        public String errorType;
        public int totalOccurrences;
        public double failureRate;
        
        public ProblematicErrorType(String errorType, int totalOccurrences, double failureRate) {
            this.errorType = errorType;
            this.totalOccurrences = totalOccurrences;
            this.failureRate = failureRate;
        }
    }
    
    public static class SuccessPattern {
        public String strategy;
        public String errorType;
        public int successCount;
        public double averageConfidence;
        
        public SuccessPattern(String strategy, String errorType, int successCount, double averageConfidence) {
            this.strategy = strategy;
            this.errorType = errorType;
            this.successCount = successCount;
            this.averageConfidence = averageConfidence;
        }
    }
    
    public static class AggregatePatternStats {
        public int totalDecisions;
        public int totalSuccessful;
        public int totalFailed;
        public int totalPartial;
        public double globalSuccessRate;
        public double averageConfidence;
        public long uniqueErrorTypes;
        public long uniqueStrategies;
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("totalDecisions", totalDecisions);
            map.put("totalSuccessful", totalSuccessful);
            map.put("totalFailed", totalFailed);
            map.put("totalPartial", totalPartial);
            map.put("globalSuccessRate", globalSuccessRate);
            map.put("averageConfidence", averageConfidence);
            map.put("uniqueErrorTypes", uniqueErrorTypes);
            map.put("uniqueStrategies", uniqueStrategies);
            return map;
        }
    }
    
    public static class RecommendationSuggestion {
        public String errorType;
        public String suggestedStrategy;
        public double successRate;
        public double strategySuccessRate;
        public boolean requiresManualReview;
        
        public RecommendationSuggestion(String errorType) {
            this.errorType = errorType;
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("errorType", errorType);
            map.put("suggestedStrategy", suggestedStrategy);
            map.put("successRate", successRate);
            map.put("strategySuccessRate", strategySuccessRate);
            map.put("requiresManualReview", requiresManualReview);
            return map;
        }
    }
}
