package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.SystemLearning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Auto-ingests execution logs and converts failures/partial outcomes
 * into structured incident learning playbooks.
 */
@Service
public class IncidentLearningIngestionService {
    private static final Logger logger = LoggerFactory.getLogger(IncidentLearningIngestionService.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Path DEFAULT_LOG_DIR = Paths.get("./execution_logs");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private SystemLearningService learningService;

    public Map<String, Object> ingestRecentExecutionLogs(int maxFiles) {
        return ingestExecutionLogs(DEFAULT_LOG_DIR, Math.max(maxFiles, 1));
    }

    @Scheduled(cron = "${learning.incident-ingestion.cron:0 0 3 * * *}")
    public void scheduledIncidentIngestion() {
        try {
            Map<String, Object> result = ingestRecentExecutionLogs(14);
            logger.info("🧠 Scheduled incident ingestion complete: {}", result);
        } catch (Exception e) {
            logger.error("❌ Scheduled incident ingestion failed: {}", e.getMessage(), e);
        }
    }

    public Map<String, Object> getIncidentLearningInsights() {
        List<SystemLearning> incidents = learningService.getIncidentPlaybooks(null);

        Map<String, Long> byCategory = incidents.stream()
            .collect(Collectors.groupingBy(
                learning -> normalizeCategory(learning.getCategory()),
                Collectors.counting()
            ));

        Map<String, Long> byDay = incidents.stream()
            .collect(Collectors.groupingBy(
                learning -> Instant.ofEpochMilli(learning.getTimestamp())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .format(DAY_FMT),
                TreeMap::new,
                Collectors.counting()
            ));

        double avgConfidence = incidents.stream()
            .map(SystemLearning::getConfidenceScore)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);

        List<Map<String, Object>> topCategories = byCategory.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .map(entry -> {
                Map<String, Object> row = new HashMap<>();
                row.put("category", entry.getKey());
                row.put("count", entry.getValue());
                return row;
            })
            .collect(Collectors.toList());

        return Map.of(
            "status", "success",
            "totalIncidents", incidents.size(),
            "averageConfidence", avgConfidence,
            "topCategories", topCategories,
            "incidentsByDay", byDay,
            "timestamp", System.currentTimeMillis()
        );
    }

