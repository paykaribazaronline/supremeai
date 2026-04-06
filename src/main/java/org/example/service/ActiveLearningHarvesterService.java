package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Active Learning Harvester
 *
 * Proactively pulls new knowledge every 6 hours from:
 *   1. Recently-closed GitHub bug issues in this repo
 *   2. Recently-merged GitHub PRs
 *   3. Recent failed CI/CD workflow logs
 *   4. Tavily web search for trending engineering patterns
 *   5. Wikipedia developer articles (no API key required)
 *   6. Hacker News top engineering stories (no API key required)
 *   7. DEV.to developer articles (no API key required)
 *   8. Stack Overflow trending questions (no API key required)
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

    /** Tavily web search topics — includes Oracle.com / cloud-native / ML topics */
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
        "Distributed systems CAP theorem trade-offs",
        // Oracle / Java ecosystem
        "site:oracle.com Java 21 new features",
        "site:oracle.com JDBC connection pool best practices",
        "site:oracle.com Oracle Database performance tuning",
        "Java virtual threads Project Loom guide",
        "Oracle Cloud OCI deployment best practices",
        // Cloud-native & DevOps
        "Google Cloud Run scaling configuration",
        "AWS Lambda cold start optimization Java",
        "Terraform infrastructure as code best practices",
        "OpenTelemetry distributed tracing Java Spring",
        // AI / ML engineering
        "LLM prompt engineering best practices developers",
        "RAG retrieval augmented generation implementation",
        "machine learning model deployment Java Spring Boot"
    );

    /** Wikipedia topics to harvest developer knowledge from */
    private static final List<String> WIKIPEDIA_TOPICS = Arrays.asList(
        "Microservices",
        "REST API",
        "GraphQL",
        "Docker (software)",
        "Kubernetes",
        "Java (programming language)",
        "Spring Framework",
        "Reactive programming",
        "Test-driven development",
        "Continuous integration",
        "DevOps",
        "Software design pattern",
        "SOLID (object-oriented design)",
        "Database index",
        "CAP theorem",
        "OAuth",
        "JSON Web Token",
        "Machine learning",
        "Large language model",
        "Cloud computing"
    );

    /** DEV.to article tags to harvest */
    private static final List<String> DEVTO_TAGS = Arrays.asList(
        "java", "javascript", "python", "devops", "security",
        "webdev", "docker", "kubernetes", "beginners", "tutorial",
        "react", "springboot", "api", "opensource", "cloud"
    );

    /** Stack Overflow tag groups to harvest trending questions from */
    private static final List<String> SO_TAGS = Arrays.asList(
        "java;spring-boot",
        "javascript;react",
        "python;django",
        "docker;kubernetes",
        "security;jwt",
        "firebase;firestore",
        "android;flutter",
        "sql;performance",
        "rest;api-design",
        "git;github-actions"
    );

    /** Max HN stories to inspect per harvest (fetched in order of top rank) */
    private static final int HN_STORY_LIMIT = 50;

    /** Minimum HN story score to be worth learning from */
    private static final int HN_MIN_SCORE = 50;

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

    /** Shared HTTP client for all no-key public APIs (Wikipedia, HN, DEV.to, Stack Overflow). */
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build();

    private static final ObjectMapper objectMapper = new ObjectMapper();

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

        AtomicInteger issuePatterns  = new AtomicInteger();
        AtomicInteger prPatterns     = new AtomicInteger();
        AtomicInteger ciPatterns     = new AtomicInteger();
        AtomicInteger webPatterns    = new AtomicInteger();
        AtomicInteger wikiPatterns   = new AtomicInteger();
        AtomicInteger hnPatterns     = new AtomicInteger();
        AtomicInteger devtoPatterns  = new AtomicInteger();
        AtomicInteger soPatterns     = new AtomicInteger();

        try { issuePatterns.set(harvestClosedIssues()); }
        catch (Exception e) { logger.warn("⚠️ Issue harvest failed: {}", e.getMessage()); }

        try { prPatterns.set(harvestMergedPRs()); }
        catch (Exception e) { logger.warn("⚠️ PR harvest failed: {}", e.getMessage()); }

        try { ciPatterns.set(harvestCIFailures()); }
        catch (Exception e) { logger.warn("⚠️ CI harvest failed: {}", e.getMessage()); }

        try { webPatterns.set(harvestWebSearch()); }
        catch (Exception e) { logger.warn("⚠️ Web search harvest failed: {}", e.getMessage()); }

        try { wikiPatterns.set(harvestWikipedia()); }
        catch (Exception e) { logger.warn("⚠️ Wikipedia harvest failed: {}", e.getMessage()); }

        try { hnPatterns.set(harvestHackerNews()); }
        catch (Exception e) { logger.warn("⚠️ Hacker News harvest failed: {}", e.getMessage()); }

        try { devtoPatterns.set(harvestDevTo()); }
        catch (Exception e) { logger.warn("⚠️ DEV.to harvest failed: {}", e.getMessage()); }

        try { soPatterns.set(harvestStackOverflow()); }
        catch (Exception e) { logger.warn("⚠️ Stack Overflow harvest failed: {}", e.getMessage()); }

        long elapsedMs = System.currentTimeMillis() - startMs;
        int total = issuePatterns.get() + prPatterns.get() + ciPatterns.get() + webPatterns.get()
                  + wikiPatterns.get() + hnPatterns.get() + devtoPatterns.get() + soPatterns.get();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("trigger", trigger);
        summary.put("timestamp", System.currentTimeMillis());
        summary.put("elapsed_ms", elapsedMs);
        summary.put("issue_patterns_learned", issuePatterns.get());
        summary.put("pr_patterns_learned", prPatterns.get());
        summary.put("ci_patterns_learned", ciPatterns.get());
        summary.put("web_patterns_learned", webPatterns.get());
        summary.put("wikipedia_patterns_learned", wikiPatterns.get());
        summary.put("hackernews_patterns_learned", hnPatterns.get());
        summary.put("devto_patterns_learned", devtoPatterns.get());
        summary.put("stackoverflow_patterns_learned", soPatterns.get());
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

    /**
     * Harvest developer knowledge from Wikipedia's Search API.
     * Fetches article summaries for a curated list of software engineering topics.
     * Wikipedia REST API is public and requires no authentication.
     */
    private int harvestWikipedia() {
        int learned = 0;
        for (String topic : WIKIPEDIA_TOPICS) {
            try {
                String encoded = URLEncoder.encode(topic, StandardCharsets.UTF_8);
                String url = "https://en.wikipedia.org/w/api.php?action=query&list=search"
                           + "&srsearch=" + encoded + "&srlimit=3&format=json&utf8=1";

                Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "SupremeAI-LearningHarvester/1.0 (educational bot)")
                    .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) continue;

                    JsonNode root = objectMapper.readTree(response.body().string());
                    JsonNode searchResults = root.path("query").path("search");

                    for (JsonNode item : searchResults) {
                        String title   = item.path("title").asText("").trim();
                        String snippet = stripHtml(item.path("snippet").asText("")).trim();
                        if (title.isEmpty() || snippet.isEmpty()) continue;

                        String category = detectCategory(title + " " + snippet);
                        systemLearningService.recordTechnique(
                            category,
                            abbreviate(title, 120),
                            abbreviate(snippet, 500),
                            Collections.singletonList("https://en.wikipedia.org/wiki/" +
                                URLEncoder.encode(title.replace(' ', '_'), StandardCharsets.UTF_8)),
                            0.55
                        );
                        learned++;
                    }
                }
            } catch (Exception e) {
                logger.debug("  ⚠️ Wikipedia fetch failed for '{}': {}", topic, e.getMessage());
            }
        }
        logger.info("  📖 Wikipedia: {} articles ingested", learned);
        return learned;
    }

    /**
     * Harvest top developer stories from Hacker News (news.ycombinator.com).
     * Uses the public HN Firebase REST API — no key required.
     * Only ingests stories with score >= {@value #HN_MIN_SCORE} to ensure quality.
     */
    private int harvestHackerNews() {
        int learned = 0;
        try {
            // Step 1: fetch the list of top story IDs
            Request listReq = new Request.Builder()
                .url("https://hacker-news.firebaseio.com/v0/topstories.json")
                .header("User-Agent", "SupremeAI-LearningHarvester/1.0")
                .build();

            List<Integer> storyIds = new ArrayList<>();
            try (Response listResp = httpClient.newCall(listReq).execute()) {
                if (listResp.isSuccessful() && listResp.body() != null) {
                    JsonNode arr = objectMapper.readTree(listResp.body().string());
                    for (JsonNode id : arr) {
                        storyIds.add(id.asInt());
                        if (storyIds.size() >= HN_STORY_LIMIT) break;
                    }
                }
            }

            // Step 2: fetch each story item and learn from qualifying ones
            for (int storyId : storyIds) {
                try {
                    Request itemReq = new Request.Builder()
                        .url("https://hacker-news.firebaseio.com/v0/item/" + storyId + ".json")
                        .header("User-Agent", "SupremeAI-LearningHarvester/1.0")
                        .build();

                    try (Response itemResp = httpClient.newCall(itemReq).execute()) {
                        if (!itemResp.isSuccessful() || itemResp.body() == null) continue;

                        JsonNode item = objectMapper.readTree(itemResp.body().string());
                        String type  = item.path("type").asText("");
                        int    score = item.path("score").asInt(0);
                        String title = item.path("title").asText("").trim();
                        String storyUrl = item.path("url").asText("").trim();

                        if (!"story".equals(type) || score < HN_MIN_SCORE || title.isEmpty()) continue;

                        String category = detectCategory(title);
                        String source   = storyUrl.isEmpty()
                            ? "https://news.ycombinator.com/item?id=" + storyId
                            : storyUrl;

                        systemLearningService.recordTechnique(
                            category,
                            abbreviate(title, 120),
                            "Hacker News top story (score=" + score + "): " + abbreviate(title, 200),
                            Collections.singletonList(source),
                            Math.min(0.95, 0.5 + (score / 1000.0)) // higher score → higher confidence
                        );
                        learned++;
                    }
                } catch (Exception e) {
                    logger.trace("  HN item {} fetch failed: {}", storyId, e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.debug("  ⚠️ HN top-stories list fetch failed: {}", e.getMessage());
        }
        logger.info("  📰 Hacker News: {} stories ingested", learned);
        return learned;
    }

    /**
     * Harvest developer articles from DEV.to (dev.to).
     * Uses the public DEV.to REST API — no API key required.
     * Fetches articles per tag to cover a wide range of dev topics.
     */
    private int harvestDevTo() {
        int learned = 0;
        for (String tag : DEVTO_TAGS) {
            try {
                String url = "https://dev.to/api/articles?tag=" +
                    URLEncoder.encode(tag, StandardCharsets.UTF_8) + "&per_page=5&top=7";

                Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "SupremeAI-LearningHarvester/1.0")
                    .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) continue;

                    JsonNode articles = objectMapper.readTree(response.body().string());
                    for (JsonNode article : articles) {
                        String title       = article.path("title").asText("").trim();
                        String description = article.path("description").asText("").trim();
                        String articleUrl  = article.path("url").asText("").trim();
                        if (title.isEmpty()) continue;

                        String snippet  = description.isEmpty() ? title : description;
                        String category = detectCategory(title + " " + description + " " + tag);

                        systemLearningService.recordTechnique(
                            category,
                            abbreviate(title, 120),
                            abbreviate(snippet, 500),
                            articleUrl.isEmpty() ? Collections.emptyList() : Collections.singletonList(articleUrl),
                            0.6
                        );
                        learned++;
                    }
                }
            } catch (Exception e) {
                logger.debug("  ⚠️ DEV.to fetch failed for tag '{}': {}", tag, e.getMessage());
            }
        }
        logger.info("  📝 DEV.to: {} articles ingested", learned);
        return learned;
    }

    /**
     * Harvest trending questions from Stack Overflow via the Stack Exchange API.
     * Uses the public, unauthenticated endpoint (300 req/day, no key needed).
     * Filters questions from the past week ordered by score.
     */
    private int harvestStackOverflow() {
        int learned = 0;
        for (String tagGroup : SO_TAGS) {
            try {
                String encodedTags = URLEncoder.encode(tagGroup, StandardCharsets.UTF_8);
                String url = "https://api.stackexchange.com/2.3/questions"
                           + "?order=desc&sort=week&site=stackoverflow"
                           + "&tagged=" + encodedTags
                           + "&pagesize=5&filter=default";

                Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "SupremeAI-LearningHarvester/1.0")
                    .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) continue;

                    JsonNode root  = objectMapper.readTree(response.body().string());
                    JsonNode items = root.path("items");

                    for (JsonNode q : items) {
                        String title    = q.path("title").asText("").trim();
                        int    score    = q.path("score").asInt(0);
                        String link     = q.path("link").asText("").trim();
                        if (title.isEmpty()) continue;

                        String category = detectCategory(title + " " + tagGroup.replace(';', ' '));

                        systemLearningService.recordPattern(
                            category,
                            "Stack Overflow: " + abbreviate(title, 120),
                            "Trending question (score=" + score + ") tagged [" + tagGroup + "]"
                                + (link.isEmpty() ? "" : " — " + link)
                        );
                        learned++;
                    }
                }
            } catch (Exception e) {
                logger.debug("  ⚠️ Stack Overflow fetch failed for tags '{}': {}", tagGroup, e.getMessage());
            }
        }
        logger.info("  💬 Stack Overflow: {} questions ingested", learned);
        return learned;
    }


    /** Coerce an Object to a non-null String. */
    private static String stringOf(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    /** Trim a String to at most {@code max} characters. */
    private static String abbreviate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    /** Strip HTML tags from a string (used to clean Wikipedia snippets). */
    private static String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", "").replaceAll("&[a-zA-Z0-9#]+;", " ").trim();
    }

    /**
     * Heuristic category detector based on keywords in the text.
     * Falls back to "general" when no keyword matches.
     */
    static String detectCategory(String text) {
        if (text == null) return "general";
        String t = text.toLowerCase(Locale.ROOT);
        if (t.contains("security") || t.contains("jwt") || t.contains("oauth") || t.contains("auth") || t.contains("vulnerability") || t.contains("xss") || t.contains("csrf")) return "security";
        if (t.contains("ci") || t.contains("cd") || t.contains("workflow") || t.contains("github action")) return "ci-cd";
        if (t.contains("docker") || t.contains("kubernetes") || t.contains("k8s") || t.contains("cloud run") || t.contains("terraform") || t.contains("ansible")) return "devops";
        if (t.contains("database") || t.contains("sql") || t.contains("firestore") || t.contains("mongo") || t.contains("redis") || t.contains("jdbc") || t.contains("oracle database") || t.contains("postgresql") || t.contains("mysql")) return "database";
        if (t.contains("performance") || t.contains("latency") || t.contains("throughput") || t.contains("memory") || t.contains("caching") || t.contains("optimiz")) return "performance";
        if (t.contains("test") || t.contains("junit") || t.contains("mock") || t.contains("coverage") || t.contains("tdd") || t.contains("bdd")) return "testing";
        if (t.contains("flutter") || t.contains("android") || t.contains("ios") || t.contains("mobile") || t.contains("kotlin") || t.contains("swift")) return "mobile";
        if (t.contains("react") || t.contains("frontend") || t.contains("css") || t.contains("html") || t.contains("vue") || t.contains("angular") || t.contains("typescript")) return "frontend";
        if (t.contains("spring") || t.contains("java") || t.contains("gradle") || t.contains("maven") || t.contains("jvm") || t.contains("virtual thread") || t.contains("loom")) return "backend";
        if (t.contains("architecture") || t.contains("design pattern") || t.contains("microservice") || t.contains("ddd") || t.contains("solid") || t.contains("hexagonal") || t.contains("event-driven")) return "architecture";
        if (t.contains("ai") || t.contains("llm") || t.contains("openai") || t.contains("gemini") || t.contains("claude") || t.contains("machine learning") || t.contains("neural") || t.contains("rag") || t.contains("prompt")) return "ai";
        if (t.contains("oracle") || t.contains("plsql") || t.contains("oci ") || t.contains("oracle cloud")) return "oracle";
        if (t.contains("linux") || t.contains("bash") || t.contains("shell") || t.contains("unix") || t.contains("systemd")) return "linux";
        if (t.contains("algorithm") || t.contains("data structure") || t.contains("sorting") || t.contains("graph") || t.contains("tree") || t.contains("hashmap")) return "data-structures";
        if (t.contains("api design") || t.contains("graphql") || t.contains("grpc") || t.contains("rest api") || t.contains("openapi") || t.contains("swagger")) return "api-design";
        return "general";
    }
}
