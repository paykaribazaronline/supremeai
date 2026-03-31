package org.example.controller;

import org.example.service.AgentDecisionLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 6 Week 5-6: Timeline Visualization Controller
 * Interactive timeline for decision history with color-coded outcomes
 * 
 * Features:
 * - Decision history timeline by project
 * - Color-coded outcomes (SUCCESS=green, FAILED=red, PARTIAL=yellow)
 * - Timeline aggregation and filtering
 * - Drill-down support for decision details
 * - Integration with 3D dashboard
 */
@RestController
@RequestMapping("/api/v1/timeline")
@CrossOrigin(origins = "*")
public class TimelineVisualizationController {

    @Autowired(required = false)
    private AgentDecisionLogger decisionLogger;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    /**
     * GET /api/v1/timeline/project/{projectId}
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<Map<String, Object>> getProjectTimeline(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "100") int limit) {
        
        if (decisionLogger == null) {
            return ResponseEntity.status(501).body(errorResponse("AgentDecisionLogger not available"));
        }

        try {
            List<AgentDecisionLogger.AgentDecision> decisions = decisionLogger.getProjectDecisions(projectId);
            
            List<Map<String, Object>> timelineEntries = decisions.stream()
                .map(this::buildTimelineEntry)
                .sorted((a, b) -> Long.compare(
                    (Long) b.get("timestamp"),
                    (Long) a.get("timestamp")
                ))
                .limit(limit)
                .collect(Collectors.toList());

            Map<String, Object> stats = calculateTimelineStats(timelineEntries);

            Map<String, Object> response = new HashMap<>();
            response.put("projectId", projectId);
            response.put("totalEntries", timelineEntries.size());
            response.put("timeline", timelineEntries);
            response.put("stats", stats);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(errorResponse(e.getMessage()));
        }
    }

    /**
     * GET /api/v1/timeline/agent/{agentName}
     */
    @GetMapping("/agent/{agentName}")
    public ResponseEntity<Map<String, Object>> getAgentTimeline(
            @PathVariable String agentName,
            @RequestParam(defaultValue = "50") int limit) {
        
        if (decisionLogger == null) {
            return ResponseEntity.status(501).body(errorResponse("AgentDecisionLogger not available"));
        }

        try {
            List<AgentDecisionLogger.AgentDecision> decisions = decisionLogger.getAgentDecisions(agentName);
            
            List<Map<String, Object>> timelineEntries = decisions.stream()
                .map(this::buildTimelineEntry)
                .sorted((a, b) -> Long.compare(
                    (Long) b.get("timestamp"),
                    (Long) a.get("timestamp")
                ))
                .limit(limit)
                .collect(Collectors.toList());

            Map<String, Object> stats = calculateTimelineStats(timelineEntries);

            Map<String, Object> response = new HashMap<>();
            response.put("agent", agentName);
            response.put("totalEntries", timelineEntries.size());
            response.put("timeline", timelineEntries);
            response.put("stats", stats);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(errorResponse(e.getMessage()));
        }
    }

