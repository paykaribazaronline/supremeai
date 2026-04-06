package org.example.service;

import org.example.api.ProjectGenerationController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * ActivitySummaryService — aggregates what the system did in the last 24 hours.
 *
 * Sources:
 *  - SystemLearningService   → total learnings, errors resolved, patterns, techniques
 *  - IdleResearchService     → research cycles run, topics explored, domain coverage
 *  - ActiveLearningHarvesterService → harvest runs, items from GitHub / web search
 *  - ExistingProjectService  → improvement cycles per project, last improved
 *  - ProjectGenerationController → new projects created, their status
 *  - MetricsService          → AI call counts, error rate, latency
 *
 * Additionally keeps an in-process event log so individual actions are visible.
 */
@Service
public class ActivitySummaryService {

    private static final long WINDOW_MS = 24L * 60 * 60 * 1000;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    private SystemLearningService systemLearningService;

    @Autowired(required = false)
    private IdleResearchService idleResearchService;

    @Autowired(required = false)
    private ActiveLearningHarvesterService harvesterService;

    @Autowired(required = false)
    private ExistingProjectService existingProjectService;

    @Autowired(required = false)
    private ProjectGenerationController projectGenerationController;

    @Autowired(required = false)
    private MetricsService metricsService;

    /** Lightweight in-process event log (newest first, capped at 500 entries). */
    private final ConcurrentLinkedDeque<ActivityEvent> eventLog = new ConcurrentLinkedDeque<>();
    private static final int MAX_EVENTS = 500;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Record a discrete activity event (called by other services when something happens).
     */
    public void record(String category, String title, String detail) {
        ActivityEvent e = new ActivityEvent(System.currentTimeMillis(), category, title, detail);
        eventLog.addFirst(e);
        while (eventLog.size() > MAX_EVENTS) eventLog.pollLast();
    }

    /**
     * Build the full 24-hour summary map returned by the REST endpoint.
     */
    public Map<String, Object> buildSummary() {
        long since = System.currentTimeMillis() - WINDOW_MS;
        Map<String, Object> summary = new LinkedHashMap<>();

        summary.put("windowHours", 24);
        summary.put("generatedAt", LocalDateTime.now().toString());

        // ── Learning stats ────────────────────────────────────────────────────
        summary.put("learning", buildLearningSection());

        // ── Research stats ────────────────────────────────────────────────────
        summary.put("research", buildResearchSection(since));

        // ── Harvest stats ─────────────────────────────────────────────────────
        summary.put("harvest", buildHarvestSection());

        // ── Project improvement stats ─────────────────────────────────────────
        summary.put("existingProjects", buildExistingProjectsSection(since));

        // ── New project generation stats ───────────────────────────────────────
        summary.put("newProjects", buildNewProjectsSection(since));

        // ── AI call stats ─────────────────────────────────────────────────────
        summary.put("aiCalls", buildAiCallsSection());

        // ── Recent event log (last 24h) ────────────────────────────────────────
        List<Map<String, Object>> recentEvents = eventLog.stream()
            .filter(ev -> ev.timestampMs >= since)
            .map(ActivityEvent::toMap)
            .collect(Collectors.toList());
        summary.put("recentEvents", recentEvents);
        summary.put("totalEventsLast24h", recentEvents.size());

        return summary;
    }

    // ── Section builders ─────────────────────────────────────────────────────

    private Map<String, Object> buildLearningSection() {
        Map<String, Object> s = new LinkedHashMap<>();
        try {
            Map<String, Object> stats = systemLearningService.getLearningStats();
            s.put("totalKnowledgeItems", stats.getOrDefault("totalLearnings", 0));
            s.put("errorsResolved",      stats.getOrDefault("errorsResolved", 0));
            s.put("patternsFound",       stats.getOrDefault("patternsFound", 0));
            s.put("techniques",          stats.getOrDefault("techniques", 0));
            s.put("avgConfidence",       stats.getOrDefault("averageConfidence", 0.0));
            s.put("byCategory",          stats.getOrDefault("byCategory", Map.of()));
            s.put("status",              stats.getOrDefault("status", "unknown"));
        } catch (Exception e) {
            s.put("error", e.getMessage());
        }
        return s;
    }

    private Map<String, Object> buildResearchSection(long since) {
        Map<String, Object> s = new LinkedHashMap<>();
        if (idleResearchService == null) {
            s.put("available", false);
            return s;
        }
        try {
            Map<String, Object> stats = idleResearchService.getResearchStats();
            s.put("available",            true);
            s.put("learningEnabled",      stats.getOrDefault("learningEnabled", false));
            s.put("totalResearchCycles",  stats.getOrDefault("totalResearchCompleted", 0));
            s.put("historySize",          stats.getOrDefault("historySize", 0));
            s.put("domainCoverage",       stats.getOrDefault("domainCoverage", Map.of()));

            // Count cycles in last 24h from recent history
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> recent = (List<Map<String, Object>>)
                stats.getOrDefault("recentTopics", Collections.emptyList());
            long last24h = recent.stream()
                .filter(t -> {
                    Object ts = t.get("completedAt");
                    if (ts == null) return false;
                    try {
                        long epoch = Long.parseLong(ts.toString());
                        return epoch >= since;
                    } catch (NumberFormatException ex) { return false; }
                }).count();
            s.put("cyclesLast24h", last24h);
            s.put("recentTopicSamples", recent.stream().limit(5).toList());
        } catch (Exception e) {
            s.put("error", e.getMessage());
        }
        return s;
    }

