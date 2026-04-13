package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Phase 5: Persistent Analytics Service
 * Stores metrics in Firestore for historical analysis and trends
 */
@Service
public class PersistentAnalyticsService {

    @Autowired(required = false)
    private com.google.cloud.firestore.Firestore firestore;

    private static class MetricsSnapshot {
        public LocalDateTime timestamp;
        public double memoryUsage;
        public double cpuUsage;
        public int activeRequests;
        public double successRate;
        public double avgLatency;
        public String frameworkName;
        public int generationCount;
        public int errorCount;

        MetricsSnapshot() {
            this.timestamp = LocalDateTime.now();
        }
    }

    private static class TimeSeriesData {
        public String metric;
        public List<Double> values;
        public List<LocalDateTime> timestamps;
        public LocalDateTime startTime;
        public LocalDateTime endTime;

        TimeSeriesData(String metric) {
            this.metric = metric;
            this.values = Collections.synchronizedList(new ArrayList<>());
            this.timestamps = Collections.synchronizedList(new ArrayList<>());
        }
    }

    private final Map<String, TimeSeriesData> timeSeries = new ConcurrentHashMap<>();
    private final List<MetricsSnapshot> snapshots = Collections.synchronizedList(new ArrayList<>());
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Record metric snapshot for persistence
     */
    public void recordSnapshot(Map<String, Object> metrics) {
        MetricsSnapshot snapshot = new MetricsSnapshot();
        snapshot.memoryUsage = ((Number) metrics.getOrDefault("memory", 0)).doubleValue();
        snapshot.cpuUsage = ((Number) metrics.getOrDefault("cpu", 0)).doubleValue();
        snapshot.activeRequests = ((Number) metrics.getOrDefault("requests", 0)).intValue();
        snapshot.successRate = ((Number) metrics.getOrDefault("successRate", 100)).doubleValue();
        snapshot.avgLatency = ((Number) metrics.getOrDefault("latency", 0)).doubleValue();

        snapshots.add(snapshot);
        persistToFirestore(snapshot);

        // Keep only last 1000 snapshots
        if (snapshots.size() > 1000) {
            snapshots.remove(0);
        }
    }

    /**
     * Get historical metrics for time range
     */
    public Map<String, Object> getHistoricalMetrics(LocalDateTime startTime, LocalDateTime endTime) {
        List<MetricsSnapshot> filtered = snapshots.stream()
                .filter(s -> !s.timestamp.isBefore(startTime) && !s.timestamp.isAfter(endTime))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            return Map.of("message", "No data for specified time range");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("period", Map.of(
            "start", startTime.toString(),
            "end", endTime.toString(),
            "duration", ChronoUnit.HOURS.between(startTime, endTime) + " hours"
        ));

        // Calculate statistics
        DoubleSummaryStatistics memStats = filtered.stream()
                .mapToDouble(s -> s.memoryUsage)
                .summaryStatistics();
        
        DoubleSummaryStatistics cpuStats = filtered.stream()
                .mapToDouble(s -> s.cpuUsage)
                .summaryStatistics();

        DoubleSummaryStatistics latencyStats = filtered.stream()
                .mapToDouble(s -> s.avgLatency)
                .summaryStatistics();

        result.put("memory", Map.of(
            "min", String.format("%.2f MB", memStats.getMin()),
            "max", String.format("%.2f MB", memStats.getMax()),
            "avg", String.format("%.2f MB", memStats.getAverage())
        ));

        result.put("cpu", Map.of(
            "min", String.format("%.1f%%", cpuStats.getMin()),
            "max", String.format("%.1f%%", cpuStats.getMax()),
            "avg", String.format("%.1f%%", cpuStats.getAverage())
        ));

        result.put("latency", Map.of(
            "min", String.format("%.0f ms", latencyStats.getMin()),
            "max", String.format("%.0f ms", latencyStats.getMax()),
            "avg", String.format("%.0f ms", latencyStats.getAverage())
        ));

        result.put("totalSnapshots", filtered.size());

        return result;
    }

