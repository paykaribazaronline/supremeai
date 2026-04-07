package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Browser Data Collector - Puppeteer Fallback
 * 
 * EMERGENCY ONLY (10 req/day limit)
 * 
 * Use cases:
 * - API failure
 * - Rate limit exceeded
 * - Authentication error
 * 
 * Browser automation via Puppeteer (Node.js)
 * - Scrapes web pages
 * - Gets dynamic content
 * - Takes screenshots
 */
public class BrowserDataCollector {
    
    private final QuotaTracker quotaTracker;
    private final FirebaseService firebaseService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Puppeteer node script path
    private static final String PUPPETEER_SCRIPT = "scripts/puppeteer-collector.js";
    private static final long PROCESS_TIMEOUT_SECONDS = 45;
    
    // Track recent scrapes for audit
    private final List<Map<String, Object>> recentScrapes = Collections.synchronizedList(new ArrayList<>());
    
    public BrowserDataCollector(QuotaTracker quotaTracker, FirebaseService firebase) {
        this.quotaTracker = quotaTracker;
        this.firebaseService = firebase;
    }
    
    /**
     * Fallback: Use Puppeteer to scrape data
     * 
     * Called when APIs fail
     * Limited to 10 req/day
     */
    public BrowserScrapedData scrapeWithPuppeteer(String url) throws Exception {
        // Validate URL before doing anything
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL cannot be empty");
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("Only http/https URLs are allowed");
        }
        
        // Check quota first
        if (!quotaTracker.shouldFallbackToBrowser()) {
            throw new Exception("Puppeteer quota exhausted - manual intervention needed");
        }
        
        System.out.println("🌐 Fallback: Scraping with Puppeteer: " + url);
        
        try {
            BrowserScrapedData data = executePuppeteerScript(url);
            
            quotaTracker.recordUsage("puppeteer", 1);
            
            System.out.println("✅ Browser scraping successful");
            
            // Log to Firebase for audit trail
            logBrowserUsage(url, "success");
            
            return data;
            
        } catch (Exception e) {
            System.err.println("❌ Puppeteer error: " + e.getMessage());
            logBrowserUsage(url, "failed: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Execute Puppeteer via Node.js subprocess
     * 
     * Structure:
     * 1. Start Node.js subprocess with validated args
     * 2. Capture stdout (JSON) and stderr separately
     * 3. Enforce timeout to prevent hangs
     * 4. Parse JSON output into BrowserScrapedData
     */
    private BrowserScrapedData executePuppeteerScript(String url) throws Exception {
        // Use array args to prevent command injection
        ProcessBuilder pb = new ProcessBuilder("node", PUPPETEER_SCRIPT, url);
        pb.directory(new File(System.getProperty("user.dir")));
        // Do NOT merge stderr into stdout — keep them separate
        pb.redirectErrorStream(false);
        
        Process process = pb.start();
        
        // Read stdout (JSON output)
        StringBuilder stdout = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stdout.append(line);
            }
        }
        
        // Read stderr (error/debug messages)
        StringBuilder stderr = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stderr.append(line).append("\n");
            }
        }
        
        // Wait with timeout
        boolean finished = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new Exception("Puppeteer timed out after " + PROCESS_TIMEOUT_SECONDS + "s");
        }
        
        int exitCode = process.exitValue();
        String output = stdout.toString().trim();
        String errors = stderr.toString().trim();
        
        if (!errors.isEmpty()) {
            System.err.println("⚠️ Puppeteer stderr: " + errors);
        }
        
        // Parse JSON output
        if (output.isEmpty()) {
            throw new Exception("Puppeteer returned empty output (exit code " + exitCode + ")"
                + (errors.isEmpty() ? "" : " stderr: " + errors));
        }
        
        JsonNode json = objectMapper.readTree(output);
        
        BrowserScrapedData data = new BrowserScrapedData();
        data.url = json.path("url").asText(url);
        data.title = json.path("title").asText("");
        data.content = json.path("content").asText("");
        data.description = json.path("description").asText("");
        data.statusCode = json.path("statusCode").asInt(0);
        data.linkCount = json.path("linkCount").asInt(0);
        data.wordCount = json.path("wordCount").asInt(0);
        data.scrapedAt = new Date();
        data.puppeteerUsed = true;
        
        // Check if Puppeteer reported its own error
        if (!json.path("success").asBoolean(false)) {
            data.errorMessage = json.path("error").asText("Unknown browser error");
            throw new Exception("Puppeteer scrape failed: " + data.errorMessage);
        }
        
        return data;
    }
    
    /**
     * Log browser usage for audit trail
     */
    private void logBrowserUsage(String url, String status) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("url", url);
        entry.put("status", status);
        entry.put("timestamp", new Date().toString());
        
        // Keep last 50 entries in memory
        if (recentScrapes.size() >= 50) {
            recentScrapes.remove(0);
        }
        recentScrapes.add(entry);
        
        // Persist to Firebase
        try {
            if (firebaseService != null) {
                firebaseService.saveSystemConfig("browser_audit/" + System.currentTimeMillis(), entry);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to log browser usage to Firebase: " + e.getMessage());
        }
        
        System.out.println("📝 Logging browser usage: " + url + " - " + status);
    }
    
    /**
     * Get browser scraping stats
     */
    public BrowserStats getStats() {
        BrowserStats stats = new BrowserStats();
        stats.quotaUsed = (int) quotaTracker.getUsagePercentage("puppeteer");
        stats.remaining = quotaTracker.getRemainingQuota("puppeteer");
        stats.warningLevel = stats.quotaUsed >= 80;
        stats.criticalLevel = stats.quotaUsed >= 95;
        stats.recentScrapeCount = recentScrapes.size();
        
        return stats;
    }
    
    /**
     * Get recent scrape audit log
     */
    public List<Map<String, Object>> getRecentScrapes() {
        return new ArrayList<>(recentScrapes);
    }
    
    /**
     * Check if Puppeteer (Node.js) is available on this system
     */
    public boolean isPuppeteerAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("node", "-e",
                "try{require('puppeteer');console.log('ok')}catch(e){console.log('missing')}");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line = r.readLine();
                p.waitFor(5, TimeUnit.SECONDS);
                return "ok".equals(line);
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    // ========== DATA CLASSES ==========
    
    public static class BrowserScrapedData {
        public String url;
        public String title;
        public String content;
        public String description;
        public int statusCode;
        public int linkCount;
        public int wordCount;
        public Date scrapedAt;
        public boolean puppeteerUsed;
        public String errorMessage;
        
        @Override
        public String toString() {
            return String.format("BrowserScrape(%s): %s", url, 
                errorMessage != null ? "ERROR: " + errorMessage : title);
        }
    }
    
    public static class BrowserStats {
        public int quotaUsed;
        public int remaining;
        public boolean warningLevel;
        public boolean criticalLevel;
        public int recentScrapeCount;
        
        @Override
        public String toString() {
            return String.format("BrowserStats: %d%% used, %d remaining, %d recent", 
                quotaUsed, remaining, recentScrapeCount);
        }
    }
}
