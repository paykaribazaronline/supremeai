package org.example.service;

import org.example.model.FixVariant;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 6 Week 7-8: Fix Variant Comparator Service
 * Compares fix variations and recommends the best option
 * 
 * Features:
 * - Head-to-head variant comparison
 * - Weighted scoring system
 * - Statistical analysis
 * - Recommendation generation
 */
@Service
public class FixVariantComparator {

    /**
     * Compare multiple variants and select the best one
     */
    public FixVariant selectBest(List<FixVariant> variants) {
        if (variants.isEmpty()) {
            return null;
        }
        
        if (variants.size() == 1) {
            return variants.get(0);
        }

        return variants.stream()
            .max(Comparator.comparingDouble(FixVariant::calculateScore))
            .orElse(null);
    }

    /**
     * Rank variants by score
     */
    public List<FixVariant> rank(List<FixVariant> variants) {
        return variants.stream()
            .sorted((a, b) -> Float.compare(b.calculateScore(), a.calculateScore()))
            .collect(Collectors.toList());
    }

    /**
     * Compare two variants in detail
     */
    public ComparisonResult compare(FixVariant variant1, FixVariant variant2) {
        ComparisonResult result = new ComparisonResult();
        
        result.variant1Id = variant1.getVariantId();
        result.variant2Id = variant2.getVariantId();
        result.variant1Score = variant1.calculateScore();
        result.variant2Score = variant2.calculateScore();
        
        result.scoreDifference = result.variant1Score - result.variant2Score;
        result.winner = result.scoreDifference > 0 ? variant1.getVariantId() : variant2.getVariantId();
        result.confidence = (Math.abs(result.scoreDifference) / 100.0f);

        // Detailed metrics comparison
        result.successRateDiff = variant1.getSuccessRate() - variant2.getSuccessRate();
        result.executionTimeDiff = variant2.getExecutionTime() - variant1.getExecutionTime(); // Lower is better
        result.regressionDiff = variant2.getRegressionDetected() - variant1.getRegressionDetected(); // Lower is better

        // Determine advantages
        if (variant1.getSuccessRate() > variant2.getSuccessRate()) {
            result.advantages.add("Variant 1 has higher success rate (" + 
                String.format("%.1f%%", variant1.getSuccessRate() * 100) + ")");
        } else if (variant2.getSuccessRate() > variant1.getSuccessRate()) {
            result.advantages.add("Variant 2 has higher success rate (" + 
                String.format("%.1f%%", variant2.getSuccessRate() * 100) + ")");
        }

        if (variant1.getExecutionTime() < variant2.getExecutionTime()) {
            result.advantages.add("Variant 1 executes faster (" + 
                String.format("%.1fms", variant1.getExecutionTime()) + ")");
        } else if (variant2.getExecutionTime() < variant1.getExecutionTime()) {
            result.advantages.add("Variant 2 executes faster (" + 
                String.format("%.1fms", variant2.getExecutionTime()) + ")");
        }

        if (variant1.getRegressionDetected() < variant2.getRegressionDetected()) {
            result.advantages.add("Variant 1 has lower regression risk (" + 
                String.format("%.1f%%", variant1.getRegressionDetected() * 100) + ")");
        } else if (variant2.getRegressionDetected() < variant1.getRegressionDetected()) {
            result.advantages.add("Variant 2 has lower regression risk (" + 
                String.format("%.1f%%", variant2.getRegressionDetected() * 100) + ")");
        }

        return result;
    }

