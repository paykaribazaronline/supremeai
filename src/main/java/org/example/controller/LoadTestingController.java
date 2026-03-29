package org.example.controller;

import org.example.testing.LoadTestingSuite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Phase 4.2: Load Testing Controller
 * REST API for running load tests and analyzing results
 */
@RestController
@RequestMapping("/api/testing/load")
public class LoadTestingController {

    @Autowired(required = false)
    private LoadTestingSuite loadTestingSuite;

    /**
     * POST /api/testing/load/throughput-test
     * Test endpoint throughput
     * Body: {endpoint: "/api/...", numRequests: 1000, concurrency: 10}
     */
    @PostMapping("/throughput-test")
    public ResponseEntity<?> runThroughputTest(@RequestBody Map<String, Object> request) {
        if (loadTestingSuite == null) {
            return ResponseEntity.ok(Map.of("message", "LoadTestingSuite not available"));
        }

        String endpoint = (String) request.get("endpoint");
        int numRequests = ((Number) request.get("numRequests")).intValue();
        int concurrency = ((Number) request.getOrDefault("concurrency", 5)).intValue();

        var result = loadTestingSuite.testEndpointThroughput(endpoint, numRequests, concurrency);

        return ResponseEntity.ok(loadTestingSuite.getTestResult(result.testName));
    }

    /**
     * POST /api/testing/load/sustained-load-test
     * Test sustained load
     * Body: {endpoint: "/api/...", requestsPerSecond: 100, durationSeconds: 60}
     */
    @PostMapping("/sustained-load-test")
    public ResponseEntity<?> runSustainedLoadTest(@RequestBody Map<String, Object> request) {
        if (loadTestingSuite == null) {
            return ResponseEntity.ok(Map.of("message", "LoadTestingSuite not available"));
        }

        String endpoint = (String) request.get("endpoint");
        int requestsPerSecond = ((Number) request.get("requestsPerSecond")).intValue();
        int durationSeconds = ((Number) request.get("durationSeconds")).intValue();

        var result = loadTestingSuite.testSustainedLoad(endpoint, requestsPerSecond, durationSeconds);

        return ResponseEntity.ok(loadTestingSuite.getTestResult(result.testName));
    }

    /**
     * POST /api/testing/load/spike-test
     * Test spike load and recovery
     * Body: {endpoint: "/api/...", normalLoad: 50, spikeLoad: 500, spikeDurationSeconds: 30}
     */
    @PostMapping("/spike-test")
    public ResponseEntity<?> runSpikeTest(@RequestBody Map<String, Object> request) {
        if (loadTestingSuite == null) {
            return ResponseEntity.ok(Map.of("message", "LoadTestingSuite not available"));
        }

        String endpoint = (String) request.get("endpoint");
        int normalLoad = ((Number) request.get("normalLoad")).intValue();
        int spikeLoad = ((Number) request.get("spikeLoad")).intValue();
        int spikeDurationSeconds = ((Number) request.get("spikeDurationSeconds")).intValue();

        var result = loadTestingSuite.testSpikeLoad(endpoint, normalLoad, spikeLoad, spikeDurationSeconds);

        return ResponseEntity.ok(loadTestingSuite.getTestResult(result.testName));
    }

    /**
     * POST /api/testing/load/websocket-stress-test
     * Test WebSocket connections
     * Body: {numConnections: 1000}
     */
    @PostMapping("/websocket-stress-test")
    public ResponseEntity<?> runWebSocketStressTest(@RequestBody Map<String, Object> request) {
        if (loadTestingSuite == null) {
            return ResponseEntity.ok(Map.of("message", "LoadTestingSuite not available"));
        }

        int numConnections = ((Number) request.get("numConnections")).intValue();

        var result = loadTestingSuite.testWebSocketConnections(numConnections);

        return ResponseEntity.ok(loadTestingSuite.getTestResult(result.testName));
    }

    /**
     * GET /api/testing/load/results
     * Get all test results
     */
    @GetMapping("/results")
    public ResponseEntity<?> getAllResults() {
        if (loadTestingSuite == null) {
            return ResponseEntity.ok(Map.of("message", "LoadTestingSuite not available"));
        }
        return ResponseEntity.ok(Map.of(
            "results", loadTestingSuite.getAllTestResults(),
            "count", loadTestingSuite.getAllTestResults().size()
        ));
    }

    /**
     * GET /api/testing/load/results/{testName}
     * Get specific test result
     */
    @GetMapping("/results/{testName}")
    public ResponseEntity<?> getTestResult(@PathVariable String testName) {
        if (loadTestingSuite == null) {
            return ResponseEntity.ok(Map.of("message", "LoadTestingSuite not available"));
        }
        var result = loadTestingSuite.getTestResult(testName);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    /**
     * DELETE /api/testing/load/results
     * Clear all test results
     */
    @DeleteMapping("/results")
    public ResponseEntity<?> clearResults() {
        if (loadTestingSuite == null) {
            return ResponseEntity.ok(Map.of("message", "LoadTestingSuite not available"));
        }
        loadTestingSuite.clearResults();
        return ResponseEntity.ok(Map.of("message", "All test results cleared"));
    }

    /**
     * POST /api/testing/load/quick-test
     * Quick benchmark test for monitoring system
     */
    @PostMapping("/quick-test")
    public ResponseEntity<?> runQuickTest() {
        if (loadTestingSuite == null) {
            return ResponseEntity.ok(Map.of("message", "LoadTestingSuite not available"));
        }

        var result = loadTestingSuite.testEndpointThroughput("/api/metrics/health", 100, 5);

        return ResponseEntity.ok(Map.of(
            "testName", "Quick Metrics Test",
            "result", loadTestingSuite.getTestResult(result.testName),
            "interpretation", result.errorRate < 1 ? "✓ PASSED" : "✗ FAILED"
        ));
    }
}
