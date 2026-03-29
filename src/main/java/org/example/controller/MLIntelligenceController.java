package org.example.controller;

import org.example.service.MLIntelligenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Phase 5: ML Intelligence Controller
 * REST API for anomaly detection, predictions, and auto-scaling suggestions
 */
@RestController
@RequestMapping("/api/intelligence/ml")
public class MLIntelligenceController {

    @Autowired(required = false)
    private MLIntelligenceService mlService;

    /**
     * POST /api/intelligence/ml/detect-anomalies
     * Detect anomalies in metric stream
     */
    @PostMapping("/detect-anomalies")
    public ResponseEntity<?> detectAnomalies(@RequestBody Map<String, Object> request) {
        if (mlService == null) {
            return ResponseEntity.ok(Map.of("message", "ML service not available"));
        }

        String metricName = (String) request.get("metric");
        @SuppressWarnings("unchecked")
        List<Double> values = (List<Double>) request.get("values");

        return ResponseEntity.ok(mlService.detectAnomalies(metricName, values));
    }

    /**
     * POST /api/intelligence/ml/predict-failure
     * Predict failure risk based on historical patterns
     */
    @PostMapping("/predict-failure")
    public ResponseEntity<?> predictFailure(@RequestBody Map<String, Object> request) {
        if (mlService == null) {
            return ResponseEntity.ok(Map.of("message", "ML service not available"));
        }

        String framework = (String) request.get("framework");
        @SuppressWarnings("unchecked")
        List<Double> successRates = (List<Double>) request.get("successRates");

        return ResponseEntity.ok(mlService.predictFailure(framework, successRates));
    }

    /**
     * POST /api/intelligence/ml/autoscale-suggestions
     * Get auto-scaling recommendations
     */
    @PostMapping("/autoscale-suggestions")
    public ResponseEntity<?> getAutoScalingSuggestions(@RequestBody Map<String, Object> request) {
        if (mlService == null) {
            return ResponseEntity.ok(Map.of("message", "ML service not available"));
        }

        double avgMemory = ((Number) request.get("avgMemory")).doubleValue();
        double peakMemory = ((Number) request.get("peakMemory")).doubleValue();
        double avgCpu = ((Number) request.get("avgCpu")).doubleValue();
        double avgLatency = ((Number) request.get("avgLatency")).doubleValue();

        return ResponseEntity.ok(mlService.suggestAutoScaling(avgMemory, peakMemory, avgCpu, avgLatency));
    }

    /**
     * POST /api/intelligence/ml/recommend-provider
     * Get ML-based provider recommendation
     */
    @PostMapping("/recommend-provider")
    public ResponseEntity<?> recommendProvider(@RequestBody Map<String, Object> request) {
        if (mlService == null) {
            return ResponseEntity.ok(Map.of("message", "ML service not available"));
        }

        String taskType = (String) request.get("taskType");
        @SuppressWarnings("unchecked")
        Map<String, Double> providerScores = (Map<String, Double>) request.get("providerScores");

        return ResponseEntity.ok(mlService.recommendProvider(taskType, providerScores));
    }

    /**
     * GET /api/intelligence/ml/anomaly-summary
     * Get anomaly detection summary
     */
    @GetMapping("/anomaly-summary")
    public ResponseEntity<?> getAnomalySummary() {
        if (mlService == null) {
            return ResponseEntity.ok(Map.of("message", "ML service not available"));
        }

        return ResponseEntity.ok(mlService.getAnomalySummary());
    }
}
