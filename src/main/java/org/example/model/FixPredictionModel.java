package org.example.model;

import java.io.Serializable;
import java.util.*;

/**
 * Machine Learning model for predicting fix success probability
 * Uses historical decision data to score fix candidates
 */
public class FixPredictionModel implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Model training data
    private List<FixTrainingData> trainingData;
    private Map<String, StrategyMetrics> strategyMetrics;
    private Map<String, ErrorTypeMetrics> errorTypeMetrics;
    private double globalSuccessRate;
    private int totalDecisions;
    
    // Model parameters
    private double learningRate = 0.01;
    private int minSamplesForConfidence = 5;
    
    public FixPredictionModel() {
        this.trainingData = new ArrayList<>();
        this.strategyMetrics = new HashMap<>();
        this.errorTypeMetrics = new HashMap<>();
        this.globalSuccessRate = 0.5; // neutral prior
        this.totalDecisions = 0;
    }
    
    /**
     * Train model with a decision outcome
     */
    public void trainWithDecision(String strategyType, String errorType, 
                                  boolean wasSuccessful, float confidence, 
                                  long executionTimeMs) {
        FixTrainingData data = new FixTrainingData(
            strategyType, errorType, wasSuccessful, confidence, executionTimeMs
        );
        trainingData.add(data);
        totalDecisions++;
        
        // Update strategy metrics
        strategyMetrics.computeIfAbsent(strategyType, k -> new StrategyMetrics())
            .recordOutcome(wasSuccessful, confidence, executionTimeMs);
        
        // Update error type metrics
        errorTypeMetrics.computeIfAbsent(errorType, k -> new ErrorTypeMetrics())
            .recordOutcome(wasSuccessful, confidence);
        
        // Update global success rate (exponential moving average)
        int successCount = (int) trainingData.stream()
            .filter(d -> d.wasSuccessful).count();
        this.globalSuccessRate = (double) successCount / totalDecisions;
    }
    
    /**
     * Predict success probability for a fix candidate
     * Returns [0.0, 1.0] confidence score
     */
    public double predictSuccessProbability(String fixStrategy, String errorType) {
        if (totalDecisions == 0) {
            return 0.5; // neutral prior
        }
        
        // Get strategy-level metrics
        StrategyMetrics stratMetrics = strategyMetrics.getOrDefault(
            fixStrategy, new StrategyMetrics()
        );
        double strategyScore = stratMetrics.successRate > 0 ? 
            stratMetrics.successRate : globalSuccessRate;
        
        // Get error-type-level metrics
        ErrorTypeMetrics errorMetrics = errorTypeMetrics.getOrDefault(
            errorType, new ErrorTypeMetrics()
        );
        double errorScore = errorMetrics.successRate > 0 ? 
            errorMetrics.successRate : globalSuccessRate;
        
        // Combine scores with weighting (60% strategy, 40% error type)
        double prediction = (0.6 * strategyScore) + (0.4 * errorScore);
        
        // Clamp to [0, 1]
        return Math.max(0.0, Math.min(1.0, prediction));
    }
    
    /**
     * Get confidence in prediction based on sample size
     */
    public double getPredictionConfidence(String fixStrategy, String errorType) {
        StrategyMetrics stratMetrics = strategyMetrics.get(fixStrategy);
        int samples = stratMetrics != null ? stratMetrics.sampleCount : 0;
        
        // Confidence increases with sample size, capped at 1.0
        double confidence = Math.min(1.0, (double) samples / minSamplesForConfidence);
        return Math.max(0.1, confidence); // minimum 10% confidence
    }
    
    /**
     * Get predicted execution time for a fix
     */
    public long predictExecutionTime(String fixStrategy) {
        StrategyMetrics metrics = strategyMetrics.get(fixStrategy);
        if (metrics != null && metrics.sampleCount > 0) {
            return metrics.averageExecutionTimeMs;
        }
        return 5000; // default 5 seconds
    }
    
    /**
     * Get model statistics
     */
    public ModelStats getStats() {
        ModelStats stats = new ModelStats();
        stats.totalTrainingDecisions = totalDecisions;
        stats.globalSuccessRate = globalSuccessRate;
        stats.strategiesLearned = strategyMetrics.size();
        stats.errorTypesLearned = errorTypeMetrics.size();
        stats.strategyMetrics = new HashMap<>(strategyMetrics);
        stats.errorTypeMetrics = new HashMap<>(errorTypeMetrics);
        return stats;
    }
    
    /**
     * Get top performing strategies
     */
    public List<String> getTopStrategies(int limit) {
        return strategyMetrics.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue().successRate, a.getValue().successRate))
            .limit(limit)
            .map(Map.Entry::getKey)
            .toList();
    }
    
    /**
     * Training data record
     */
    public static class FixTrainingData {
        public String strategyType;
        public String errorType;
        public boolean wasSuccessful;
        public float confidence;
        public long executionTimeMs;
        public long timestamp;
        
        public FixTrainingData(String strategyType, String errorType, 
                              boolean wasSuccessful, float confidence, 
                              long executionTimeMs) {
            this.strategyType = strategyType;
            this.errorType = errorType;
            this.wasSuccessful = wasSuccessful;
            this.confidence = confidence;
            this.executionTimeMs = executionTimeMs;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * Strategy performance metrics
     */
    public static class StrategyMetrics {
        public int sampleCount = 0;
        public int successCount = 0;
        public double successRate = 0.0;
        public long totalExecutionTimeMs = 0;
        public long averageExecutionTimeMs = 0;
        public double averageConfidence = 0.0;
        
        public void recordOutcome(boolean success, float confidence, long executionTimeMs) {
            sampleCount++;
            if (success) successCount++;
            totalExecutionTimeMs += executionTimeMs;
            averageExecutionTimeMs = totalExecutionTimeMs / sampleCount;
            
            // Update success rate with exponential moving average
            successRate = (double) successCount / sampleCount;
            
            // Update average confidence
            averageConfidence = (averageConfidence * (sampleCount - 1) + confidence) / sampleCount;
        }
    }
    
    /**
     * Error type performance metrics
     */
    public static class ErrorTypeMetrics {
        public int sampleCount = 0;
        public int successCount = 0;
        public double successRate = 0.0;
        public double averageConfidence = 0.0;
        
        public void recordOutcome(boolean success, float confidence) {
            sampleCount++;
            if (success) successCount++;
            successRate = (double) successCount / sampleCount;
            averageConfidence = (averageConfidence * (sampleCount - 1) + confidence) / sampleCount;
        }
    }
    
    /**
     * Model statistics container
     */
    public static class ModelStats {
        public int totalTrainingDecisions;
        public double globalSuccessRate;
        public int strategiesLearned;
        public int errorTypesLearned;
        public Map<String, StrategyMetrics> strategyMetrics;
        public Map<String, ErrorTypeMetrics> errorTypeMetrics;
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("totalDecisions", totalTrainingDecisions);
            map.put("globalSuccessRate", globalSuccessRate);
            map.put("strategiesLearned", strategiesLearned);
            map.put("errorTypesLearned", errorTypesLearned);
            map.put("strategyCount", strategyMetrics.size());
            map.put("errorTypeCount", errorTypeMetrics.size());
            return map;
        }
    }
    
    // Getters and Setters
    public List<FixTrainingData> getTrainingData() {
        return trainingData;
    }
    
    public int getTotalDecisions() {
        return totalDecisions;
    }
    
    public double getGlobalSuccessRate() {
        return globalSuccessRate;
    }
    
    public int getMinSamplesForConfidence() {
        return minSamplesForConfidence;
    }
    
    public void setMinSamplesForConfidence(int minSamples) {
        this.minSamplesForConfidence = minSamples;
    }
}