    /**
     * Get trend analysis (improving/degrading)
     */
    public Map<String, Object> getTrendAnalysis(String metric, int hours) {
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
        
        List<MetricsSnapshot> filtered = snapshots.stream()
                .filter(s -> !s.timestamp.isBefore(startTime))
                .collect(Collectors.toList());

        if (filtered.size() < 2) {
            return Map.of("message", "Insufficient data for trend analysis");
        }

        // Split into two halves
        int midpoint = filtered.size() / 2;
        List<MetricsSnapshot> firstHalf = filtered.subList(0, midpoint);
        List<MetricsSnapshot> secondHalf = filtered.subList(midpoint, filtered.size());

        Map<String, Object> analysis = new HashMap<>();
        analysis.put("metric", metric);
        analysis.put("period", hours + " hours");
        analysis.put("samples", filtered.size());

        double firstAvg = getMetricAverage(firstHalf, metric);
        double secondAvg = getMetricAverage(secondHalf, metric);
        double change = ((secondAvg - firstAvg) / firstAvg) * 100;

        analysis.put("firstPeriodAvg", String.format("%.2f", firstAvg));
        analysis.put("secondPeriodAvg", String.format("%.2f", secondAvg));
        analysis.put("changePercent", String.format("%.2f%%", change));
        analysis.put("trend", Math.abs(change) < 5 ? "STABLE" : (change > 0 ? "IMPROVING" : "DEGRADING"));

        return analysis;
    }

    /**
     * Get daily summary
     */
    public Map<String, Object> getDailySummary(LocalDateTime date) {
        LocalDateTime startOfDay = date.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = date.withHour(23).withMinute(59).withSecond(59);

        List<MetricsSnapshot> daySnapshots = snapshots.stream()
                .filter(s -> !s.timestamp.isBefore(startOfDay) && !s.timestamp.isAfter(endOfDay))
                .collect(Collectors.toList());

        Map<String, Object> summary = new HashMap<>();
        summary.put("date", date.toLocalDate().toString());
        summary.put("snapshotCount", daySnapshots.size());

        if (!daySnapshots.isEmpty()) {
            DoubleSummaryStatistics stats = daySnapshots.stream()
                    .mapToDouble(s -> s.avgLatency)
                    .summaryStatistics();

            summary.put("avgLatency", String.format("%.0f ms", stats.getAverage()));
            summary.put("peakLatency", String.format("%.0f ms", stats.getMax()));
            summary.put("minLatency", String.format("%.0f ms", stats.getMin()));

            double avgSuccessRate = daySnapshots.stream()
                    .mapToDouble(s -> s.successRate)
                    .average()
                    .orElse(100);
            summary.put("avgSuccessRate", String.format("%.1f%%", avgSuccessRate));

            int totalErrors = daySnapshots.stream()
                    .mapToInt(s -> s.errorCount)
                    .sum();
            summary.put("totalErrors", totalErrors);
        }

        return summary;
    }

    /**
     * Get monthly summary with aggregations
     */
    public Map<String, Object> getMonthlySummary(int year, int month) {
        LocalDateTime monthStart = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);

        List<MetricsSnapshot> monthSnapshots = snapshots.stream()
                .filter(s -> !s.timestamp.isBefore(monthStart) && !s.timestamp.isAfter(monthEnd))
                .collect(Collectors.toList());

        Map<String, Object> summary = new HashMap<>();
        summary.put("period", year + "-" + String.format("%02d", month));
        summary.put("totalSnapshots", monthSnapshots.size());

