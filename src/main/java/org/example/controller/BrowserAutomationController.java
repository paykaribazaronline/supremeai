package org.example.controller;

import org.example.service.BrowserDataCollector;
import org.example.service.DataCollectorService;
import org.example.service.HybridDataCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Browser Automation Controller - REST API for browser-based data collection
 * 
 * Endpoints:
 *   POST /api/browser/scrape         - Scrape a URL via Puppeteer
 *   GET  /api/browser/stats          - Browser quota/usage stats
 *   GET  /api/browser/health         - Subsystem health check
 *   GET  /api/browser/audit          - Recent scrape audit log
 *   GET  /api/data/github/{o}/{r}    - GitHub repo data (hybrid)
 *   GET  /api/data/vercel/{id}       - Vercel project data (hybrid)
 *   GET  /api/data/firebase          - Firebase status (hybrid)
 *   GET  /api/data/health            - Data collector health
 */
@RestController
public class BrowserAutomationController {
    private static final Logger logger = LoggerFactory.getLogger(BrowserAutomationController.class);

    @Autowired
    private BrowserDataCollector browserDataCollector;

    @Autowired
    private DataCollectorService dataCollectorService;

    @Autowired
    private HybridDataCollector hybridDataCollector;

    // ========== BROWSER SCRAPING ==========

