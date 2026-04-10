package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Internet Research Service - FREE autonomous learning from public sources.
 * No paid AI APIs required. Pulls knowledge from:
 * 1. GitHub Trending repos + README analysis
 * 2. Stack Overflow top voted answers
 * 3. Hacker News best stories (tech discussions)
 * 4. DEV.to trending articles
 * 5. Wikipedia tech summaries
 * 
 * Runs every 2 hours, learns ~5-15 new things per cycle.
 * Rate-limited to stay within free API tiers.
 */
@Service
public class InternetResearchService {
    private static final Logger logger = LoggerFactory.getLogger(InternetResearchService.class);

    @Autowired(required = false)
    private SystemLearningService learningService;

    @Autowired
    private SystemModeService systemModeService;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    // Track what we already learned to avoid duplicates
    private final Set<String> learnedKeys = ConcurrentHashMap.newKeySet();
    private final AtomicInteger totalLearned = new AtomicInteger(0);
    private final AtomicInteger cycleCount = new AtomicInteger(0);

    // Research topics rotate each cycle
    private static final String[][] GITHUB_TOPICS = {
        {"java", "spring-boot"}, {"python", "fastapi"}, {"typescript", "react"},
        {"flutter", "dart"}, {"rust", "go"}, {"kotlin", "android"},
        {"docker", "kubernetes"}, {"machine-learning", "llm"},
        {"devops", "terraform"}, {"security", "authentication"},
        {"nextjs", "nestjs"}, {"postgresql", "redis"}
    };

    private static final String[] SO_TAGS = {
        "java", "spring-boot", "python", "javascript", "typescript",
        "react", "flutter", "docker", "kubernetes", "postgresql",
        "firebase", "git", "rest", "security", "performance"
    };

    // ─── Scheduled: Every 2 hours ──────────────────────────────────────────
    @Scheduled(fixedDelay = 7200000, initialDelay = 120000) // 2hr, start after 2min
    public void runResearchCycle() {
        SystemModeService.OperationDecision decision =
                systemModeService.canExecuteOperation("UPDATE_TECHNICAL_KNOWLEDGE", 90);
        if (!decision.isAllowed()) {
            logger.info("⏸️ Research cycle skipped by system mode: {}", decision.getReason());
            return;
        }

        int cycle = cycleCount.incrementAndGet();
        logger.info("🔬 Internet Research Cycle #{} starting...", cycle);
        int learned = 0;

        try {
            learned += researchGitHubTrending(cycle);
        } catch (Exception e) {
            logger.warn("GitHub research failed: {}", e.getMessage());
        }

        try {
            learned += researchStackOverflow(cycle);
        } catch (Exception e) {
            logger.warn("StackOverflow research failed: {}", e.getMessage());
        }

        try {
            learned += researchHackerNews();
        } catch (Exception e) {
            logger.warn("HackerNews research failed: {}", e.getMessage());
        }

        try {
            learned += researchDevTo(cycle);
        } catch (Exception e) {
            logger.warn("DEV.to research failed: {}", e.getMessage());
        }

        totalLearned.addAndGet(learned);
        logger.info("🔬 Research Cycle #{} complete — learned {} new items (total lifetime: {})",
                cycle, learned, totalLearned.get());
    }

