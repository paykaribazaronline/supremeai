package org.supremeai.selfhealing.adaptive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AdaptiveThresholdEngine: The Brain
 * 
 * Uses machine learning to analyze failure patterns and automatically tune
 * circuit breaker thresholds, timeouts, and other configuration parameters.
 * Learns from history to predict and prevent failures before they occur.
 * 
 * Adaptation Cycle (Every hour):
 *   1. Analyze historical failures
 *   2. Identify patterns (time-of-day, external dependencies, load)
 *   3. Predict future failures
 *   4. Auto-tune thresholds for optimal balance
 *   5. Preemptively activate failover if failure predicted
 */
@Component
public class AdaptiveThresholdEngine {
    
    private static final Logger log = LoggerFactory.getLogger(AdaptiveThresholdEngine.class);
    
    @Autowired
    private FailureAnalyticsService failureAnalytics;
    
    @Autowired
    private CircuitBreakerConfigService circuitBreakerConfig;
    
    @Autowired
    private MLAnomalyDetectionService mlModel;
    
    @Autowired
    private SelfHealingService healingService;
    
    public class FailurePattern {
        public String provider;
        public String timeWindow;           // "morning", "afternoon", "night"
        public double failureRate;
        public long averageRecoveryMs;
        public List<Long> responseTimes;    // Historical response times
        public int occurrenceCount;
        public long lastOccurrence;
    }
    
    public class FailurePrediction {
        public String provider;
        public double probability;         // 0.0-1.0
        public long predictedTimestamp;
        public String reason;
        public String recommendedAction;
    }
    
    public class AdaptiveConfig {
        public String provider;
        public int circuitBreakerFailureThreshold;  // Default: 5
        public int circuitBreakerTimeoutSeconds;    // Default: 30
        public int httpTimeoutSeconds;              // Default: 10
        public int retryAttempts;                   // Default: 3
        public long maxWaitBetweenRetriesMs;        // Default: 5000
    }
    
