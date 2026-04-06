package org.example.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.example.service.ExecutionLogManager;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * ExecutionLogController: REST API for execution metrics and analytics
 * 
 * Provides endpoints for:
 * - Project-level performance metrics
 * - System-wide execution statistics
 * - Daily and historical trends
 * - Log management and export
 */
@RestController
@RequestMapping("/api/execution-logs")
@CrossOrigin(origins = "*")
public class ExecutionLogController {

    @Autowired
    private ExecutionLogManager logManager;

    /**
     * Default constructor for Spring injection
     */
    public ExecutionLogController() {
    }

    /**
     * Constructor for dependency injection (especially for testing)
     */
    public ExecutionLogController(ExecutionLogManager logManager) {
        this.logManager = logManager;
    }

    /**
     * Get metrics for a specific project
     * GET /api/execution-logs/project/{projectId}
     */
    @GetMapping("/project/{projectId}")
    public Map<String, Object> getProjectMetrics(@PathVariable String projectId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> metrics = logManager.getProjectMetrics(projectId);
            response.put("success", true);
            response.put("data", metrics);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Get overall system metrics
     * GET /api/execution-logs/system
     */
    @GetMapping("/system")
    public Map<String, Object> getSystemMetrics() {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> metrics = logManager.getSystemMetrics();
            response.put("success", true);
            response.put("data", metrics);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Get daily metrics breakdown
     * GET /api/execution-logs/daily/{date}
     */
    @GetMapping("/daily/{date}")
    public Map<String, Object> getDailyMetrics(@PathVariable String date) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> metrics = logManager.getDailyMetrics(date);
            response.put("success", true);
            response.put("data", metrics);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Get performance trends over N days
     * GET /api/execution-logs/trends/{days}
     */
    @GetMapping("/trends/{days}")
    public Map<String, Object> getPerformanceTrends(@PathVariable int days) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> trends = logManager.getPerformanceTrends(days);
            response.put("success", true);
            response.put("data", trends);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Clear logs older than specified days
     * POST /api/execution-logs/cleanup/{daysToKeep}
     */
    @PostMapping("/cleanup/{daysToKeep}")
    public Map<String, Object> cleanupOldLogs(@PathVariable int daysToKeep) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> result = logManager.clearOldLogs(daysToKeep);
            response.put("success", true);
            response.put("data", result);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Export execution logs to CSV
     * GET /api/execution-logs/export
     */
    @GetMapping("/export")
    public Map<String, Object> exportLogs(@RequestParam(defaultValue = "./execution_logs_export.csv") String outputPath) {
        Map<String, Object> response = new HashMap<>();
        try {
            logManager.exportLogsToCSV(outputPath);
            response.put("success", true);
            response.put("message", "Logs exported successfully");
            response.put("outputPath", outputPath);
        } catch (IOException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Health check
     * GET /api/execution-logs/health
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "ExecutionLogManager");
        response.put("version", "1.0");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}