    /**
     * POST /api/browser/scrape - Scrape a URL using Puppeteer
     * Body: { "url": "https://example.com" }
     */
    @PostMapping("/api/browser/scrape")
    public ResponseEntity<Map<String, Object>> scrapeUrl(@RequestBody Map<String, String> request) {
        String url = request.get("url");
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Missing 'url' in request body",
                "example", Map.of("url", "https://example.com")
            ));
        }

        logger.info("🌐 Browser scrape requested: {}", url);

        try {
            BrowserDataCollector.BrowserScrapedData result = browserDataCollector.scrapeWithPuppeteer(url);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("url", result.url);
            response.put("title", result.title);
            response.put("content", result.content != null ? result.content.substring(0, Math.min(result.content.length(), 5000)) : "");
            response.put("description", result.description);
            response.put("statusCode", result.statusCode);
            response.put("linkCount", result.linkCount);
            response.put("wordCount", result.wordCount);
            response.put("scrapedAt", result.scrapedAt);
            response.put("puppeteerUsed", result.puppeteerUsed);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("❌ Browser scrape failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Scrape failed: " + e.getMessage(),
                "url", url
            ));
        }
    }

    /**
     * POST /api/browser/scrape-auth - Scrape URL with optional auth bootstrap.
     *
     * Body example:
     * {
     *   "url": "https://example.com/private",
     *   "auth": {
     *     "type": "bearer",
     *     "token": "..."
     *   }
     * }
     */
    @PostMapping("/api/browser/scrape-auth")
    public ResponseEntity<Map<String, Object>> scrapeUrlWithAuth(@RequestBody Map<String, Object> request) {
        String url = String.valueOf(request.getOrDefault("url", "")).trim();
        if (url.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Missing 'url' in request body"
            ));
        }

        BrowserDataCollector.BrowserAuthOptions authOptions = null;
        Object authRaw = request.get("auth");
        if (authRaw instanceof Map<?, ?> authMap) {
            authOptions = new BrowserDataCollector.BrowserAuthOptions();
            authOptions.type = getAsString(authMap, "type");
            authOptions.username = getAsString(authMap, "username");
            authOptions.password = getAsString(authMap, "password");
            authOptions.token = getAsString(authMap, "token");
            authOptions.cookieHeader = getAsString(authMap, "cookieHeader");
            authOptions.loginUrl = getAsString(authMap, "loginUrl");
            authOptions.usernameSelector = getAsString(authMap, "usernameSelector");
            authOptions.passwordSelector = getAsString(authMap, "passwordSelector");
            authOptions.submitSelector = getAsString(authMap, "submitSelector");
        }

        logger.info("🌐 Auth browser scrape requested: {}", url);

        try {
            BrowserDataCollector.BrowserScrapedData result = browserDataCollector.scrapeWithPuppeteer(url, authOptions);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("url", result.url);
            response.put("title", result.title);
            response.put("content", result.content != null ? result.content.substring(0, Math.min(result.content.length(), 5000)) : "");
            response.put("description", result.description);
            response.put("statusCode", result.statusCode);
            response.put("linkCount", result.linkCount);
            response.put("wordCount", result.wordCount);
            response.put("scrapedAt", result.scrapedAt);
            response.put("puppeteerUsed", result.puppeteerUsed);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("❌ Auth browser scrape failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Scrape failed: " + e.getMessage(),
                "url", url
            ));
        }
    }

    /**
     * GET /api/browser/stats - Browser quota and usage statistics
     */
    @GetMapping("/api/browser/stats")
    public ResponseEntity<Map<String, Object>> getBrowserStats() {
        BrowserDataCollector.BrowserStats stats = browserDataCollector.getStats();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("quotaUsedPercent", stats.quotaUsed);
        response.put("remaining", stats.remaining);
        response.put("warningLevel", stats.warningLevel);
        response.put("criticalLevel", stats.criticalLevel);
        response.put("recentScrapeCount", stats.recentScrapeCount);
        response.put("puppeteerAvailable", browserDataCollector.isPuppeteerAvailable());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/browser/health - Browser subsystem health
     */
    @GetMapping("/api/browser/health")
    public ResponseEntity<Map<String, Object>> getBrowserHealth() {
        BrowserDataCollector.BrowserStats stats = browserDataCollector.getStats();
        boolean puppeteerInstalled = browserDataCollector.isPuppeteerAvailable();

        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", puppeteerInstalled && !stats.criticalLevel ? "healthy" : "degraded");
        health.put("puppeteerInstalled", puppeteerInstalled);
        health.put("quotaRemaining", stats.remaining);
        health.put("quotaWarning", stats.warningLevel);
        health.put("quotaCritical", stats.criticalLevel);

        return ResponseEntity.ok(health);
    }

    /**
     * GET /api/browser/audit - Recent scrape audit log
     */
    @GetMapping("/api/browser/audit")
    public ResponseEntity<Map<String, Object>> getAuditLog() {
        List<Map<String, Object>> recent = browserDataCollector.getRecentScrapes();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("count", recent.size());
        response.put("scrapes", recent);

        return ResponseEntity.ok(response);
    }

    // ========== DATA COLLECTOR (HYBRID) ==========

    /**
     * GET /api/data/github/{owner}/{repo} - GitHub repo data (API-first, browser fallback)
     */
    @GetMapping("/api/data/github/{owner}/{repo}")
    public ResponseEntity<Map<String, Object>> getGitHubData(
            @PathVariable String owner, @PathVariable String repo) {
        logger.info("📊 Data request: GitHub {}/{}", owner, repo);

        try {
            Map<String, Object> data = dataCollectorService.getGitHubData(owner, repo);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            logger.error("❌ GitHub data fetch failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "error", e.getMessage(),
                "source", "github",
                "target", owner + "/" + repo
            ));
        }
    }

    /**
     * GET /api/data/vercel/{projectId} - Vercel project data
     */
    @GetMapping("/api/data/vercel/{projectId}")
    public ResponseEntity<Map<String, Object>> getVercelData(@PathVariable String projectId) {
        logger.info("📊 Data request: Vercel {}", projectId);

        try {
            Map<String, Object> data = dataCollectorService.getVercelStatus(projectId);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            logger.error("❌ Vercel data fetch failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "error", e.getMessage(),
                "source", "vercel"
            ));
        }
    }

    /**
     * GET /api/data/firebase - Firebase project status
     */
    @GetMapping("/api/data/firebase")
    public ResponseEntity<Map<String, Object>> getFirebaseData() {
        logger.info("📊 Data request: Firebase status");

        try {
            Map<String, Object> data = dataCollectorService.getFirebaseStatus();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            logger.error("❌ Firebase data fetch failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "error", e.getMessage(),
                "source", "firebase"
            ));
        }
    }

    /**
     * GET /api/data/health - Full data collector system health
     */
    @GetMapping("/api/data/health")
    public ResponseEntity<Map<String, Object>> getDataCollectorHealth() {
        try {
            Map<String, Object> health = dataCollectorService.getSystemHealth();
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }

    /**
     * GET /api/data/stats - Request statistics
     */
    @GetMapping("/api/data/stats")
    public ResponseEntity<Map<String, Object>> getRequestStats() {
        try {
            Map<String, Object> stats = dataCollectorService.getRequestStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }

    private String getAsString(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