    // ─── GitHub Trending Repos ─────────────────────────────────────────────
    private int researchGitHubTrending(int cycle) throws Exception {
        int topicIdx = (cycle - 1) % GITHUB_TOPICS.length;
        String[] topics = GITHUB_TOPICS[topicIdx];
        int learned = 0;

        for (String topic : topics) {
            String url = "https://api.github.com/search/repositories?q=" + topic
                    + "+stars:>1000&sort=updated&per_page=5";
            JsonNode root = fetchJson(url);
            if (root == null || !root.has("items")) continue;

            for (JsonNode repo : root.get("items")) {
                String name = safeText(repo, "full_name");
                String desc = safeText(repo, "description");
                int stars = repo.has("stargazers_count") ? repo.get("stargazers_count").asInt() : 0;
                String lang = safeText(repo, "language");
                String key = "github:" + name;

                if (desc.isEmpty() || learnedKeys.contains(key)) continue;

                String topicsStr = "";
                if (repo.has("topics") && repo.get("topics").isArray()) {
                    List<String> t = new ArrayList<>();
                    repo.get("topics").forEach(n -> t.add(n.asText()));
                    topicsStr = String.join(", ", t);
                }

                recordLearning(
                    "GITHUB_TRENDING",
                    String.format("[%s] %s — %s (⭐ %d)", lang, name, desc, stars),
                    Arrays.asList(
                        "Trending repo: " + name + " with " + stars + " stars",
                        "Language: " + lang + ", Topics: " + topicsStr,
                        "Study its architecture and patterns for " + topic + " projects"
                    ),
                    0.75,
                    key
                );
                learned++;
            }
            pause(1500); // rate limit: GitHub allows 10 unauthenticated/min
        }
        return learned;
    }

    // ─── Stack Overflow Top Answers ────────────────────────────────────────
    private int researchStackOverflow(int cycle) throws Exception {
        int tagIdx = (cycle - 1) % SO_TAGS.length;
        String tag = SO_TAGS[tagIdx];
        int learned = 0;

        // Get top voted questions from last 30 days
        String url = "https://api.stackexchange.com/2.3/questions?order=desc&sort=votes"
                + "&tagged=" + tag + "&site=stackoverflow&filter=withbody&pagesize=5";
        JsonNode root = fetchJson(url);
        if (root == null || !root.has("items")) return 0;

        for (JsonNode q : root.get("items")) {
            String title = safeText(q, "title");
            int score = q.has("score") ? q.get("score").asInt() : 0;
            boolean answered = q.has("is_answered") && q.get("is_answered").asBoolean();
            String key = "so:" + q.get("question_id").asText();

            if (score < 5 || !answered || learnedKeys.contains(key)) continue;

            // Extract clean text from body (strip HTML tags)
            String body = safeText(q, "body");
            String cleanBody = body.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ");
            if (cleanBody.length() > 500) cleanBody = cleanBody.substring(0, 500) + "...";

            recordLearning(
                "STACKOVERFLOW",
                String.format("[%s] Q: %s (score: %d)", tag, decodeHtml(title), score),
                Arrays.asList(
                    "High-voted SO question about " + tag,
                    "Problem: " + cleanBody,
                    "This is a common issue developers face — learn the pattern"
                ),
                Math.min(0.7 + (score / 100.0), 0.95),
                key
            );
            learned++;
        }
        pause(1000); // SO allows 300 requests/day without key
        return learned;
    }

    // ─── Hacker News Best Stories ──────────────────────────────────────────
    private int researchHackerNews() throws Exception {
        int learned = 0;
        String url = "https://hacker-news.firebaseio.com/v0/beststories.json";
        JsonNode ids = fetchJson(url);
        if (ids == null || !ids.isArray()) return 0;

        // Check top 10 stories
        int checked = 0;
        for (JsonNode idNode : ids) {
            if (checked++ >= 10) break;
            long id = idNode.asLong();
            String key = "hn:" + id;
            if (learnedKeys.contains(key)) continue;

            String storyUrl = "https://hacker-news.firebaseio.com/v0/item/" + id + ".json";
            JsonNode story = fetchJson(storyUrl);
            if (story == null) continue;

            String title = safeText(story, "title");
            int score = story.has("score") ? story.get("score").asInt() : 0;
            String storyLink = safeText(story, "url");

            if (score < 100 || !isTechRelated(title)) continue;

            recordLearning(
                "HACKER_NEWS",
                String.format("HN trending: %s (score: %d)", title, score),
                Arrays.asList(
                    "Trending on Hacker News with " + score + " points",
                    "Source: " + storyLink,
                    "Research this topic further for emerging tech trends"
                ),
                0.70,
                key
            );
            learned++;
            pause(200); // HN API is very generous
        }
        return learned;
    }

