package org.example.service;

import org.example.model.FixPredictionModel;
import org.example.model.FixVariant;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for scoring fix confidence based on ML predictions
 * Provides confidence levels for recommendations
 */
@Service
public class ConfidenceScorer {
    
    private FixPredictionModel model;
    
    public ConfidenceScorer() {
        this.model = new FixPredictionModel();
    }
    
    /**
     * Score a fix variant with confidence metrics
     */
    public FixConfidenceScore scoreVariant(FixVariant variant, String errorType) {
        String strategyType = variant.getStrategy();
        
        // Predict success probability
        double successProbability = model.predictSuccessProbability(strategyType, errorType);
        
        // Get prediction confidence based on sample size
        double predictionConfidence = model.getPredictionConfidence(strategyType, errorType);
        
        // Predicted execution time
        long predictedExecutionTime = (long) model.predictExecutionTime(strategyType);
        
        // Create composite confidence score
        double compositeScore = calculateCompositeScore(
            successProbability, 
            predictionConfidence, 
            variant.getSuccessRate(),
            variant.getRegressionDetected()
        );
        
        FixConfidenceScore score = new FixConfidenceScore();
        score.variantId = variant.getVariantId();
        score.successProbability = successProbability;
        score.predictionConfidence = predictionConfidence;
        score.predictedExecutionTime = predictedExecutionTime;
        score.compositeConfidence = compositeScore;
        score.recommendationLevel = getRecommendationLevel(compositeScore);
        score.riskAssessment = assessRisk(variant, successProbability, predictionConfidence);
        
        return score;
    }
    
    /**
     * Score multiple variants and return ranked list
     */
    public List<FixConfidenceScore> rankVariantsByConfidence(List<FixVariant> variants, String errorType) {
        return variants.stream()
            .map(v -> scoreVariant(v, errorType))
            .sorted((a, b) -> Double.compare(b.compositeConfidence, a.compositeConfidence))
            .collect(Collectors.toList());
    }
    
    /**
     * Calculate composite confidence from multiple factors
     * Weights: 40% success probability, 30% prediction confidence, 20% variant success rate, 10% regression risk
     */
    private double calculateCompositeScore(double successProbability, double predictionConfidence, 
                                          double variantSuccessRate, double regressionScore) {
        double regressionRisk = regressionScore < 0.5 ? 1.0 : 0.0;
        
        double composite = (0.40 * successProbability) +
                          (0.30 * predictionConfidence) +
                          (0.20 * variantSuccessRate) +
                          (0.10 * regressionRisk);
        
        return Math.max(0.0, Math.min(1.0, composite));
    }
    
    /**
     * Determine recommendation level based on confidence score
     */
    private String getRecommendationLevel(double confidence) {
        if (confidence >= 0.85) return "HIGHLY_RECOMMENDED";
        if (confidence >= 0.70) return "RECOMMENDED";
        if (confidence >= 0.55) return "MODERATE";
        if (confidence >= 0.40) return "LOW_CONFIDENCE";
        return "NOT_RECOMMENDED";
    }
    
    /**
     * Assess risk level for applying this fix
     */
    private RiskAssessment assessRisk(FixVariant variant, double successProbability, 
                                      double predictionConfidence) {
        RiskAssessment risk = new RiskAssessment();
        
        // Check various risk factors
        risk.regressionRisk = variant.getRegressionDetected() > 0.5 ? "HIGH" : "LOW";
        risk.executionTimeRisk = variant.getExecutionTime() > 30 ? "MEDIUM" : "LOW";
        risk.confidenceRisk = predictionConfidence < 0.3 ? "HIGH" : (predictionConfidence < 0.6 ? "MEDIUM" : "LOW");
        risk.successRisk = successProbability < 0.5 ? "HIGH" : (successProbability < 0.7 ? "MEDIUM" : "LOW");
        
        // Overall risk
        risk.overallRisk = determineOverallRisk(risk);
        
        // Recommendation for approval
        risk.requiresManualReview = successProbability < 0.6 || predictionConfidence < 0.3;
        
        return risk;
    }
    
    /**
     * Determine overall risk level from individual risk factors
     */
    private String determineOverallRisk(RiskAssessment risk) {
        int highRiskCount = 0;
        if ("HIGH".equals(risk.regressionRisk)) highRiskCount++;
        if ("HIGH".equals(risk.confidenceRisk)) highRiskCount++;
        if ("HIGH".equals(risk.successRisk)) highRiskCount++;
        
        if (highRiskCount >= 2) return "HIGH";
        if (highRiskCount == 1 || "MEDIUM".equals(risk.executionTimeRisk)) return "MEDIUM";
        return "LOW";
    }
    
    /**
     * Train model with decision outcome
     */
    public void trainModelWithOutcome(String strategyType, String errorType, 
                                     boolean wasSuccessful, float confidence, 
                                     long executionTimeMs) {
        model.trainWithDecision(strategyType, errorType, wasSuccessful, confidence, executionTimeMs);
    }
    
    /**
     * Get model statistics
     */
    public FixPredictionModel.ModelStats getModelStats() {
        return model.getStats();
    }
    
    /**
     * Get top performing strategies
     */
    public List<String> getTopStrategies(int limit) {
        return model.getTopStrategies(limit);
    }
    
    // Inner classes for response objects
    
    public static class FixConfidenceScore {
        public String variantId;
        public double successProbability;        // [0, 1] - predicted chance of success
        public double predictionConfidence;      // [0, 1] - confidence in the prediction
        public long predictedExecutionTime;      // predicted time in ms
        public double compositeConfidence;       // [0, 1] - composite score for ranking
        public String recommendationLevel;       // HIGHLY_RECOMMENDED, RECOMMENDED, MODERATE, LOW_CONFIDENCE, NOT_RECOMMENDED
        public RiskAssessment riskAssessment;    // detailed risk breakdown
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("variantId", variantId);
            map.put("successProbability", successProbability);
            map.put("predictionConfidence", predictionConfidence);
            map.put("predictedExecutionTime", predictedExecutionTime);
            map.put("compositeConfidence", compositeConfidence);
            map.put("recommendationLevel", recommendationLevel);
            map.put("riskAssessment", riskAssessment.toMap());
            return map;
        }
    }
    
    public static class RiskAssessment {
        public String regressionRisk;    // HIGH, MEDIUM, LOW
        public String executionTimeRisk; // HIGH, MEDIUM, LOW
        public String confidenceRisk;    // HIGH, MEDIUM, LOW
        public String successRisk;       // HIGH, MEDIUM, LOW
        public String overallRisk;       // HIGH, MEDIUM, LOW
        public boolean requiresManualReview;
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("regressionRisk", regressionRisk);
            map.put("executionTimeRisk", executionTimeRisk);
            map.put("confidenceRisk", confidenceRisk);
            map.put("successRisk", successRisk);
            map.put("overallRisk", overallRisk);
            map.put("requiresManualReview", requiresManualReview);
            return map;
        }
    }
}
