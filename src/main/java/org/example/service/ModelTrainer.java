package org.example.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

/**
 * Service for training ML models with decision outcomes
 * Integrates model training with decision logging pipeline
 */
@Service
public class ModelTrainer {
    
    private final ConfidenceScorer confidenceScorer;
    private final AgentDecisionLogger decisionLogger;
    private final DecisionPatternAnalyzer patternAnalyzer;
    
    // Training queue for async processing
    private final BlockingQueue<TrainingJob> trainingQueue = new LinkedBlockingQueue<>();
    private final ExecutorService trainingExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ModelTrainerThread");
        t.setDaemon(true);
        return t;
    });
    
    private volatile boolean running = true;
    private TrainingStats trainingStats;
    
    public ModelTrainer(ConfidenceScorer confidenceScorer, 
                       AgentDecisionLogger decisionLogger,
                       DecisionPatternAnalyzer patternAnalyzer) {
        this.confidenceScorer = confidenceScorer;
        this.decisionLogger = decisionLogger;
        this.patternAnalyzer = patternAnalyzer;
        this.trainingStats = new TrainingStats();
        startAsyncTraining();
    }
    
    /**
     * Queue a decision outcome for model training
     */
    public void queueDecisionForTraining(String strategyType, String errorType,
                                        boolean wasSuccessful, float confidence,
                                        long executionTimeMs) {
        TrainingJob job = new TrainingJob(
            strategyType, errorType, wasSuccessful, confidence, executionTimeMs
        );
        try {
            trainingQueue.offer(job, 5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Synchronously train model with decision outcome
     */
    public void trainImmediately(String strategyType, String errorType,
                                boolean wasSuccessful, float confidence,
                                long executionTimeMs) {
        confidenceScorer.trainModelWithOutcome(
            strategyType, errorType, wasSuccessful, confidence, executionTimeMs
        );
        
        trainingStats.totalTrainingEvents++;
        if (wasSuccessful) {
            trainingStats.successfulOutcomes++;
        } else {
            trainingStats.failedOutcomes++;
        }
        trainingStats.lastTrainingTime = System.currentTimeMillis();
    }
    
    /**
     * Batch train model with multiple decisions
     */
    public BatchTrainingResult batchTrain(List<TrainingData> trainingDataList) {
        BatchTrainingResult result = new BatchTrainingResult();
        result.requestedCount = trainingDataList.size();
        
        for (TrainingData data : trainingDataList) {
            try {
                trainImmediately(
                    data.strategyType,
                    data.errorType,
                    data.wasSuccessful,
                    data.confidence,
                    data.executionTimeMs
                );
                result.successCount++;
            } catch (Exception e) {
                result.failureCount++;
                result.errors.add(e.getMessage());
            }
        }
        
        result.completionTime = System.currentTimeMillis();
        return result;
    }
    
    /**
     * Retrain model from all historical decisions
     * Useful for model recovery or after parameter changes
     */
    public RetrainingResult retrainFromHistory() {
        RetrainingResult result = new RetrainingResult();
        result.startTime = System.currentTimeMillis();
        
        // Get all historical decisions
        List<AgentDecisionLogger.AgentDecision> allDecisions = decisionLogger.getAllDecisions();
        result.totalDecisionsProcessed = allDecisions.size();
        
        // Clear and retrain
        for (AgentDecisionLogger.AgentDecision decision : allDecisions) {
            try {
                boolean wasSuccessful = "SUCCESS".equals(decision.outcome);
                trainImmediately(
                    decision.decision,
                    decision.taskType,
                    wasSuccessful,
                    decision.confidence,
                    Long.parseLong(decision.timestamp)
                );
                result.successfulRetrains++;
            } catch (Exception e) {
                result.failedRetrains++;
                result.errors.add(e.getMessage());
            }
        }
        
        result.endTime = System.currentTimeMillis();
        result.durationMs = result.endTime - result.startTime;
        
        return result;
    }
    
    /**
     * Get training statistics
     */
    public TrainingStats getTrainingStats() {
        return new TrainingStats(trainingStats);
    }
    
    /**
     * Get model performance report
     */
    public ModelPerformanceReport getPerformanceReport() {
        ModelPerformanceReport report = new ModelPerformanceReport();
        report.trainingStats = new TrainingStats(trainingStats);
        
        // Get pattern analysis
        DecisionPatternAnalyzer.AggregatePatternStats aggregateStats = 
            patternAnalyzer.getAggregateStats();
        report.aggregateStats = aggregateStats.toMap();
        
        // Get top strategies
        report.topStrategies = confidenceScorer.getTopStrategies(5);
        
        // Calculate model quality metrics
        report.modelQuality = calculateModelQuality();
        report.predictionAccuracy = calculatePredictionAccuracy();
        
        report.generatedAt = System.currentTimeMillis();
        
        return report;
    }
    
    /**
     * Calculate model quality score [0, 1]
     */
    private double calculateModelQuality() {
        TrainingStats stats = trainingStats;
        
        if (stats.totalTrainingEvents == 0) {
            return 0.0;
        }
        
        // Quality based on success rate and training volume
        double successRatio = (double) stats.successfulOutcomes / stats.totalTrainingEvents;
        double volumePenalty = Math.min(1.0, (double) stats.totalTrainingEvents / 100);
        
        return (0.7 * successRatio) + (0.3 * volumePenalty);
    }
    
    /**
     * Calculate prediction accuracy based on recent decisions
     */
    private double calculatePredictionAccuracy() {
        List<AgentDecisionLogger.AgentDecision> recentDecisions = decisionLogger.getAllDecisions().stream()
            .sorted((a, b) -> Long.compare(Long.parseLong(b.timestamp), 
                                          Long.parseLong(a.timestamp)))
            .limit(20)
            .toList();
        
        if (recentDecisions.isEmpty()) {
            return 0.5;
        }
        
        // Check if predicted vs actual outcomes match
        int correct = 0;
        for (AgentDecisionLogger.AgentDecision decision : recentDecisions) {
            double predictedProb = confidenceScorer.getModelStats().globalSuccessRate;
            boolean wasSuccessful = "SUCCESS".equals(decision.outcome);
            
            boolean prediction = predictedProb > 0.5;
            if (prediction == wasSuccessful) {
                correct++;
            }
        }
        
        return (double) correct / recentDecisions.size();
    }
    
    /**
     * Start background training thread
     */
    private void startAsyncTraining() {
        trainingExecutor.submit(() -> {
            while (running) {
                try {
                    TrainingJob job = trainingQueue.poll(1, TimeUnit.SECONDS);
                    if (job != null) {
                        trainImmediately(
                            job.strategyType,
                            job.errorType,
                            job.wasSuccessful,
                            job.confidence,
                            job.executionTimeMs
                        );
                        trainingStats.queuedJobsProcessed++;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
    
    /**
     * Shutdown trainer gracefully
     */
    public void shutdown() {
        running = false;
        trainingExecutor.shutdown();
        try {
            if (!trainingExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                trainingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            trainingExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // Inner classes for results and metrics
    
    public static class TrainingJob {
        public String strategyType;
        public String errorType;
        public boolean wasSuccessful;
        public float confidence;
        public long executionTimeMs;
        public long queuedAt;
        
        public TrainingJob(String strategyType, String errorType, boolean wasSuccessful,
                          float confidence, long executionTimeMs) {
            this.strategyType = strategyType;
            this.errorType = errorType;
            this.wasSuccessful = wasSuccessful;
            this.confidence = confidence;
            this.executionTimeMs = executionTimeMs;
            this.queuedAt = System.currentTimeMillis();
        }
    }
    
    public static class TrainingData {
        public String strategyType;
        public String errorType;
        public boolean wasSuccessful;
        public float confidence;
        public long executionTimeMs;
    }
    
    public static class BatchTrainingResult {
        public int requestedCount;
        public int successCount;
        public int failureCount;
        public List<String> errors = new ArrayList<>();
        public long completionTime;
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("requestedCount", requestedCount);
            map.put("successCount", successCount);
            map.put("failureCount", failureCount);
            map.put("successRate", requestedCount > 0 ? (double) successCount / requestedCount : 0);
            map.put("completionTime", completionTime);
            if (!errors.isEmpty()) {
                map.put("errors", errors);
            }
            return map;
        }
    }
    
    public static class RetrainingResult {
        public long startTime;
        public long endTime;
        public long durationMs;
        public int totalDecisionsProcessed;
        public int successfulRetrains;
        public int failedRetrains;
        public List<String> errors = new ArrayList<>();
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("startTime", startTime);
            map.put("endTime", endTime);
            map.put("durationMs", durationMs);
            map.put("totalProcessed", totalDecisionsProcessed);
            map.put("successfulRetrains", successfulRetrains);
            map.put("failedRetrains", failedRetrains);
            map.put("successRate", totalDecisionsProcessed > 0 ? 
                (double) successfulRetrains / totalDecisionsProcessed : 0);
            if (!errors.isEmpty()) {
                map.put("errors", errors);
            }
            return map;
        }
    }
    
    public static class TrainingStats {
        public int totalTrainingEvents;
        public int successfulOutcomes;
        public int failedOutcomes;
        public int queuedJobsProcessed;
        public long lastTrainingTime;
        
        public TrainingStats() {}
        
        public TrainingStats(TrainingStats other) {
            this.totalTrainingEvents = other.totalTrainingEvents;
            this.successfulOutcomes = other.successfulOutcomes;
            this.failedOutcomes = other.failedOutcomes;
            this.queuedJobsProcessed = other.queuedJobsProcessed;
            this.lastTrainingTime = other.lastTrainingTime;
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("totalTrainingEvents", totalTrainingEvents);
            map.put("successfulOutcomes", successfulOutcomes);
            map.put("failedOutcomes", failedOutcomes);
            map.put("queuedJobsProcessed", queuedJobsProcessed);
            map.put("successRate", totalTrainingEvents > 0 ? 
                (double) successfulOutcomes / totalTrainingEvents : 0);
            map.put("lastTrainingTime", lastTrainingTime);
            return map;
        }
    }
    
    public static class ModelPerformanceReport {
        public TrainingStats trainingStats;
        public Map<String, Object> aggregateStats;
        public List<String> topStrategies;
        public double modelQuality;
        public double predictionAccuracy;
        public long generatedAt;
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("trainingStats", trainingStats.toMap());
            map.put("aggregateStats", aggregateStats);
            map.put("topStrategies", topStrategies);
            map.put("modelQuality", modelQuality);
            map.put("predictionAccuracy", predictionAccuracy);
            map.put("generatedAt", generatedAt);
            return map;
        }
    }
}
