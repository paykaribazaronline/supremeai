package org.example.service;

import java.util.*;

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
    
    // Puppeteer node script path
    private static final String PUPPETEER_SCRIPT = "scripts/puppeteer-collector.js";
    
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
     * 1. Start Node.js subprocess
     * 2. Load Puppeteer script
     * 3. Navigate to URL
     * 4. Wait for dynamic content
     * 5. Extract data
     * 6. Return JSON
     */
    private BrowserScrapedData executePuppeteerScript(String url) throws Exception {
        // TODO: Implement actual Puppeteer execution
        // For now: Mock implementation
        
        BrowserScrapedData data = new BrowserScrapedData();
        data.url = url;
        data.title = "Mock Title";
        data.content = "Mock content from browser scrape";
        data.scrapedAt = new Date();
        data.puppeteerUsed = true;
        
        // In production:
        // ProcessBuilder pb = new ProcessBuilder("node", PUPPETEER_SCRIPT, url);
        // pb.redirectErrorStream(true);
        // Process process = pb.start();
        // BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        // String line;
        // StringBuilder output = new StringBuilder();
        // while ((line = reader.readLine()) != null) {
        //     output.append(line);
        // }
        // int exitCode = process.waitFor();
        // if (exitCode != 0) throw new Exception("Puppeteer failed with code " + exitCode);
        // Parse output JSON and return
        
        return data;
    }
    
    /**
     * Log browser usage for audit trail
     */
    private void logBrowserUsage(String url, String status) {
        // TODO: Log to Firebase
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
        
        return stats;
    }
    
    // ========== DATA CLASSES ==========
    
    public static class BrowserScrapedData {
        public String url;
        public String title;
        public String content;
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
        
        @Override
        public String toString() {
            return String.format("BrowserStats: %d%% used, %d remaining", 
                quotaUsed, remaining);
        }
    }
}