    /**
     * Analyze variants for patterns
     */
    public VariantAnalysis analyze(List<FixVariant> variants) {
        VariantAnalysis analysis = new VariantAnalysis();
        
        if (variants.isEmpty()) {
            return analysis;
        }

        analysis.totalVariants = variants.size();
        analysis.avgScore = (float) variants.stream()
            .mapToDouble(FixVariant::calculateScore)
            .average()
            .orElse(0);

        analysis.avgSuccessRate = (float) variants.stream()
            .mapToDouble(FixVariant::getSuccessRate)
            .average()
            .orElse(0);

        analysis.avgExecutionTime = (float) variants.stream()
            .mapToDouble(FixVariant::getExecutionTime)
            .average()
            .orElse(0);

        analysis.avgRegressionRate = (float) variants.stream()
            .mapToDouble(FixVariant::getRegressionDetected)
            .average()
            .orElse(0);

        // Group by strategy
        analysis.strategyDistribution = variants.stream()
            .collect(Collectors.groupingBy(
                FixVariant::getStrategy,
                Collectors.summingInt(v -> 1)
            ));

        // Find outliers
        double variance = variants.stream()
            .mapToDouble(v -> Math.pow(v.calculateScore() - analysis.avgScore, 2))
            .average()
            .orElse(0);
        analysis.stdDev = (float) Math.sqrt(variance);

        // Best and worst
        analysis.bestVariant = selectBest(variants);
        analysis.worstVariant = variants.stream()
            .min(Comparator.comparingDouble(FixVariant::calculateScore))
            .orElse(null);

        return analysis;
    }

    /**
     * Get recommendation for variant selection
     */
    public Recommendation getRecommendation(List<FixVariant> variants) {
        if (variants.isEmpty()) {
            return new Recommendation("No variants available", "Unable to select", 0, null);
        }

        FixVariant best = selectBest(variants);
        VariantAnalysis analysis = analyze(variants);

        String recommendation = String.format(
            "Recommend %s with score %.1f (%.1f%% success rate, %.1fms execution time)",
            best.getStrategy(),
            best.calculateScore(),
            best.getSuccessRate() * 100,
            best.getExecutionTime()
        );

        String reasoning = String.format(
            "This variant outperforms the average by %.1f%% (avg score: %.1f)",
            (best.calculateScore() - analysis.avgScore),
            analysis.avgScore
        );

        return new Recommendation(recommendation, reasoning, best.calculateScore(), best.getVariantId());
    }

    // ==================== Comparison Result ====================

    public static class ComparisonResult {
        public String variant1Id;
        public String variant2Id;
        public float variant1Score;
        public float variant2Score;
        public float scoreDifference;
        public String winner;
        public float confidence;
        public float successRateDiff;
        public float executionTimeDiff;
        public float regressionDiff;
        public List<String> advantages = new ArrayList<>();

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("variant1Id", variant1Id);
            map.put("variant2Id", variant2Id);
            map.put("variant1Score", variant1Score);
            map.put("variant2Score", variant2Score);
            map.put("scoreDifference", scoreDifference);
            map.put("winner", winner);
            map.put("confidence", confidence);
            map.put("advantages", advantages);
            return map;
        }
    }

    // ==================== Variant Analysis ====================

    public static class VariantAnalysis {
        public int totalVariants;
        public float avgScore;
        public float avgSuccessRate;
        public float avgExecutionTime;
        public float avgRegressionRate;
        public float stdDev;
        public FixVariant bestVariant;
        public FixVariant worstVariant;
        public Map<String, Integer> strategyDistribution = new HashMap<>();

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("totalVariants", totalVariants);
            map.put("avgScore", avgScore);
            map.put("avgSuccessRate", avgSuccessRate);
            map.put("avgExecutionTime", avgExecutionTime);
            map.put("avgRegressionRate", avgRegressionRate);
            map.put("stdDev", stdDev);
            map.put("bestVariantId", bestVariant != null ? bestVariant.getVariantId() : null);
            map.put("worstVariantId", worstVariant != null ? worstVariant.getVariantId() : null);
            map.put("strategyDistribution", strategyDistribution);
            return map;
        }
    }

    // ==================== Recommendation ====================

    public static class Recommendation {
        public String recommendation;
        public String reasoning;
        public float confidence;
        public String selectedVariantId;

        public Recommendation(String recommendation, String reasoning, float confidence, String selectedVariantId) {
            this.recommendation = recommendation;
            this.reasoning = reasoning;
            this.confidence = confidence;
            this.selectedVariantId = selectedVariantId;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("recommendation", recommendation);
            map.put("reasoning", reasoning);
            map.put("confidence", confidence);
            map.put("selectedVariantId", selectedVariantId);
            return map;
        }
    }
}