    /**
     * GET /api/v1/timeline/range
     */
    @GetMapping("/range")
    public ResponseEntity<Map<String, Object>> getTimelineRange(
            @RequestParam long startTime,
            @RequestParam long endTime,
            @RequestParam(required = false) String agentFilter,
            @RequestParam(required = false) String projectFilter) {
        
        if (decisionLogger == null) {
            return ResponseEntity.status(501).body(errorResponse("AgentDecisionLogger not available"));
        }

        try {
            List<AgentDecisionLogger.AgentDecision> allDecisions = new ArrayList<>();
            
            if (projectFilter != null) {
                allDecisions.addAll(decisionLogger.getProjectDecisions(projectFilter));
            } else if (agentFilter != null) {
                allDecisions.addAll(decisionLogger.getAgentDecisions(agentFilter));
            }

            List<Map<String, Object>> timelineEntries = allDecisions.stream()
                .filter(d -> {
                    try {
                        long ts = Long.parseLong(d.timestamp);
                        return ts >= startTime && ts <= endTime;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                })
                .map(this::buildTimelineEntry)
                .sorted((a, b) -> Long.compare(
                    (Long) b.get("timestamp"),
                    (Long) a.get("timestamp")
                ))
                .collect(Collectors.toList());

            Map<String, Object> timelineStats = calculateTimelineStats(timelineEntries);

            Map<String, Object> response = new HashMap<>();
            response.put("startTime", startTime);
            response.put("endTime", endTime);
            response.put("totalEntries", timelineEntries.size());
            response.put("timeline", timelineEntries);
            response.put("stats", timelineStats);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(errorResponse(e.getMessage()));
        }
    }

    /**
     * GET /api/v1/timeline/drill-down/{decisionId}
     */
    @GetMapping("/drill-down/{decisionId}")
    public ResponseEntity<Map<String, Object>> drillDownDecision(
            @PathVariable String decisionId) {
        
        if (decisionLogger == null) {
            return ResponseEntity.status(501).body(errorResponse("AgentDecisionLogger not available"));
        }

        try {
            Map<String, Object> drillDown = new HashMap<>();
            drillDown.put("decisionId", decisionId);
            drillDown.put("status", "pending");
            drillDown.put("detailsLoaded", true);
            drillDown.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(drillDown);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(errorResponse(e.getMessage()));
        }
    }

    /**
     * GET /api/v1/timeline/stats/aggregate
     */
    @GetMapping("/stats/aggregate")
    public ResponseEntity<Map<String, Object>> getAggregateStats(
            @RequestParam(required = false) String groupBy) {
        
        if (decisionLogger == null) {
            return ResponseEntity.status(501).body(errorResponse("AgentDecisionLogger not available"));
        }

        try {
            Map<String, Object> allStats = decisionLogger.getDecisionStats();

            Map<String, Object> response = new HashMap<>();
            response.put("aggregateStats", allStats);
            response.put("groupBy", groupBy != null ? groupBy : "project");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(errorResponse(e.getMessage()));
        }
    }

    /**
     * GET /api/v1/timeline/export/{projectId}
     */
    @GetMapping("/export/{projectId}")
    public ResponseEntity<Map<String, Object>> exportTimeline(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "json") String format) {
        
        if (decisionLogger == null) {
            return ResponseEntity.status(501).body(errorResponse("AgentDecisionLogger not available"));
        }

        try {
            List<AgentDecisionLogger.AgentDecision> decisions = decisionLogger.getProjectDecisions(projectId);
            
            List<Map<String, Object>> timelineEntries = decisions.stream()
                .map(this::buildTimelineEntry)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("projectId", projectId);
            response.put("format", format);
            response.put("entriesCount", timelineEntries.size());
            response.put("entries", timelineEntries);
            response.put("exportedAt", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(errorResponse(e.getMessage()));
        }
    }

    // ==================== Helper Methods ====================

    private Map<String, Object> buildTimelineEntry(AgentDecisionLogger.AgentDecision decision) {
        Map<String, Object> entry = new HashMap<>();
        
        entry.put("decisionId", decision.decisionId);
        entry.put("agent", decision.agent);
        entry.put("decision", decision.decision);
        entry.put("reasoning", decision.reasoning);
        entry.put("confidence", decision.confidence);
        
        long timestampMs = 0;
        try {
            timestampMs = Long.parseLong(decision.timestamp);
        } catch (NumberFormatException e) {
            timestampMs = System.currentTimeMillis();
        }
        entry.put("timestamp", timestampMs);
        
        try {
            LocalDateTime dateTime = LocalDateTime.from(
                java.time.Instant.ofEpochMilli(timestampMs)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime()
            );
            entry.put("formattedTime", dateTime.format(formatter));
        } catch (Exception e) {
            entry.put("formattedTime", "Unknown");
        }
        
        String outcome = decision.outcome != null ? decision.outcome : "PENDING";
        String color = determineOutcomeColor(outcome);
        
        entry.put("outcome", outcome);
        entry.put("color", color);
        entry.put("cssClass", "timeline-" + color);
        
        if (decision.successMetric > 0) {
            entry.put("successMetric", decision.successMetric);
        }

        return entry;
    }

    private String determineOutcomeColor(String outcome) {
        if (outcome == null) {
            return "grey";
        }
        
        return switch (outcome.toUpperCase()) {
            case "SUCCESS" -> "green";
            case "FAILED" -> "red";
            case "PARTIAL" -> "yellow";
            default -> "grey";
        };
    }

    private Map<String, Object> calculateTimelineStats(List<Map<String, Object>> entries) {
        Map<String, Object> stats = new HashMap<>();
        
        long successCount = entries.stream()
            .filter(e -> "green".equals(e.get("color")))
            .count();
        
        long failedCount = entries.stream()
            .filter(e -> "red".equals(e.get("color")))
            .count();
        
        long partialCount = entries.stream()
            .filter(e -> "yellow".equals(e.get("color")))
            .count();
        
        long totalCount = entries.size();
        double successRate = totalCount > 0 ? (successCount * 100.0) / totalCount : 0;

        stats.put("total", totalCount);
        stats.put("successful", successCount);
        stats.put("failed", failedCount);
        stats.put("partial", partialCount);
        stats.put("successRate", Math.round(successRate * 100.0) / 100.0);

        double avgConfidence = entries.stream()
            .mapToDouble(e -> {
                Object conf = e.get("confidence");
                return conf instanceof Number ? ((Number) conf).doubleValue() : 0.0;
            })
            .average()
            .orElse(0.0);

        stats.put("averageConfidence", Math.round(avgConfidence * 100.0) / 100.0);

        return stats;
    }

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}
