package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Active Learning Harvester
 *
 * Proactively pulls new knowledge every 6 hours from:
 *   1. Recently-closed GitHub bug issues in this repo
 *   2. Recently-merged GitHub PRs
 *   3. Recent failed CI/CD workflow logs
 *   4. Tavily web search for trending engineering patterns
 *
 * All discoveries are persisted via {@link SystemLearningService}.
 *
 * The harvester also exposes {@link #runHarvest(String)} so it can be
 * triggered on-demand from the REST API or GitHub Actions.
 */
@Service
public class ActiveLearningHarvesterService {

    private static final Logger logger = LoggerFactory.getLogger(ActiveLearningHarvesterService.class);

    /** 6-hour fixed-rate schedule (ms) */
    private static final long HARVEST_INTERVAL_MS = 6 * 60 * 60 * 1_000L;

    /** Number of recent issues / PRs to inspect per harvest cycle */
    private static final int GITHUB_ITEM_LIMIT = 20;

    /** Number of Tavily search topics to run per harvest cycle */
    private static final List<String> SEARCH_TOPICS = Arrays.asList(
        "Spring Boot best practices 2025",
        "Java microservices performance optimization",
        "Firebase Firestore query optimization patterns",
        "Flutter state management best practices",
        "Kubernetes pod crash loop debugging",
        "GitHub Actions CI failure common fixes",
        "React performance optimization hooks",
        "JWT security best practices",
        "Docker multi-stage build optimization",
        "Distributed systems CAP theorem trade-offs"
    );

    // ── Dependencies ──────────────────────────────────────────────────────────

    @Autowired
    private GitHubAPIService gitHubAPIService;

    @Autowired
    private SystemLearningService systemLearningService;

    /** Tavily API key — resolved from the environment via Spring properties */
    @Value("${tavily.api.key:}")
    private String tavilyApiKey;

    /** Created once after injection so we don't allocate a new instance every harvest cycle. */
    private InternetSearchService searchService;

    @javax.annotation.PostConstruct
    void initSearchService() {
        searchService = new InternetSearchService(tavilyApiKey != null ? tavilyApiKey : "");
    }

    // ── Scheduled entry point ─────────────────────────────────────────────────

    /**
     * Runs automatically every 6 hours.
     * All errors are caught internally so the scheduler is never disrupted.
     */
    @Scheduled(fixedRate = HARVEST_INTERVAL_MS, initialDelay = 60_000)
    public void scheduledHarvest() {
        runHarvest("scheduled");
    }

    // ── Public API (also used by REST controller) ─────────────────────────────

    /**
     * Execute one full harvest cycle.
     *
     * @param trigger  label recorded in logs (e.g. "scheduled", "api", "webhook")
     * @return summary map with counts of items learned
     */
    public Map<String, Object> runHarvest(String trigger) {
        logger.info("🌱 [ActiveLearningHarvester] Starting harvest — trigger={}", trigger);
        long startMs = System.currentTimeMillis();

        AtomicInteger issuePatterns = new AtomicInteger();
        AtomicInteger prPatterns    = new AtomicInteger();
        AtomicInteger ciPatterns    = new AtomicInteger();
        AtomicInteger webPatterns   = new AtomicInteger();

        try { issuePatterns.set(harvestClosedIssues()); }
        catch (Exception e) { logger.warn("⚠️ Issue harvest failed: {}", e.getMessage()); }

        try { prPatterns.set(harvestMergedPRs()); }
        catch (Exception e) { logger.warn("⚠️ PR harvest failed: {}", e.getMessage()); }

        try { ciPatterns.set(harvestCIFailures()); }
        catch (Exception e) { logger.warn("⚠️ CI harvest failed: {}", e.getMessage()); }

        try { webPatterns.set(harvestWebSearch()); }
        catch (Exception e) { logger.warn("⚠️ Web search harvest failed: {}", e.getMessage()); }

        long elapsedMs = System.currentTimeMillis() - startMs;
        int total = issuePatterns.get() + prPatterns.get() + ciPatterns.get() + webPatterns.get();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("trigger", trigger);
        summary.put("timestamp", System.currentTimeMillis());
        summary.put("elapsed_ms", elapsedMs);
        summary.put("issue_patterns_learned", issuePatterns.get());
        summary.put("pr_patterns_learned", prPatterns.get());
        summary.put("ci_patterns_learned", ciPatterns.get());
        summary.put("web_patterns_learned", webPatterns.get());
        summary.put("total_learned", total);

        logger.info("✅ [ActiveLearningHarvester] Done — {} patterns learned in {}ms", total, elapsedMs);
        return summary;
    }

    // ── Private harvest methods ───────────────────────────────────────────────

    /**
     * Harvest bug/fix patterns from recently-closed GitHub issues.
     */
    private int harvestClosedIssues() {
        List<Map<String, Object>> issues = gitHubAPIService.getRecentClosedIssues(GITHUB_ITEM_LIMIT);
        int learned = 0;
        for (Map<String, Object> issue : issues) {
            String title = stringOf(issue.get("title"));
            String body  = stringOf(issue.get("body"));
            if (title.isEmpty()) continue;

            String problem    = title;
            String rootCause  = body.isEmpty() ? "See GitHub issue for details" : abbreviate(body, 500);
            String fix        = "Issue closed — see linked PR or commit for resolution";
            String category   = detectCategory(title + " " + body);

            systemLearningService.learnFromIncident(
                category, problem, rootCause, fix,
                Collections.singletonList("Review closed issue before implementing similar feature"),
                0.65,
                Map.of("source", "github-issue", "trigger", "active-harvest")
            );
            learned++;
        }
        logger.info("  📌 Issues: {} bug patterns ingested", learned);
        return learned;
    }

    /**
     * Harvest improvement patterns from recently-merged PRs.
     */
    private int harvestMergedPRs() {
        List<Map<String, Object>> prs = gitHubAPIService.getRecentMergedPRs(GITHUB_ITEM_LIMIT);
        int learned = 0;
        for (Map<String, Object> pr : prs) {
            String title = stringOf(pr.get("title"));
            String body  = stringOf(pr.get("body"));
            // Only learn from merged PRs (merged_at is non-null)
            if (title.isEmpty() || pr.get("merged_at") == null) continue;

            String category = detectCategory(title + " " + body);
            String pattern  = "PR merged: " + title;
            String detail   = body.isEmpty() ? "No description provided" : abbreviate(body, 400);

            systemLearningService.recordPattern(
                category, pattern,
                "Merged PR — " + detail
            );
            learned++;
        }
        logger.info("  🔀 PRs: {} improvement patterns ingested", learned);
        return learned;
    }

    /**
     * Harvest failure patterns from recent failed CI/CD workflow runs.
     */
    private int harvestCIFailures() {
        List<Map<String, Object>> runs = gitHubAPIService.getRecentWorkflowRuns(10);
        int learned = 0;
        for (Map<String, Object> run : runs) {
            String conclusion = stringOf(run.get("conclusion"));
            if (!"failure".equalsIgnoreCase(conclusion)) continue;

            String name    = stringOf(run.get("name"));
            String runId   = stringOf(run.get("id"));
            String problem = "CI failure in workflow: " + name;
            String logs    = runId.isEmpty() ? "" : abbreviate(gitHubAPIService.getWorkflowLogs(runId), 600);

            systemLearningService.learnFromIncident(
                "ci-cd",
                problem,
                logs.isEmpty() ? "Logs unavailable" : logs,
                "Investigate and fix failing workflow step",
                Arrays.asList(
                    "Check GitHub Actions run for detailed error",
                    "Ensure dependencies are pinned",
                    "Run tests locally before pushing"
                ),
                0.7,
                Map.of("source", "github-actions", "workflow", name, "trigger", "active-harvest")
            );
            learned++;
        }
        logger.info("  🔴 CI: {} failure patterns ingested", learned);
        return learned;
    }

    /**
     * Harvest engineering best-practices via Tavily web search.
     * Gracefully skips if Tavily key is not configured.
     */
    private int harvestWebSearch() {
        if (tavilyApiKey == null || tavilyApiKey.isBlank()) {
            logger.debug("  🔍 Tavily key not configured — skipping web search harvest");
            return 0;
        }

        int learned = 0;

        for (String topic : SEARCH_TOPICS) {
            try {
                List<InternetSearchService.SearchResult> results = searchService.search(topic);
                for (InternetSearchService.SearchResult result : results) {
                    if (result.snippet == null || result.snippet.isBlank()) continue;

                    String category = detectCategory(topic + " " + result.title);
                    systemLearningService.recordTechnique(
                        category,
                        abbreviate(result.title, 120),
                        abbreviate(result.snippet, 500),
                        Collections.singletonList(result.url),
                        0.6
                    );
                    learned++;
                }
            } catch (Exception e) {
                logger.debug("  ⚠️ Search failed for '{}': {}", topic, e.getMessage());
            }
        }
        logger.info("  🔍 Web: {} technique entries ingested", learned);
        return learned;
    }

    // ── Utility helpers ───────────────────────────────────────────────────────

    /** Coerce an Object to a non-null String. */
    private static String stringOf(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    /** Trim a String to at most {@code max} characters. */
    private static String abbreviate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    /**
     * Heuristic category detector based on keywords in the text.
     * Falls back to "general" when no keyword matches.
     */
    static String detectCategory(String text) {
        if (text == null) return "general";
        String t = text.toLowerCase(Locale.ROOT);
        if (t.contains("security") || t.contains("jwt") || t.contains("oauth") || t.contains("auth")) return "security";
        if (t.contains("ci") || t.contains("cd") || t.contains("workflow") || t.contains("github action")) return "ci-cd";
        if (t.contains("docker") || t.contains("kubernetes") || t.contains("k8s") || t.contains("cloud run")) return "devops";
        if (t.contains("database") || t.contains("sql") || t.contains("firestore") || t.contains("mongo") || t.contains("redis")) return "database";
        if (t.contains("performance") || t.contains("latency") || t.contains("throughput") || t.contains("memory")) return "performance";
        if (t.contains("test") || t.contains("junit") || t.contains("mock") || t.contains("coverage")) return "testing";
        if (t.contains("flutter") || t.contains("android") || t.contains("ios") || t.contains("mobile")) return "mobile";
        if (t.contains("react") || t.contains("frontend") || t.contains("css") || t.contains("html")) return "frontend";
        if (t.contains("spring") || t.contains("java") || t.contains("gradle") || t.contains("maven")) return "backend";
        if (t.contains("architecture") || t.contains("design pattern") || t.contains("microservice") || t.contains("ddd")) return "architecture";
        if (t.contains("ai") || t.contains("llm") || t.contains("openai") || t.contains("gemini") || t.contains("claude")) return "ai";
        return "general";
    }
}
