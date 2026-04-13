package org.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Phase 5: ML-Based Intelligence Service
 * Anomaly detection, predictions, and auto-scaling suggestions
 */
@Service
public class MLIntelligenceService {



    private static class AnomalyPoint {
        public long timestamp;
        public double value;
        public double zScore;
        public boolean isAnomaly;
        public String anomalyType;

        AnomalyPoint(double value, double mean, double stdDev) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
            this.zScore = stdDev > 0 ? (value - mean) / stdDev : 0;
            this.isAnomaly = Math.abs(zScore) > 2.5; // 3-sigma rule
            this.anomalyType = classifyAnomaly(zScore);
        }

        private String classifyAnomaly(double zScore) {
            if (!isAnomaly) return "NORMAL";
            if (Math.abs(zScore) > 3) return "CRITICAL_ANOMALY";
            return "MILD_ANOMALY";
        }
    }

    private final List<AnomalyPoint> anomalies = Collections.synchronizedList(new ArrayList<>());

    /**
     * Detect anomalies in metric stream (Z-score method)
     */
    public Map<String, Object> detectAnomalies(String metricName, List<Double> values) {
        // Calculate statistics
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0);
        double stdDev = Math.sqrt(variance);

        List<AnomalyPoint> detected = new ArrayList<>();
        for (double value : values) {
            AnomalyPoint point = new AnomalyPoint(value, mean, stdDev);
            if (point.isAnomaly) {
                detected.add(point);
                anomalies.add(point);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("metric", metricName);
        result.put("totalPoints", values.size());
        result.put("mean", String.format("%.2f", mean));
        result.put("stdDev", String.format("%.2f", stdDev));
        result.put("anomaliesDetected", detected.size());
        result.put("anomalyRate", String.format("%.1f%%", (detected.size() / (double) values.size()) * 100));
        result.put("anomalies", detected.stream()
                .map(a -> Map.of(
                    "value", a.value,
                    "zScore", String.format("%.2f", a.zScore),
                    "type", a.anomalyType
                ))
                .collect(Collectors.toList()));

        return result;
    }

    /**
     * Predict failure based on historical patterns
     */
    public Map<String, Object> predictFailure(String framework, List<Double> successRates) {
        if (successRates.isEmpty()) {
            return Map.of("framework", framework, "prediction", "Insufficient data");
        }

        // Simple linear regression prediction
        double[] x = new double[successRates.size()];
        double[] y = new double[successRates.size()];
        
        for (int i = 0; i < successRates.size(); i++) {
            x[i] = i;
            y[i] = successRates.get(i);
        }

        double[] regression = calculateLinearRegression(x, y);
        double slope = regression[0];
        double intercept = regression[1];

        // Predict next 10 steps
        List<Map<String, Object>> predictions = new ArrayList<>();
        double nextValue = y[y.length - 1];
        
        for (int i = 1; i <= 10; i++) {
            nextValue = slope * (successRates.size() + i) + intercept;
            predictions.add(Map.of(
                "step", i,
                "predictedSuccessRate", String.format("%.1f%%", Math.min(100, Math.max(0, nextValue)))
            ));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("framework", framework);
        result.put("trend", slope > 0.5 ? "IMPROVING" : slope < -0.5 ? "DEGRADING" : "STABLE");
        result.put("trendSlope", String.format("%.4f", slope));
        result.put("predictions", predictions);

        // Failure prediction
        if (nextValue < 80) {
            result.put("failureRisk", "HIGH");
            result.put("recommendation", "Monitor closely or consider provider change");
        } else if (nextValue < 90) {
            result.put("failureRisk", "MEDIUM");
            result.put("recommendation", "Watch for degradation patterns");
        } else {
            result.put("failureRisk", "LOW");
            result.put("recommendation", "System operating normally");
        }

        return result;
    }

    /**
     * Suggest auto-scaling configuration
     */
    public Map<String, Object> suggestAutoScaling(double avgMemory, double peakMemory, 
                                                    double avgCpu, double avgLatency) {
        Map<String, Object> suggestions = new HashMap<>();

        // Memory scaling
        if (peakMemory > avgMemory * 1.8) {
            suggestions.put("memory", Map.of(
                "action", "SCALE_UP",
                "reason", "Peak memory (" + String.format("%.0f", peakMemory) + "MB) is 80% above average",
                "recommendation", "Increase memory allocation by 30-50%"
            ));
        } else if (peakMemory < avgMemory * 0.5) {
            suggestions.put("memory", Map.of(
                "action", "SCALE_DOWN",
                "reason", "Memory utilization is well below peak",
                "recommendation", "Consider reducing memory allocation"
            ));
        } else {
            suggestions.put("memory", Map.of(
                "action", "NO_CHANGE",
                "reason", "Memory utilization is within normal range"
            ));
        }

        // CPU scaling
        if (avgCpu > 75) {
            suggestions.put("cpu", Map.of(
                "action", "SCALE_UP",
                "reason", "CPU usage (" + String.format("%.1f", avgCpu) + "%) is above 75%",
                "recommendation", "Increase CPU cores or optimize code"
            ));
        } else if (avgCpu < 25) {
            suggestions.put("cpu", Map.of(
                "action", "SCALE_DOWN",
                "reason", "CPU utilization is very low",
                "recommendation", "Consolidate workloads or reduce cores"
            ));
        } else {
            suggestions.put("cpu", Map.of(
                "action", "NO_CHANGE",
                "reason", "CPU utilization is optimal"
            ));
        }

        // Latency-based scaling
        if (avgLatency > 500) {
            suggestions.put("latency", Map.of(
                "action", "SCALE_UP",
                "reason", "Avg latency (" + String.format("%.0f", avgLatency) + "ms) indicates resource constraint",
                "recommendation", "Scale up instances or optimize queries"
            ));
        } else {
            suggestions.put("latency", Map.of(
                "action", "NO_CHANGE",
                "reason", "Latency is within acceptable range"
            ));
        }

        // Overall recommendation
        long scaleUpCount = suggestions.values().stream()
                .filter(v -> v instanceof Map && ((Map<?, ?>) v).containsKey("action") &&
                        "SCALE_UP".equals(((Map<?, ?>) v).get("action")))
                .count();

        if (scaleUpCount >= 2) {
            suggestions.put("overallAction", "SCALE_UP_AGGRESSIVE");
            suggestions.put("confidence", "HIGH");
        } else if (scaleUpCount == 1) {
            suggestions.put("overallAction", "SCALE_UP");
            suggestions.put("confidence", "MEDIUM");
        } else {
            suggestions.put("overallAction", "MAINTAIN");
            suggestions.put("confidence", "HIGH");
        }

        return suggestions;
    }

    /**
     * Recommend best provider based on ML model
     */
    public Map<String, Object> recommendProvider(String taskType, Map<String, Double> providerScores) {
        Map<String, Object> recommendation = new HashMap<>();

        // Sort by score
        var sorted = providerScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());

        if (sorted.isEmpty()) {
            return Map.of("recommendation", "No providers available");
        }

        String bestProvider = sorted.get(0).getKey();
        double bestScore = sorted.get(0).getValue();

        recommendation.put("taskType", taskType);
        recommendation.put("recommendedProvider", bestProvider);
        recommendation.put("score", String.format("%.2f", bestScore));

        // Confidence calculation
        double confidence;
        if (sorted.size() > 1) {
            double secondScore = sorted.get(1).getValue();
            confidence = ((bestScore - secondScore) / bestScore) * 100;
        } else {
            confidence = bestScore * 100;
        }

        recommendation.put("confidence", String.format("%.1f%%", Math.min(100, confidence)));
        recommendation.put("explanation", generateRecommendationExplanation(bestProvider, bestScore));

        // Alternative providers
        recommendation.put("alternatives", sorted.stream()
                .skip(1)
                .limit(3)
                .map(e -> Map.of(
                    "provider", e.getKey(),
                    "score", String.format("%.2f", e.getValue())
                ))
                .collect(Collectors.toList()));

        return recommendation;
    }

    /**
     * Get anomaly summary
     */
    public Map<String, Object> getAnomalySummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalAnomalies", anomalies.size());

        long criticalCount = anomalies.stream()
                .filter(a -> "CRITICAL_ANOMALY".equals(a.anomalyType))
                .count();
        long mildCount = anomalies.stream()
                .filter(a -> "MILD_ANOMALY".equals(a.anomalyType))
                .count();

        summary.put("criticalAnomalies", criticalCount);
        summary.put("mildAnomalies", mildCount);

        // Recent anomalies
        summary.put("recentAnomalies", anomalies.stream()
                .sorted((a, b) -> Long.compare(b.timestamp, a.timestamp))
                .limit(10)
                .map(a -> Map.of(
                    "value", a.value,
                    "type", a.anomalyType,
                    "severity", Math.abs(a.zScore) > 3 ? "HIGH" : "MEDIUM"
                ))
                .collect(Collectors.toList()));

        return summary;
    }

    /**
     * Helper: Calculate linear regression
     */
    private double[] calculateLinearRegression(double[] x, double[] y) {
        int n = x.length;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            sumX += x[i];
            sumY += y[i];
            sumXY += x[i] * y[i];
            sumX2 += x[i] * x[i];
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        return new double[]{slope, intercept};
    }

    /**
     * Generate human-readable recommendation explanation
     */
    private String generateRecommendationExplanation(String provider, double score) {
        if (score > 0.9) {
            return provider + " has exceptional performance for this task";
        } else if (score > 0.75) {
            return provider + " is the best choice for this task";
        } else {
            return provider + " is recommended, but monitor performance";
        }
    }

    /**
     * Clear anomaly history
     */
    public void clearAnomalies() {
        anomalies.clear();
    }
}