    // ─── DEV.to Trending Articles ──────────────────────────────────────────
    private int researchDevTo(int cycle) throws Exception {
        int learned = 0;
        String[] devTags = {"java", "python", "javascript", "devops", "security",
                            "react", "flutter", "docker", "kubernetes", "ai"};
        String tag = devTags[(cycle - 1) % devTags.length];

        String url = "https://dev.to/api/articles?tag=" + tag + "&top=7&per_page=5";
        JsonNode articles = fetchJson(url);
        if (articles == null || !articles.isArray()) return 0;

        for (JsonNode article : articles) {
            String title = safeText(article, "title");
            String desc = safeText(article, "description");
            int reactions = article.has("positive_reactions_count")
                    ? article.get("positive_reactions_count").asInt() : 0;
            String key = "devto:" + article.get("id").asText();

            if (reactions < 10 || learnedKeys.contains(key)) continue;

            String tags = "";
            if (article.has("tag_list") && article.get("tag_list").isArray()) {
                List<String> t = new ArrayList<>();
                article.get("tag_list").forEach(n -> t.add(n.asText()));
                tags = String.join(", ", t);
            }

            recordLearning(
                "DEVTO_ARTICLE",
                String.format("[%s] %s — %s", tag, title, desc),
                Arrays.asList(
                    "Popular DEV.to article (" + reactions + " reactions)",
                    "Tags: " + tags,
                    "Extract key practices and patterns from this article"
                ),
                Math.min(0.65 + (reactions / 200.0), 0.90),
                key
            );
            learned++;
        }
        pause(1000); // DEV.to rate limit gentle
        return learned;
    }

    // ─── Helper: Record learning via SystemLearningService ─────────────────
    private void recordLearning(String category, String content,
                                List<String> insights, double confidence, String key) {
        if (learningService == null) {
            logger.warn("SystemLearningService not available — skipping");
            return;
        }
        try {
            learningService.recordTechnique(
                category,
                content.length() > 120 ? content.substring(0, 120) : content,
                content,
                insights,
                confidence,
                Map.of("source", "internet_research", "key", key)
            );
            learnedKeys.add(key);
        } catch (Exception e) {
            logger.warn("Failed to record learning [{}]: {}", key, e.getMessage());
        }
    }

    // ─── HTTP fetch ────────────────────────────────────────────────────────
    private JsonNode fetchJson(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .header("User-Agent", "SupremeAI-Research/1.0")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return mapper.readTree(response.body());
            }
            logger.debug("HTTP {} from {}", response.statusCode(), url);
            return null;
        } catch (Exception e) {
            logger.debug("Fetch failed [{}]: {}", url, e.getMessage());
            return null;
        }
    }

    private String safeText(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText("") : "";
    }

    private String decodeHtml(String s) {
        return s.replace("&amp;", "&").replace("&lt;", "<")
                .replace("&gt;", ">").replace("&#39;", "'").replace("&quot;", "\"");
    }

    private boolean isTechRelated(String title) {
        String lower = title.toLowerCase();
        String[] keywords = {"programming", "software", "code", "api", "database", "cloud",
            "ai", "ml", "react", "java", "python", "rust", "go", "docker", "kubernetes",
            "security", "performance", "open source", "framework", "developer", "engineering",
            "deploy", "devops", "linux", "server", "microservice", "startup", "tech",
            "algorithm", "data", "machine learning", "typescript", "llm", "gpu"};
        for (String kw : keywords) {
            if (lower.contains(kw)) return true;
        }
        return false;
    }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    // ─── Admin stats ───────────────────────────────────────────────────────
    public Map<String, Object> getResearchStats() {
        return Map.of(
            "totalLearned", totalLearned.get(),
            "cyclesCompleted", cycleCount.get(),
            "uniqueSourcesTracked", learnedKeys.size(),
            "sources", List.of("GitHub Trending", "Stack Overflow", "Hacker News", "DEV.to"),
            "scheduleIntervalHours", 2,
            "status", cycleCount.get() > 0 ? "ACTIVE" : "WAITING_FIRST_CYCLE"
        );
    }
}
