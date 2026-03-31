package org.example.model;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Phase 6 Week 5-6: Decision Timeline Model
 * Represents a visual timeline entry for decision history
 * 
 * Supports:
 * - Color-coded outcomes (SUCCESS, FAILED, PARTIAL, PENDING)
 * - Chronological ordering
 * - Outcome filtering
 * - Integration with 3D dashboard
 */
public class DecisionTimeline {
    
    private String timelineId;
    private String projectId;
    private List<TimelineEntry> entries;
    private TimelineStats stats;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DecisionTimeline() {
        this.timelineId = UUID.randomUUID().toString();
        this.entries = new ArrayList<>();
        this.stats = new TimelineStats();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public DecisionTimeline(String projectId) {
        this();
        this.projectId = projectId;
    }

    // ==================== Timeline Entry ====================

    /**
     * Individual timeline entry for a decision
     */
    public static class TimelineEntry {
        public String decisionId;
        public String agent;
        public String decision;
        public String reasoning;
        public float confidence;
        public long timestamp;
        public String outcome;
        public OutcomeColor color;
        public String formattedTime;
        public double successMetric;
        public Map<String, String> metadata;

        public TimelineEntry() {
            this.metadata = new HashMap<>();
        }

        public TimelineEntry(String decisionId, String agent, String decision) {
            this();
            this.decisionId = decisionId;
            this.agent = agent;
            this.decision = decision;
            this.timestamp = System.currentTimeMillis();
            this.outcome = OutcomeType.PENDING.toString();
            this.color = OutcomeColor.GREY;
        }

        public void setOutcome(OutcomeType type) {
            this.outcome = type.toString();
            this.color = OutcomeColor.fromOutcomeType(type);
        }

        public boolean isSuccessful() {
            return OutcomeType.SUCCESS.toString().equals(outcome);
        }

        public boolean isFailed() {
            return OutcomeType.FAILED.toString().equals(outcome);
        }

        public boolean isPartial() {
            return OutcomeType.PARTIAL.toString().equals(outcome);
        }
    }

    // ==================== Outcome Types ====================

    /**
     * Outcome enumeration for decision results
     */
    public enum OutcomeType {
        SUCCESS("success", "Decision executed successfully"),
        FAILED("failed", "Decision execution failed"),
        PARTIAL("partial", "Decision partially executed"),
        PENDING("pending", "Decision outcome pending");

        public final String value;
        public final String description;

        OutcomeType(String value, String description) {
            this.value = value;
            this.description = description;
        }
    }

    /**
     * Color enumeration for UI rendering
     */
    public enum OutcomeColor {
        GREEN("#27ae60", "success"),
        RED("#e74c3c", "failed"),
        YELLOW("#f39c12", "partial"),
        GREY("#95a5a6", "pending");

        public final String hexCode;
        public final String outcomeType;

        OutcomeColor(String hexCode, String outcomeType) {
            this.hexCode = hexCode;
            this.outcomeType = outcomeType;
        }

        public static OutcomeColor fromOutcomeType(OutcomeType type) {
            return switch (type) {
                case SUCCESS -> GREEN;
                case FAILED -> RED;
                case PARTIAL -> YELLOW;
                case PENDING -> GREY;
            };
        }

        public static OutcomeColor fromString(String outcome) {
            return switch (outcome.toUpperCase()) {
                case "SUCCESS" -> GREEN;
                case "FAILED" -> RED;
                case "PARTIAL" -> YELLOW;
                default -> GREY;
            };
        }
    }

    // ==================== Timeline Statistics ====================

    /**
     * Aggregate statistics for timeline
     */
    public static class TimelineStats {
        public long totalEntries;
        public long successfulCount;
        public long failedCount;
        public long partialCount;
        public long pendingCount;
        public double successRate;
        public double failureRate;
        public double averageConfidence;
        public long earliestTimestamp;
        public long latestTimestamp;
        public Map<String, Long> agentCounts;
        public Map<String, Long> outcomeCounts;

        public TimelineStats() {
            this.agentCounts = new HashMap<>();
            this.outcomeCounts = new HashMap<>();
            this.outcomeCounts.put("SUCCESS", 0L);
            this.outcomeCounts.put("FAILED", 0L);
            this.outcomeCounts.put("PARTIAL", 0L);
            this.outcomeCounts.put("PENDING", 0L);
        }

        public void update(List<TimelineEntry> entries) {
            if (entries.isEmpty()) {
                return;
            }

            this.totalEntries = entries.size();
            this.successfulCount = entries.stream().filter(TimelineEntry::isSuccessful).count();
            this.failedCount = entries.stream().filter(TimelineEntry::isFailed).count();
            this.partialCount = entries.stream().filter(TimelineEntry::isPartial).count();
            this.pendingCount = totalEntries - successfulCount - failedCount - partialCount;

            this.successRate = totalEntries > 0 ? (successfulCount * 100.0) / totalEntries : 0;
            this.failureRate = totalEntries > 0 ? (failedCount * 100.0) / totalEntries : 0;

            // Calculate average confidence
            this.averageConfidence = entries.stream()
                .mapToDouble(e -> e.confidence)
                .average()
                .orElse(0.0);

            // Get time range
            this.earliestTimestamp = entries.stream()
                .mapToLong(e -> e.timestamp)
                .min()
                .orElse(0);

            this.latestTimestamp = entries.stream()
                .mapToLong(e -> e.timestamp)
                .max()
                .orElse(0);

            // Count by agent
            entries.stream()
                .forEach(e -> agentCounts.merge(e.agent, 1L, Long::sum));

            // Count by outcome
            entries.stream()
                .forEach(e -> outcomeCounts.merge(e.outcome.toUpperCase(), 1L, Long::sum));
        }

        public long getDurationMillis() {
            return latestTimestamp - earliestTimestamp;
        }

        public boolean hasHighSuccessRate() {
            return successRate >= 70.0;
        }

        public boolean hasHighFailureRate() {
            return failureRate >= 30.0;
        }
    }

    // ==================== Timeline Filters ====================

    /**
     * Filter criteria for timeline entries
     */
    public static class TimelineFilter {
        public OutcomeType outcomeFilter;
        public String agentFilter;
        public long startTime;
        public long endTime;
        public float minConfidence;
        public int maxResults;

        public TimelineFilter() {
            this.minConfidence = 0.0f;
            this.maxResults = 100;
        }

        public boolean matches(TimelineEntry entry) {
            if (outcomeFilter != null && !entry.outcome.equals(outcomeFilter.toString())) {
                return false;
            }
            if (agentFilter != null && !entry.agent.equals(agentFilter)) {
                return false;
            }
            if (startTime > 0 && entry.timestamp < startTime) {
                return false;
            }
            if (endTime > 0 && entry.timestamp > endTime) {
                return false;
            }
            return entry.confidence >= minConfidence;
        }
    }

    // ==================== Getters and Setters ====================

    public String getTimelineId() {
        return timelineId;
    }

    public void setTimelineId(String timelineId) {
        this.timelineId = timelineId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public List<TimelineEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<TimelineEntry> entries) {
        this.entries = entries;
        this.stats.update(entries);
        this.updatedAt = LocalDateTime.now();
    }

    public void addEntry(TimelineEntry entry) {
        this.entries.add(entry);
        this.updatedAt = LocalDateTime.now();
    }

    public TimelineStats getStats() {
        return stats;
    }

    public void setStats(TimelineStats stats) {
        this.stats = stats;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ==================== Utility Methods ====================

    /**
     * Filter entries by criteria
     */
    public List<TimelineEntry> filter(TimelineFilter filter) {
        return entries.stream()
            .filter(filter::matches)
            .limit(filter.maxResults)
            .sorted((a, b) -> Long.compare(b.timestamp, a.timestamp))
            .toList();
    }

    /**
     * Get entries by outcome
     */
    public List<TimelineEntry> getEntriesByOutcome(OutcomeType outcome) {
        return entries.stream()
            .filter(e -> e.outcome.equals(outcome.toString()))
            .sorted((a, b) -> Long.compare(b.timestamp, a.timestamp))
            .toList();
    }

    /**
     * Get entries by agent
     */
    public List<TimelineEntry> getEntriesByAgent(String agent) {
        return entries.stream()
            .filter(e -> e.agent.equals(agent))
            .sorted((a, b) -> Long.compare(b.timestamp, a.timestamp))
            .toList();
    }

    /**
     * Get representation as map for JSON serialization
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("timelineId", timelineId);
        map.put("projectId", projectId);
        map.put("entries", entries);
        map.put("stats", stats);
        map.put("createdAt", createdAt.toString());
        map.put("updatedAt", updatedAt.toString());
        return map;
    }

    @Override
    public String toString() {
        return "DecisionTimeline{" +
                "timelineId='" + timelineId + '\'' +
                ", projectId='" + projectId + '\'' +
                ", entriesCount=" + entries.size() +
                ", successRate=" + stats.successRate + "%" +
                '}';
    }
}
