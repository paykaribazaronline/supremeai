package org.example.service;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * ExecutionLogManager: Comprehensive execution tracking and performance metrics
 * 
 * Tracks:
 * - All code generation executions
 * - Agent performance and selection metrics
 * - Validation results and fixes applied
 * - Performance metrics (response time, resource usage)
 * - Error patterns and recovery strategies
 * - Daily/weekly/monthly analytics
 */
@Service
public class ExecutionLogManager {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String LOG_DIR = "./execution_logs";

    public ExecutionLogManager() {
        try {
            Files.createDirectories(Paths.get(LOG_DIR));
        } catch (IOException e) {
            System.err.println("Failed to create log directory: " + e.getMessage());
        }
    }

    // Execution event record
    public static class ExecutionEvent {
        public String eventId;
        public String timestamp;
        public String eventType; // GENERATION, VALIDATION, ERROR_FIX, AGENT_SELECTION
        public String projectId;
        public String componentName;
        public String framework;
        public long durationMs;
        public String status; // SUCCESS, FAILED, PARTIAL
        public String agent;
        public double score;
        public Map<String, Object> metadata;

        public ExecutionEvent(String eventType, String projectId, String componentName, String framework) {
            this.eventId = UUID.randomUUID().toString();
            this.timestamp = LocalDateTime.now().format(formatter);
            this.eventType = eventType;
            this.projectId = projectId;
            this.componentName = componentName;
            this.framework = framework;
            this.metadata = new HashMap<>();
        }
    }

    /**
     * Log a code generation execution
     */
    public void logGeneration(String projectId, String componentName, String framework, 
                             long durationMs, boolean success, String selectedAgent, double validationScore) {
        ExecutionEvent event = new ExecutionEvent("GENERATION", projectId, componentName, framework);
        event.durationMs = durationMs;
        event.status = success ? "SUCCESS" : "FAILED";
        event.agent = selectedAgent;
        event.score = validationScore;
        logEvent(event);
    }

    /**
     * Log a validation execution
     */
    public void logValidation(String projectId, String framework, boolean isValid, 
                             int totalIssues, double validationScore) {
        ExecutionEvent event = new ExecutionEvent("VALIDATION", projectId, "", framework);
        event.durationMs = 0;
        event.status = isValid ? "SUCCESS" : "PARTIAL";
        event.score = validationScore;
        event.metadata.put("totalIssues", totalIssues);
        logEvent(event);
    }

    /**
     * Log an error fixing execution
     */
    public void logErrorFix(String projectId, int totalIssues, int autoFixable, int appliedFixes) {
        ExecutionEvent event = new ExecutionEvent("ERROR_FIX", projectId, "", "");
        event.status = appliedFixes > 0 ? "SUCCESS" : "FAILED";
        event.metadata.put("totalIssues", totalIssues);
        event.metadata.put("autoFixable", autoFixable);
        event.metadata.put("appliedFixes", appliedFixes);
        event.score = (double) appliedFixes / totalIssues * 100;
        logEvent(event);
    }

    /**
     * Log agent selection and ranking
     */
    public void logAgentSelection(String taskType, String selectedAgent, List<String> fallbackChain, 
                                 Map<String, Double> scores) {
        ExecutionEvent event = new ExecutionEvent("AGENT_SELECTION", "", "", "");
        event.agent = selectedAgent;
        event.metadata.put("taskType", taskType);
        event.metadata.put("fallbackChain", fallbackChain);
        event.metadata.put("scores", scores);
        logEvent(event);
    }

    /**
     * Log custom execution event
     */
    public void logEvent(ExecutionEvent event) {
        try {
            String logFile = LOG_DIR + "/" + event.eventType.toLowerCase() + "_" + 
                           LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".json";
            
            List<Map<String, Object>> events = new ArrayList<>();
            if (Files.exists(Paths.get(logFile))) {
                String content = new String(Files.readAllBytes(Paths.get(logFile)));
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> existing = mapper.readValue(content, List.class);
                events.addAll(existing);
            }

            Map<String, Object> eventMap = mapper.convertValue(event, Map.class);
            events.add(eventMap);
            
            Files.write(Paths.get(logFile), mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsBytes(events));
        } catch (IOException e) {
            System.err.println("Failed to log event: " + e.getMessage());
        }
    }

    /**
     * Get performance metrics for a project
     */
    public Map<String, Object> getProjectMetrics(String projectId) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("projectId", projectId);
        