    private Map<String, Object> buildHarvestSection() {
        Map<String, Object> s = new LinkedHashMap<>();
        if (harvesterService == null) {
            s.put("available", false);
            return s;
        }
        s.put("available", true);
        // Harvester doesn't expose a stats API yet — report what we can from event log
        long harvestEvents = eventLog.stream()
            .filter(ev -> "HARVEST".equalsIgnoreCase(ev.category))
            .filter(ev -> ev.timestampMs >= System.currentTimeMillis() - WINDOW_MS)
            .count();
        s.put("harvestRunsLast24h", harvestEvents);
        s.put("note", "Scheduled every 6h; on-demand via POST /api/learning/harvest");
        return s;
    }

    private Map<String, Object> buildExistingProjectsSection(long since) {
        Map<String, Object> s = new LinkedHashMap<>();
        if (existingProjectService == null) {
            s.put("available", false);
            return s;
        }
        try {
            List<Map<String, Object>> projects = existingProjectService.listProjects();
            s.put("available", true);
            s.put("totalRegistered", projects.size());

            List<Map<String, Object>> improvedLast24h = projects.stream()
                .filter(p -> {
                    Object ts = p.get("lastImprovedAt");
                    if (ts == null) return false;
                    try { return Long.parseLong(ts.toString()) >= since; }
                    catch (Exception ex) { return false; }
                })
                .map(p -> {
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("id",     p.get("id"));
                    r.put("name",   p.get("name"));
                    r.put("status", p.get("status"));
                    r.put("repoUrl",p.get("repoUrl"));
                    long ts = Long.parseLong(String.valueOf(p.getOrDefault("lastImprovedAt", 0)));
                    r.put("lastImprovedAt", ts > 0
                        ? LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        : "—");
                    return r;
                })
                .collect(Collectors.toList());

            s.put("improvedLast24h", improvedLast24h.size());
            s.put("projectDetails",  improvedLast24h);

            // All projects summary
            s.put("allProjectSummary", projects.stream().map(p -> {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("name",   p.get("name"));
                r.put("status", p.get("status"));
                long ts = Long.parseLong(String.valueOf(p.getOrDefault("lastImprovedAt", 0)));
                r.put("lastImprovedAt", ts > 0
                    ? LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    : "Not yet improved");
                return r;
            }).toList());
        } catch (Exception e) {
            s.put("error", e.getMessage());
        }
        return s;
    }

    private Map<String, Object> buildNewProjectsSection(long since) {
        Map<String, Object> s = new LinkedHashMap<>();
        if (projectGenerationController == null) {
            s.put("available", false);
            return s;
        }
        try {
            Map<String, Object> response = projectGenerationController.listProjects();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> all = (List<Map<String, Object>>)
                response.getOrDefault("projects", Collections.emptyList());
            s.put("available", true);
            s.put("totalGenerated", all.size());

            // Projects created in last 24h
            List<Map<String, Object>> last24h = all.stream()
                .filter(p -> {
                    Object created = p.get("createdAt");
                    if (created == null) return false;
                    try {
                        LocalDateTime dt = LocalDateTime.parse(created.toString(),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        return dt.isAfter(LocalDateTime.now().minusHours(24));
                    } catch (Exception ex) { return false; }
                })
                .map(p -> {
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("projectId",    p.get("projectId"));
                    r.put("description",  p.get("description"));
                    r.put("status",       p.get("status"));
                    r.put("progress",     p.get("progress"));
                    r.put("templateType", p.get("templateType"));
                    r.put("createdAt",    p.get("createdAt"));
                    return r;
                })
                .collect(Collectors.toList());

            s.put("createdLast24h", last24h.size());
            s.put("projectDetails", last24h);

            long completed = all.stream()
                .filter(p -> "COMPLETE".equals(p.get("status")) || "PUSHED_TO_REPO".equals(p.get("status")))
                .count();
            s.put("completedTotal", completed);
        } catch (Exception e) {
            s.put("error", e.getMessage());
        }
        return s;
    }

    private Map<String, Object> buildAiCallsSection() {
        Map<String, Object> s = new LinkedHashMap<>();
        if (metricsService == null) {
            s.put("available", false);
            return s;
        }
        try {
            s.put("available", true);
            s.put("totalRequests", metricsService.getTotalRequests());
            s.put("totalErrors",   metricsService.getTotalErrors());
            Map<String, Object> health = metricsService.getSystemHealth();
            s.put("systemHealth",  health);
            Map<String, Object> genStats = metricsService.getGenerationStats();
            s.put("generationStats", genStats);
        } catch (Exception e) {
            s.put("error", e.getMessage());
        }
        return s;
    }

    // ── Event model ──────────────────────────────────────────────────────────

    public static class ActivityEvent {
        public final long timestampMs;
        public final String category;
        public final String title;
        public final String detail;

        public ActivityEvent(long ts, String category, String title, String detail) {
            this.timestampMs = ts;
            this.category = category;
            this.title = title;
            this.detail = detail;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("time",     LocalDateTime.ofInstant(Instant.ofEpochMilli(timestampMs), ZoneId.systemDefault())
                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            m.put("category", category);
            m.put("title",    title);
            m.put("detail",   detail);
            return m;
        }
    }
}