        if (!monthSnapshots.isEmpty()) {
            double avgMemory = monthSnapshots.stream()
                    .mapToDouble(s -> s.memoryUsage)
                    .average()
                    .orElse(0);

            double avgCpu = monthSnapshots.stream()
                    .mapToDouble(s -> s.cpuUsage)
                    .average()
                    .orElse(0);

            double avgLatency = monthSnapshots.stream()
                    .mapToDouble(s -> s.avgLatency)
                    .average()
                    .orElse(0);

            summary.put("avgMemory", String.format("%.2f MB", avgMemory));
            summary.put("avgCpu", String.format("%.1f%%", avgCpu));
            summary.put("avgLatency", String.format("%.0f ms", avgLatency));

            int totalGenerations = monthSnapshots.stream()
                    .mapToInt(s -> s.generationCount)
                    .sum();
            int totalErrors = monthSnapshots.stream()
                    .mapToInt(s -> s.errorCount)
                    .sum();

            summary.put("totalGenerations", totalGenerations);
            summary.put("totalErrors", totalErrors);
            summary.put("successRate", totalGenerations == 0 ? 100 : 
                String.format("%.1f%%", ((double) (totalGenerations - totalErrors) / totalGenerations) * 100));
        }

        return summary;
    }

    /**
     * Export metrics to JSON
     */
    public String exportMetricsAsJson(LocalDateTime startTime, LocalDateTime endTime) {
        List<MetricsSnapshot> filtered = snapshots.stream()
                .filter(s -> !s.timestamp.isBefore(startTime) && !s.timestamp.isAfter(endTime))
                .collect(Collectors.toList());

        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(filtered);
        } catch (Exception e) {
            return "{\"error\": \"Failed to export metrics\"}";
        }
    }

    /**
     * Export metrics to CSV
     */
    public String exportMetricsAsCsv(LocalDateTime startTime, LocalDateTime endTime) {
        List<MetricsSnapshot> filtered = snapshots.stream()
                .filter(s -> !s.timestamp.isBefore(startTime) && !s.timestamp.isAfter(endTime))
                .collect(Collectors.toList());

        StringBuilder csv = new StringBuilder();
        csv.append("Timestamp,Memory(MB),CPU(%),Requests,SuccessRate(%),LatencyMs\n");

        for (MetricsSnapshot s : filtered) {
            csv.append(String.format("%s,%.2f,%.1f,%d,%.1f,%.0f\n",
                    s.timestamp, s.memoryUsage, s.cpuUsage, s.activeRequests, 
                    s.successRate, s.avgLatency));
        }

        return csv.toString();
    }

    /**
     * Get comparison between time periods
     */
    public Map<String, Object> comparePeriods(LocalDateTime period1Start, LocalDateTime period1End,
                                              LocalDateTime period2Start, LocalDateTime period2End) {
        Map<String, Object> period1 = getHistoricalMetrics(period1Start, period1End);
        Map<String, Object> period2 = getHistoricalMetrics(period2Start, period2End);

        Map<String, Object> comparison = new HashMap<>();
        comparison.put("period1", period1);
        comparison.put("period2", period2);
        comparison.put("comparison", "Compare above metrics for trends");

        return comparison;
    }

    /**
     * Helper: Get metric average from snapshots
     */
    private double getMetricAverage(List<MetricsSnapshot> snapshots, String metric) {
        return switch (metric.toLowerCase()) {
            case "memory" -> snapshots.stream().mapToDouble(s -> s.memoryUsage).average().orElse(0);
            case "cpu" -> snapshots.stream().mapToDouble(s -> s.cpuUsage).average().orElse(0);
            case "latency" -> snapshots.stream().mapToDouble(s -> s.avgLatency).average().orElse(0);
            case "successrate" -> snapshots.stream().mapToDouble(s -> s.successRate).average().orElse(100);
            default -> 0;
        };
    }

    /**
     * Persist snapshot to Firestore (async)
     */
    private void persistToFirestore(MetricsSnapshot snapshot) {
        if (firestore == null) {
            return; // Firestore not configured
        }

        try {
            // Store in Firestore collection
            firestore.collection("metrics")
                    .document(snapshot.timestamp.toString())
                    .set(snapshot);
        } catch (Exception e) {
            System.err.println("Failed to persist to Firestore: " + e.getMessage());
        }
    }

    /**
     * Clear old snapshots (retention policy)
     */
    public void clearOldSnapshots(int retentionDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        snapshots.removeIf(s -> s.timestamp.isBefore(cutoff));
    }
}