        int totalGenerations = 0;
        int successfulGenerations = 0;
        long totalDuration = 0;
        double avgValidationScore = 0.0;
        List<String> agentsUsed = new ArrayList<>();

        try {
            File logDir = new File(LOG_DIR);
            if (logDir.exists()) {
                File[] files = logDir.listFiles();
                if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".json")) {
                        String content = new String(Files.readAllBytes(file.toPath()));
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> events = mapper.readValue(content, List.class);
                        
                        for (Map<String, Object> event : events) {
                            if (projectId.equals(event.get("projectId"))) {
                                totalGenerations++;
                                if ("SUCCESS".equals(event.get("status"))) {
                                    successfulGenerations++;
                                }
                                if (event.containsKey("durationMs")) {
                                    totalDuration += ((Number) event.get("durationMs")).longValue();
                                }
                                if (event.containsKey("score")) {
                                    avgValidationScore += ((Number) event.get("score")).doubleValue();
                                }
                                if (event.containsKey("agent") && event.get("agent") != null) {
                                    agentsUsed.add((String) event.get("agent"));
                                }
                            }
                        }
                    }
                } // end for file
                } // end if files != null
            } // end if logDir.exists
        } catch (IOException e) {
            System.err.println("Failed to read metrics: " + e.getMessage());
        }

        metrics.put("totalGenerations", totalGenerations);
        metrics.put("successfulGenerations", successfulGenerations);
        metrics.put("successRate", totalGenerations > 0 ? (successfulGenerations * 100.0 / totalGenerations) : 0.0);
        metrics.put("avgDurationMs", totalGenerations > 0 ? (totalDuration / totalGenerations) : 0);
        metrics.put("avgValidationScore", totalGenerations > 0 ? (avgValidationScore / totalGenerations) : 0.0);
        
        // Agent usage stats
        Map<String, Long> agentCounts = agentsUsed.stream()
            .collect(Collectors.groupingBy(a -> a, Collectors.counting()));
        metrics.put("agentUsage", agentCounts);
        metrics.put("mostUsedAgent", agentCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("NONE"));
        
        metrics.put("timestamp", LocalDateTime.now().format(formatter));
        return metrics;
    }

    /**
     * Get overall system metrics
     */
    public Map<String, Object> getSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        int totalEvents = 0;
        int successCount = 0;
        int failureCount = 0;
        long totalDuration = 0;
        double avgScore = 0.0;
        Set<String> uniqueProjects = new HashSet<>();
        Map<String, Long> eventTypeDistribution = new HashMap<>();

        try {
            File logDir = new File(LOG_DIR);
            if (logDir.exists()) {
                File[] files = logDir.listFiles();
                if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".json")) {
                        String content = new String(Files.readAllBytes(file.toPath()));
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> events = mapper.readValue(content, List.class);
                        
                        for (Map<String, Object> event : events) {
                            totalEvents++;
                            
                            String status = (String) event.get("status");
                            if ("SUCCESS".equals(status)) {
                                successCount++;
                            } else if ("FAILED".equals(status)) {
                                failureCount++;
                            }
                            
                            if (event.containsKey("durationMs")) {
                                totalDuration += ((Number) event.get("durationMs")).longValue();
                            }
                            if (event.containsKey("score")) {
                                avgScore += ((Number) event.get("score")).doubleValue();
                            }
                            
                            String projectId = (String) event.get("projectId");
                            if (projectId != null && !projectId.isEmpty()) {
                                uniqueProjects.add(projectId);
                            }
                            
                            String eventType = (String) event.get("eventType");
                            if (eventType != null) {
                                eventTypeDistribution.merge(eventType, 1L, Long::sum);
                            }
                        }
                    }
                } // end for file
                } // end if files != null
            } // end if logDir.exists
        } catch (IOException e) {
            System.err.println("Failed to read system metrics: " + e.getMessage());
        }

        metrics.put("totalEvents", totalEvents);
        metrics.put("successCount", successCount);
        metrics.put("failureCount", failureCount);
        metrics.put("successRate", totalEvents > 0 ? (successCount * 100.0 / totalEvents) : 0.0);
        metrics.put("avgDurationMs", totalEvents > 0 ? (totalDuration / totalEvents) : 0);
        metrics.put("avgValidationScore", totalEvents > 0 ? (avgScore / totalEvents) : 0.0);
        metrics.put("uniqueProjects", uniqueProjects.size());
        metrics.put("eventTypeDistribution", eventTypeDistribution);
        metrics.put("timestamp", LocalDateTime.now().format(formatter));
        
        return metrics;
    }

    /**
     * Get daily metrics breakdown
     */
    public Map<String, Object> getDailyMetrics(String date) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("date", date);
        
        int eventCount = 0;
        int successCount = 0;
        double avgScore = 0.0;

        try {
            File logDir = new File(LOG_DIR);
            if (logDir.exists()) {
                for (File file : logDir.listFiles()) {
                    if (file.getName().contains(date) && file.getName().endsWith(".json")) {
                        String content = new String(Files.readAllBytes(file.toPath()));
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> events = mapper.readValue(content, List.class);
                        
                        eventCount = events.size();
                        successCount = (int) events.stream()
                            .filter(e -> "SUCCESS".equals(e.get("status")))
                            .count();
                        
                        avgScore = events.stream()
                            .filter(e -> e.containsKey("score"))
                            .mapToDouble(e -> ((Number) e.get("score")).doubleValue())
                            .average()
                            .orElse(0.0);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to read daily metrics: " + e.getMessage());
        }

        metrics.put("eventCount", eventCount);
        metrics.put("successCount", successCount);
        metrics.put("successRate", eventCount > 0 ? (successCount * 100.0 / eventCount) : 0.0);
        metrics.put("avgValidationScore", avgScore);
        metrics.put("timestamp", LocalDateTime.now().format(formatter));
        
        return metrics;
    }

    /**
     * Get performance trends (last N days)
     */
    public Map<String, Object> getPerformanceTrends(int days) {
        Map<String, Object> trends = new HashMap<>();
        List<Map<String, Object>> dailyData = new ArrayList<>();
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        
        for (int i = 0; i < days; i++) {
            LocalDateTime date = startDate.plusDays(i);
            String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Map<String, Object> dailyMetrics = getDailyMetrics(dateStr);
            dailyData.add(dailyMetrics);
        }

        trends.put("days", days);
        trends.put("dailyBreakdown", dailyData);
        trends.put("timestamp", LocalDateTime.now().format(formatter));
        
        return trends;
    }

    /**
     * Clear old logs (older than specified days)
     */
    public Map<String, Object> clearOldLogs(int daysToKeep) {
        Map<String, Object> result = new HashMap<>();
        int deletedFiles = 0;

        try {
            File logDir = new File(LOG_DIR);
            if (logDir.exists()) {
                LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
                for (File file : logDir.listFiles()) {
                    if (file.isFile() && file.getName().endsWith(".json")) {
                        long lastModified = file.lastModified();
                        LocalDateTime fileDate = LocalDateTime.now();
                        if (fileDate.isAfter(cutoffDate)) {
                            if (file.delete()) {
                                deletedFiles++;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }

        result.put("deletedFiles", deletedFiles);
        result.put("daysToKeep", daysToKeep);
        result.put("timestamp", LocalDateTime.now().format(formatter));
        return result;
    }

    /**
     * Export logs to CSV format
     */
    public void exportLogsToCSV(String outputPath) throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("Timestamp,Event Type,Project ID,Component,Framework,Status,Duration(ms),Agent,Score\n");

        File logDir = new File(LOG_DIR);
        if (logDir.exists()) {
            for (File file : logDir.listFiles()) {
                if (file.getName().endsWith(".json")) {
                    String content = new String(Files.readAllBytes(file.toPath()));
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> events = mapper.readValue(content, List.class);
                    
                    for (Map<String, Object> event : events) {
                        csv.append(event.getOrDefault("timestamp", "")).append(",");
                        csv.append(event.getOrDefault("eventType", "")).append(",");
                        csv.append(event.getOrDefault("projectId", "")).append(",");
                        csv.append(event.getOrDefault("componentName", "")).append(",");
                        csv.append(event.getOrDefault("framework", "")).append(",");
                        csv.append(event.getOrDefault("status", "")).append(",");
                        csv.append(event.getOrDefault("durationMs", "")).append(",");
                        csv.append(event.getOrDefault("agent", "")).append(",");
                        csv.append(event.getOrDefault("score", "")).append("\n");
                    }
                }
            }
        }

        Files.write(Paths.get(outputPath), csv.toString().getBytes());
    }
}