    /**
     * Main adaptation cycle: runs every hour
     */
    @Scheduled(fixedRate = 3600000)  // Every 1 hour
    public void analyzeAndAdapt() {
        long startTime = System.currentTimeMillis();
        
        log.info("🧠 ADAPTIVE ENGINE: Starting analysis cycle");
        
        try {
            // Step 1: Analyze historical failures
            Map<String, FailurePattern> patterns = analyzeHistoricalFailures();
            log.info("   Analyzed {} failure patterns", patterns.size());
            
            // Step 2: Predict future failures
            List<FailurePrediction> predictions = predictFutureFailures(patterns);
            log.info("   Generated {} failure predictions", predictions.size());
            
            // Step 3: Auto-tune thresholds
            Map<String, AdaptiveConfig> newConfigs = generateAdaptiveConfigs(patterns);
            applyAdaptiveConfigs(newConfigs);
            log.info("   Applied adaptive configurations for {} providers", newConfigs.size());
            
            // Step 4: Preemptive failover
            for (FailurePrediction prediction : predictions) {
                if (prediction.probability > 0.8) {
                    log.warn("   ⚠️  High-confidence failure predicted for {}: {}", 
                        prediction.provider, prediction.reason);
                    
                    if (prediction.recommendedAction.equals("PREEMPTIVE_FAILOVER")) {
                        log.info("   Activating preemptive failover for {}", prediction.provider);
                        healingService.preemptiveFailover(prediction.provider);
                    }
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ ADAPTIVE ENGINE: Cycle complete in {}ms", duration);
            
        } catch (Exception e) {
            log.error("❌ ADAPTIVE ENGINE: Analysis failed - {}", e.getMessage(), e);
        }
    }
    
    /**
     * Analyze failure patterns from historical data
     */
    private Map<String, FailurePattern> analyzeHistoricalFailures() {
        Map<String, FailurePattern> patterns = new HashMap<>();
        
        // Get last 7 days of failure data
        List<FailureEvent> events = failureAnalytics.getFailureEventsLast(7, "days");
        
        // Group by provider
        Map<String, List<FailureEvent>> byProvider = events.stream()
            .collect(Collectors.groupingBy(FailureEvent::getProvider));
        
        for (Map.Entry<String, List<FailureEvent>> entry : byProvider.entrySet()) {
            String provider = entry.getKey();
            List<FailureEvent> providerEvents = entry.getValue();
            
            FailurePattern pattern = new FailurePattern();
            pattern.provider = provider;
            pattern.occurrenceCount = providerEvents.size();
            pattern.failureRate = calculateFailureRate(providerEvents);
            pattern.averageRecoveryMs = calculateAverageRecovery(providerEvents);
            pattern.responseTimes = extractResponseTimes(providerEvents);
            pattern.lastOccurrence = providerEvents.stream()
                .mapToLong(FailureEvent::getTimestamp)
                .max()
                .orElse(0);
            
            // Analyze time-of-day pattern
            pattern.timeWindow = analyzeTimeOfDay(providerEvents);
            
            patterns.put(provider, pattern);
            
            log.info("   Pattern {}: {}% failure, avg recovery {}ms, peak: {}",
                provider, 
                String.format("%.1f", pattern.failureRate * 100),
                pattern.averageRecoveryMs,
                pattern.timeWindow
            );
        }
        
        return patterns;
    }
    
    /**
     * Predict future failures using ML anomaly detection
     */
    private List<FailurePrediction> predictFutureFailures(Map<String, FailurePattern> patterns) {
        List<FailurePrediction> predictions = new ArrayList<>();
        
        for (Map.Entry<String, FailurePattern> entry : patterns.entrySet()) {
            String provider = entry.getKey();
            FailurePattern pattern = entry.getValue();
            
            // Use ML model to predict failure probability
            double failureProbability = mlModel.predictFailureProbability(
                pattern.failureRate,
                pattern.responseTimes,
                pattern.timeWindow
            );
            
            if (failureProbability > 0.6) {
                FailurePrediction prediction = new FailurePrediction();
                prediction.provider = provider;
                prediction.probability = failureProbability;
                prediction.predictedTimestamp = System.currentTimeMillis() + (5 * 60 * 1000); // 5 min
                prediction.reason = String.format(
                    "Pattern: {}% failure rate, peak hours, {} recoveries in 7d",
                    String.format("%.1f", pattern.failureRate * 100),
                    pattern.occurrenceCount
                );
                prediction.recommendedAction = failureProbability > 0.8 
                    ? "PREEMPTIVE_FAILOVER" 
                    : "MONITOR";
                
                predictions.add(prediction);
                
                log.info("   🔮 Prediction: {} failure {} likely ({} confidence)",
                    provider,
                    failureProbability > 0.8 ? "HIGHLY" : "MODERATELY",
                    String.format("%.1f%%", failureProbability * 100)
                );
            }
        }
        
        return predictions;
    }
    
    /**
     * Generate adaptive configuration based on patterns
     */
    private Map<String, AdaptiveConfig> generateAdaptiveConfigs(Map<String, FailurePattern> patterns) {
        Map<String, AdaptiveConfig> configs = new HashMap<>();
        
        for (Map.Entry<String, FailurePattern> entry : patterns.entrySet()) {
            String provider = entry.getKey();
            FailurePattern pattern = entry.getValue();
            
            AdaptiveConfig config = new AdaptiveConfig();
            config.provider = provider;
            
            // Tune circuit breaker threshold based on failure rate
            if (pattern.failureRate > 0.3) {
                // High failure rate → more lenient (let more failures before opening)
                config.circuitBreakerFailureThreshold = 8;
                config.circuitBreakerTimeoutSeconds = 45;
            } else if (pattern.failureRate > 0.1) {
                // Moderate failure rate → standard
                config.circuitBreakerFailureThreshold = 5;
                config.circuitBreakerTimeoutSeconds = 30;
            } else {
                // Low failure rate → strict (open faster)
                config.circuitBreakerFailureThreshold = 3;
                config.circuitBreakerTimeoutSeconds = 20;
            }
            
            // Tune HTTP timeout based on response time percentiles
            int p95ResponseTime = percentile(pattern.responseTimes, 0.95);
            config.httpTimeoutSeconds = (int) Math.ceil(p95ResponseTime * 1.4 / 1000.0); // 40% safety margin
            config.httpTimeoutSeconds = Math.max(5, Math.min(60, config.httpTimeoutSeconds)); // Bound 5-60s
            
            // Retry strategy based on recovery time
            if (pattern.averageRecoveryMs > 10000) {
                // Slow recovery → fewer retries
                config.retryAttempts = 2;
                config.maxWaitBetweenRetriesMs = 2000;
            } else {
                // Fast recovery → more retries
                config.retryAttempts = 3;
                config.maxWaitBetweenRetriesMs = 5000;
            }
            
            configs.put(provider, config);
            
            log.info("   Config {}: CB-threshold={}, timeout={}s, retries={}",
                provider,
                config.circuitBreakerFailureThreshold,
                config.circuitBreakerTimeoutSeconds,
                config.retryAttempts
            );
        }
        
        return configs;
    }
    
    /**
     * Apply adaptive configurations
     */
    private void applyAdaptiveConfigs(Map<String, AdaptiveConfig> configs) {
        for (Map.Entry<String, AdaptiveConfig> entry : configs.entrySet()) {
            String provider = entry.getKey();
            AdaptiveConfig config = entry.getValue();
            
            try {
                circuitBreakerConfig.updateConfig(
                    provider,
                    config.circuitBreakerFailureThreshold,
                    config.circuitBreakerTimeoutSeconds,
                    config.httpTimeoutSeconds,
                    config.retryAttempts,
                    config.maxWaitBetweenRetriesMs
                );
            } catch (Exception e) {
                log.error("   Failed to apply config for {}: {}", provider, e.getMessage());
            }
        }
    }
    
    /**
     * Get current failure predictions
     */
    public List<FailurePrediction> getPredictions() {
        try {
            Map<String, FailurePattern> patterns = analyzeHistoricalFailures();
            return predictFutureFailures(patterns);
        } catch (Exception e) {
            log.error("Failed to get predictions: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
    
    // Helper methods
    private double calculateFailureRate(List<FailureEvent> events) {
        if (events.isEmpty()) return 0.0;
        long totalAttempts = failureAnalytics.getAttemptCountSame(events);
        return (double) events.size() / totalAttempts;
    }
    
    private long calculateAverageRecovery(List<FailureEvent> events) {
        if (events.isEmpty()) return 0;
        return events.stream()
            .mapToLong(FailureEvent::getRecoveryTimeMs)
            .average()
            .orElse(0);
    }
    
    private List<Long> extractResponseTimes(List<FailureEvent> events) {
        return events.stream()
            .map(FailureEvent::getResponseTimeMs)
            .collect(Collectors.toList());
    }
    
    private String analyzeTimeOfDay(List<FailureEvent> events) {
        Map<String, Long> byHour = events.stream()
            .collect(Collectors.groupingBy(
                e -> getHourOfDay(e.getTimestamp()),
                Collectors.counting()
            ));
        
        if (byHour.isEmpty()) return "UNKNOWN";
        
        String peakHour = byHour.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("UNKNOWN");
        
        return peakHour;
    }
    
    private String getHourOfDay(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        
        if (hour >= 6 && hour < 12) return "morning";
        if (hour >= 12 && hour < 17) return "afternoon";
        if (hour >= 17 && hour < 21) return "evening";
        return "night";
    }
    
    private int percentile(List<Long> values, double percentile) {
        if (values.isEmpty()) return 0;
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int index = (int) (sorted.size() * percentile);
        return sorted.get(Math.min(index, sorted.size() - 1)).intValue();
    }
    
    // Helper records
    private static class FailureEvent {
        private String provider;
        private long timestamp;
        private long recoveryTimeMs;
        private long responseTimeMs;
        
        public String getProvider() { return provider; }
        public long getTimestamp() { return timestamp; }
        public long getRecoveryTimeMs() { return recoveryTimeMs; }
        public long getResponseTimeMs() { return responseTimeMs; }
    }
}