    Map<String, Object> ingestExecutionLogs(Path logDir, int maxFiles) {
        if (learningService == null) {
            return Map.of("status", "error", "message", "SystemLearningService unavailable");
        }

        if (logDir == null || !Files.exists(logDir) || !Files.isDirectory(logDir)) {
            return Map.of(
                "status", "success",
                "message", "No execution log directory found",
                "scannedFiles", 0,
                "eventsScanned", 0,
                "incidentsLearned", 0
            );
        }

        int scannedFiles = 0;
        int eventsScanned = 0;
        int incidentsLearned = 0;
        int skippedEvents = 0;
        Set<String> seenFingerprints = new HashSet<>();

        try {
            List<Path> files = Files.list(logDir)
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .sorted(Comparator.comparingLong(this::safeLastModified).reversed())
                .limit(maxFiles)
                .collect(Collectors.toList());

            for (Path file : files) {
                scannedFiles++;
                List<Map<String, Object>> events = readEvents(file);
                for (Map<String, Object> event : events) {
                    eventsScanned++;

                    if (!isIncidentCandidate(event)) {
                        skippedEvents++;
                        continue;
                    }

                    String fingerprint = fingerprint(event);
                    if (!seenFingerprints.add(fingerprint)) {
                        skippedEvents++;
                        continue;
                    }

                    IncidentDraft draft = toIncidentDraft(event, file.getFileName().toString());
                    Map<String, Object> result = learningService.learnFromIncident(
                        draft.category,
                        draft.problem,
                        draft.rootCause,
                        draft.fix,
                        draft.preventionChecks,
                        draft.confidence,
                        draft.metadata
                    );

                    if ("success".equals(result.get("status"))) {
                        incidentsLearned++;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("❌ Incident ingestion failed: {}", e.getMessage(), e);
            return Map.of(
                "status", "error",
                "message", e.getMessage(),
                "scannedFiles", scannedFiles,
                "eventsScanned", eventsScanned,
                "incidentsLearned", incidentsLearned
            );
        }

        return Map.of(
            "status", "success",
            "scannedFiles", scannedFiles,
            "eventsScanned", eventsScanned,
            "incidentsLearned", incidentsLearned,
            "skippedEvents", skippedEvents,
            "timestamp", System.currentTimeMillis()
        );
    }

    private List<Map<String, Object>> readEvents(Path file) {
        try {
            String content = Files.readString(file);
            return mapper.readValue(content, new TypeReference<List<Map<String, Object>>>() {});
        } catch (IOException e) {
            logger.warn("⚠️ Failed to parse log file {}: {}", file, e.getMessage());
            return List.of();
        }
    }

    private boolean isIncidentCandidate(Map<String, Object> event) {
        String status = String.valueOf(event.getOrDefault("status", "")).toUpperCase(Locale.ROOT);
        if ("FAILED".equals(status) || "PARTIAL".equals(status)) {
            return true;
        }

        String eventType = String.valueOf(event.getOrDefault("eventType", "")).toUpperCase(Locale.ROOT);
        if ("ERROR_FIX".equals(eventType)) {
            Map<String, Object> metadata = getMetadata(event);
            int totalIssues = safeInt(metadata.get("totalIssues"));
            int appliedFixes = safeInt(metadata.get("appliedFixes"));
            return totalIssues > 0 && appliedFixes < totalIssues;
        }

        return false;
    }

    private IncidentDraft toIncidentDraft(Map<String, Object> event, String sourceFile) {
        String eventType = String.valueOf(event.getOrDefault("eventType", "UNKNOWN")).toUpperCase(Locale.ROOT);
        String category = deriveCategory(eventType, event);
        String projectId = String.valueOf(event.getOrDefault("projectId", "unknown-project"));
        String component = String.valueOf(event.getOrDefault("componentName", ""));
        String status = String.valueOf(event.getOrDefault("status", "UNKNOWN"));
        Map<String, Object> metadata = getMetadata(event);

        int totalIssues = safeInt(metadata.get("totalIssues"));
        int appliedFixes = safeInt(metadata.get("appliedFixes"));
        String problem = String.format(
            "%s incident in project=%s component=%s status=%s issues=%d fixes=%d",
            eventType,
            projectId,
            component,
            status,
            totalIssues,
            appliedFixes
        );

        String rootCause;
        if ("VALIDATION".equals(eventType) && "PARTIAL".equalsIgnoreCase(status)) {
            rootCause = "Validation quality gate failed; unresolved issues remain";
        } else if ("ERROR_FIX".equals(eventType) && totalIssues > appliedFixes) {
            rootCause = "Auto-fix coverage insufficient for detected issues";
        } else if ("GENERATION".equals(eventType) && "FAILED".equalsIgnoreCase(status)) {
            rootCause = "Generation failed due to provider/tool/runtime error";
        } else {
            rootCause = "Execution pipeline instability detected";
        }

        String fix;
        if ("VALIDATION".equals(eventType)) {
            fix = "Run targeted fix loop, then re-run validation until issues are zero";
        } else if ("ERROR_FIX".equals(eventType)) {
            fix = "Extend fix patterns for unresolved issue classes and re-validate";
        } else {
            fix = "Inspect failing step, apply category-specific remediation, and re-run pipeline";
        }

        List<String> preventionChecks = new ArrayList<>();
        preventionChecks.add("Add regression test for this failure signature");
        preventionChecks.add("Re-run build/validation gates before marking success");
        preventionChecks.add("Track recurrence count by category");
        if ("SECURITY".equals(category)) {
            preventionChecks.add("Verify least-privilege authorization tests pass");
        }

        double confidence = deriveConfidence(eventType, status, event);

        Map<String, Object> incidentMetadata = new HashMap<>();
        incidentMetadata.put("source", "execution-log-ingestion");
        incidentMetadata.put("sourceFile", sourceFile);
        incidentMetadata.put("eventType", eventType);
        incidentMetadata.put("projectId", projectId);
        incidentMetadata.put("component", component);
        incidentMetadata.put("rawStatus", status);
        incidentMetadata.put("eventTimestamp", event.get("timestamp"));
        incidentMetadata.putAll(metadata);

        return new IncidentDraft(category, problem, rootCause, fix, preventionChecks, confidence, incidentMetadata);
    }

    private String deriveCategory(String eventType, Map<String, Object> event) {
        if ("VALIDATION".equals(eventType)) {
            return "VALIDATION";
        }
        if ("ERROR_FIX".equals(eventType)) {
            return "DEBUGGING";
        }
        if ("GENERATION".equals(eventType)) {
            String framework = String.valueOf(event.getOrDefault("framework", "")).toUpperCase(Locale.ROOT);
            if (framework.contains("FLUTTER")) {
                return "FLUTTER";
            }
            if (framework.contains("REACT")) {
                return "REACT";
            }
            return "APP_CREATION";
        }
        return "GENERAL";
    }

    private double deriveConfidence(String eventType, String status, Map<String, Object> event) {
        if ("FAILED".equalsIgnoreCase(status)) {
            return 0.95;
        }
        if ("PARTIAL".equalsIgnoreCase(status)) {
            return 0.9;
        }

        if ("ERROR_FIX".equals(eventType)) {
            Map<String, Object> metadata = getMetadata(event);
            int totalIssues = safeInt(metadata.get("totalIssues"));
            int appliedFixes = safeInt(metadata.get("appliedFixes"));
            if (totalIssues > 0) {
                double unresolvedRate = (double) (totalIssues - appliedFixes) / totalIssues;
                return Math.max(0.7, Math.min(0.96, 0.96 - unresolvedRate * 0.2));
            }
        }

        return 0.85;
    }

    private String fingerprint(Map<String, Object> event) {
        return String.join("|",
            String.valueOf(event.getOrDefault("eventType", "")),
            String.valueOf(event.getOrDefault("projectId", "")),
            String.valueOf(event.getOrDefault("componentName", "")),
            String.valueOf(event.getOrDefault("status", "")),
            String.valueOf(event.getOrDefault("timestamp", ""))
        );
    }

    private Map<String, Object> getMetadata(Map<String, Object> event) {
        Object metadata = event.get("metadata");
        if (metadata instanceof Map<?, ?> rawMap) {
            Map<String, Object> normalized = new HashMap<>();
            rawMap.forEach((k, v) -> normalized.put(String.valueOf(k), v));
            return normalized;
        }
        return new HashMap<>();
    }

    private int safeInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private long safeLastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "GENERAL";
        }
        return category.trim().toUpperCase(Locale.ROOT);
    }

    private record IncidentDraft(
        String category,
        String problem,
        String rootCause,
        String fix,
        List<String> preventionChecks,
        Double confidence,
        Map<String, Object> metadata
    ) {}
}
