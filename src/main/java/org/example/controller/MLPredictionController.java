package org.example.controller;

import org.example.model.FixPredictionModel;
import org.example.model.FixVariant;
import org.example.service.ConfidenceScorer;
import org.example.service.DecisionPatternAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST Controller for ML-powered fix predictions and pattern analysis
 * Provides 5 endpoints for ML model inference and decision analysis
 */
@RestController
@RequestMapping("/api/v1/ml")
public class MLPredictionController {
    
    @Autowired
    private ConfidenceScorer confidenceScorer;
    
    @Autowired
    private DecisionPatternAnalyzer patternAnalyzer;
    
    /**
     * POST /predict
     * Predict success probability for a fix variant
     */
    @PostMapping("/predict")
    public ResponseEntity<Map<String, Object>> predictFixSuccess(
            @RequestBody PredictionRequest request) {
        
        FixVariant variant = parseVariant(request.variant);
        ConfidenceScorer.FixConfidenceScore score = 
            confidenceScorer.scoreVariant(variant, request.errorType);
        
        Map<String, Object> response = new HashMap<>();
        response.put("variantId", score.variantId);
        response.put("successProbability", score.successProbability);
        response.put("predictionConfidence", score.predictionConfidence);
        response.put("predictedExecutionTime", score.predictedExecutionTime);
        response.put("compositeConfidence", score.compositeConfidence);
        response.put("recommendationLevel", score.recommendationLevel);
        response.put("riskAssessment", score.riskAssessment.toMap());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /rank-variants
     * Rank multiple fix variants by confidence
     */
    @PostMapping("/rank-variants")
    public ResponseEntity<Map<String, Object>> rankVariants(
            @RequestBody RankVariantsRequest request) {
        
        List<FixVariant> variants = request.variants.stream()
            .map(this::parseVariant)
            .toList();
        
        List<ConfidenceScorer.FixConfidenceScore> ranked = 
            confidenceScorer.rankVariantsByConfidence(variants, request.errorType);
        
        Map<String, Object> response = new HashMap<>();
        response.put("errorType", request.errorType);
        response.put("totalVariants", ranked.size());
        response.put("ranking", ranked.stream()
            .map(score -> score.toMap())
            .toList());
        response.put("topRecommendation", !ranked.isEmpty() ? ranked.get(0).toMap() : null);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /patterns/error-type/{errorType}
     * Analyze patterns for a specific error type
     */
    @GetMapping("/patterns/error-type/{errorType}")
    public ResponseEntity<Map<String, Object>> analyzeErrorTypePattern(
            @PathVariable String errorType) {
        
        DecisionPatternAnalyzer.ErrorPatternAnalysis analysis = 
            patternAnalyzer.analyzeErrorTypePattern(errorType);
        
        Map<String, Object> response = new HashMap<>();
        response.put("analysis", analysis.toMap());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /patterns/strategy/{strategy}
     * Analyze patterns for a specific fix strategy
     */
    @GetMapping("/patterns/strategy/{strategy}")
    public ResponseEntity<Map<String, Object>> analyzeStrategyPattern(
            @PathVariable String strategy) {
        
        DecisionPatternAnalyzer.StrategyPatternAnalysis analysis = 
            patternAnalyzer.analyzeStrategyPattern(strategy);
        
        Map<String, Object> response = new HashMap<>();
        response.put("strategy", analysis.strategy);
        response.put("successRate", analysis.successRate);
        response.put("totalApplications", analysis.totalApplications);
        response.put("commonErrorTypes", analysis.commonErrorTypes.stream()
            .map(et -> new HashMap<String, Object>() {{
                put("errorType", et.errorType);
                put("frequency", et.frequency);
            }})
            .toList());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /insights/aggregate
     * Get aggregate statistics and insights
     */
    @GetMapping("/insights/aggregate")
    public ResponseEntity<Map<String, Object>> getAggregateInsights() {
        
        DecisionPatternAnalyzer.AggregatePatternStats stats = 
            patternAnalyzer.getAggregateStats();
        
        List<DecisionPatternAnalyzer.ProblematicErrorType> problematic = 
            patternAnalyzer.identifyProblematicErrorTypes(5);
        
        List<DecisionPatternAnalyzer.SuccessPattern> successPatterns = 
            patternAnalyzer.discoverSuccessPatterns(5);
        
        FixPredictionModel.ModelStats modelStats = 
            confidenceScorer.getModelStats();
        
        Map<String, Object> response = new HashMap<>();
        response.put("aggregateStats", stats.toMap());
        response.put("problematicErrorTypes", problematic.stream()
            .map(e -> new HashMap<String, Object>() {{
                put("errorType", e.errorType);
                put("totalOccurrences", e.totalOccurrences);
                put("failureRate", e.failureRate);
            }})
            .toList());
        response.put("successPatterns", successPatterns.stream()
            .map(p -> new HashMap<String, Object>() {{
                put("strategy", p.strategy);
                put("errorType", p.errorType);
                put("successCount", p.successCount);
                put("averageConfidence", p.averageConfidence);
            }})
            .toList());
        response.put("modelStats", modelStats.toMap());
        response.put("topStrategies", confidenceScorer.getTopStrategies(3));
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /recommendation/{errorType}
     * Get ML recommendation for an error type
     */
    @GetMapping("/recommendation/{errorType}")
    public ResponseEntity<Map<String, Object>> getRecommendation(
            @PathVariable String errorType) {
        
        DecisionPatternAnalyzer.RecommendationSuggestion suggestion = 
            patternAnalyzer.getRecommendationForError(errorType);
        
        Map<String, Object> response = new HashMap<>();
        response.put("errorType", suggestion.errorType);
        response.put("suggestedStrategy", suggestion.suggestedStrategy);
        response.put("successRate", suggestion.successRate);
        response.put("strategySuccessRate", suggestion.strategySuccessRate);
        response.put("requiresManualReview", suggestion.requiresManualReview);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /train
     * Train model with decision outcome
     */
    @PostMapping("/train")
    public ResponseEntity<Map<String, Object>> trainModel(
            @RequestBody TrainingRequest request) {
        
        confidenceScorer.trainModelWithOutcome(
            request.strategyType,
            request.errorType,
            request.wasSuccessful,
            request.confidence,
            request.executionTimeMs
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "TRAINING_COMPLETE");
        response.put("strategy", request.strategyType);
        response.put("errorType", request.errorType);
        response.put("outcome", request.wasSuccessful ? "SUCCESS" : "FAILED");
        response.put("modelStats", confidenceScorer.getModelStats().toMap());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /model/stats
     * Get current model statistics
     */
    @GetMapping("/model/stats")
    public ResponseEntity<Map<String, Object>> getModelStats() {
        FixPredictionModel.ModelStats stats = confidenceScorer.getModelStats();
        
        Map<String, Object> response = new HashMap<>();
        response.put("stats", stats.toMap());
        response.put("topStrategies", confidenceScorer.getTopStrategies(5));
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    // Helper method to parse variant from map
    private FixVariant parseVariant(Map<String, Object> variantMap) {
        FixVariant variant = new FixVariant();
        variant.setVariantId((String) variantMap.get("variantId"));
        variant.setFixId((String) variantMap.get("fixId"));
        variant.setStrategy((String) variantMap.get("strategy"));
        variant.setImplementation((String) variantMap.get("implementation"));
        variant.setSuccessRate(((Number) variantMap.getOrDefault("successRate", 0.5)).floatValue());
        variant.setExecutionTime(((Number) variantMap.getOrDefault("executionTime", 5000)).floatValue());
        variant.setRegressionDetected(((Number) variantMap.getOrDefault("regressionDetected", 0)).floatValue());
        return variant;
    }
    
    // Request/Response DTOs
    
    public static class PredictionRequest {
        public Map<String, Object> variant;
        public String errorType;
    }
    
    public static class RankVariantsRequest {
        public List<Map<String, Object>> variants;
        public String errorType;
    }
    
    public static class TrainingRequest {
        public String strategyType;
        public String errorType;
        public boolean wasSuccessful;
        public float confidence;
        public long executionTimeMs;
    }
}
