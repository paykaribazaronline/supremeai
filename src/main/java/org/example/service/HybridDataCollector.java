package org.example.service;

import java.util.*;

/**
 * Hybrid Data Collector
 * 
 * 🎯 CORE LOGIC:
 * 1. Try API (free, fast, preferred)
 * 2. If API fails → Try Browser fallback (limited)
 * 3. If both fail → Alert admin, return error
 * 
 * 📊 Usage Strategy:
 * - API: 99% of requests
 * - Browser: 1% of requests (emergency only)
 */
public class HybridDataCollector {
    
    private final APIDataCollector apiCollector;
    private final BrowserDataCollector browserCollector;
    private final QuotaTracker quotaTracker;
    private final FirebaseService firebaseService;
    
    // Metrics
    private final CollectorMetrics metrics = new CollectorMetrics();
    
    public HybridDataCollector(APIDataCollector api, BrowserDataCollector browser,
                               QuotaTracker quota, FirebaseService firebase) {
        this.apiCollector = api;
        this.browserCollector = browser;
        this.quotaTracker = quota;
        this.firebaseService = firebase;
    }
    
    // ========== MAIN ENTRY POINT ==========
    
    /**
     * 🚀 Try API first, fallback to browser
     * 
     * Strategy: API-Primary, Browser-Fallback
     */
    public HybridResult collectGitHubData(String owner, String repo) {
        HybridResult result = new HybridResult();
        result.type = "GitHub";
        result.target = owner + "/" + repo;
        result.timestamp = new Date();
        
        try {
            // Step 1: Try API (99% success path)
            try {
                System.out.println("📡 Attempting GitHub API...");
                APIDataCollector.GitHubRepoData apiData = apiCollector.getGitHubRepoInfo(owner, repo);
                
                result.success = true;
                result.dataSource = "API";
                result.data = apiData;
                result.error = null;
                
                metrics.apiSuccess++;
                System.out.println("✅ API Success");
                
                return result;
                
            } catch (Exception apiError) {
                System.out.println("⚠️ API failed: " + apiError.getMessage());
                metrics.apiFailed++;
                
                // Step 2: Try fallback (browser) if API failed
                String fallbackUrl = "https://github.com/" + owner + "/" + repo;
                if (quotaTracker.shouldFallbackToBrowser()) {
                    
                    System.out.println("🔄 Attempting Browser fallback...");
                    try {
                        BrowserDataCollector.BrowserScrapedData browserData = 
                            browserCollector.scrapeWithPuppeteer(fallbackUrl);
                        
                        result.success = true;
                        result.dataSource = "BROWSER";
                        result.data = browserData;
                        result.fallbackUsed = true;
                        result.error = null;
                        
                        metrics.browserSuccess++;
                        System.out.println("✅ Browser Fallback Success");
                        
                        return result;
                        
                    } catch (Exception browserError) {
                        System.out.println("❌ Browser also failed: " + browserError.getMessage());
                        metrics.browserFailed++;
                        
                        result.success = false;
                        result.error = "Both API and browser failed: " + apiError.getMessage();
                        result.fallbackUsed = true;
                        
                        alertAdminAllFailed(result);
                        return result;
                    }
                } else {
                    // Browser quota exhausted
                    result.success = false;
                    result.error = "API failed and browser quota exhausted";
                    
                    alertAdminAllFailed(result);
                    return result;
                }
            }
            
        } catch (Exception e) {
            result.success = false;
            result.error = "Unexpected error: " + e.getMessage();
            return result;
        }
    }
    
    /**
     * Collect Vercel deployment status (API only - no browser fallback needed)
     */
    public HybridResult collectVercelStatus(String projectId) {
        HybridResult result = new HybridResult();
        result.type = "Vercel";
        result.target = projectId;
        result.timestamp = new Date();
        
        try {
            APIDataCollector.VercelDeploymentData data = 
                apiCollector.getVercelDeploymentStatus(projectId);
            
            result.success = true;
            result.dataSource = "API";
            result.data = data;
            
            metrics.apiSuccess++;
            return result;
            
        } catch (Exception e) {
            result.success = false;
            result.error = e.getMessage();
            metrics.apiFailed++;
            return result;
        }
    }
    
    /**
     * Collect Firebase project status
     */
    public HybridResult collectFirebaseStatus() {
        HybridResult result = new HybridResult();
        result.type = "Firebase";
        result.target = "Current Project";
        result.timestamp = new Date();
        
        try {
            APIDataCollector.FirebaseProjectData data = 
                apiCollector.getFirebaseProjectStatus();
            
            result.success = true;
            result.dataSource = "API";
            result.data = data;
            
            metrics.apiSuccess++;
            return result;
            
        } catch (Exception e) {
            result.success = false;
            result.error = e.getMessage();
            metrics.apiFailed++;
            return result;
        }
    }
    
    // ========== ALERTS & NOTIFICATIONS ==========
    
    private void alertAdminAllFailed(HybridResult result) {
        String message = "🚨 CRITICAL: Data collection failed\n" +
                        "Type: " + result.type + "\n" +
                        "Target: " + result.target + "\n" +
                        "Error: " + result.error + "\n" +
                        "Manual intervention needed";
        
        System.out.println(message);
        
        // TODO: Send email to admin
        // emailService.sendUrgentAlert("admin@supremeai.com",
        //     "Data Collection Failed: " + result.type,
        //     message);
        
        // TODO: Log to Firebase
        // firebaseService.logAlert({type: "DATA_COLLECTION_FAILED", ...});
    }
    
    // ========== MONITORING & HEALTH ==========
    
    /**
     * Get system health status
     */
    public CollectorHealth getHealth() {
        CollectorHealth health = new CollectorHealth();
        health.metrics = metrics;
        health.quotas = quotaTracker.getAllStatus();
        health.browserStats = browserCollector.getStats();
        
        // Calculate health score
        long total = metrics.apiSuccess + metrics.apiFailed;
        if (total > 0) {
            health.successRate = ((double) metrics.apiSuccess / total) * 100;
        }
        
        // Determine status
        if (health.successRate >= 95) {
            health.status = "HEALTHY";
        } else if (health.successRate >= 80) {
            health.status = "DEGRADED";
        } else {
            health.status = "CRITICAL";
        }
        
        return health;
    }
    
    /**
     * Print comprehensive report
     */
    public void printReport() {
        CollectorHealth health = getHealth();
        
        System.out.println("\n📊 ===== HYBRID DATA COLLECTOR REPORT =====\n");
        System.out.println("Status: " + health.status);
        System.out.println("Success Rate: " + String.format("%.1f%%", health.successRate));
        System.out.println("\n📈 Metrics:");
        System.out.println("  API Calls: " + metrics.apiSuccess + " success, " 
                         + metrics.apiFailed + " failed");
        System.out.println("  Browser Calls: " + metrics.browserSuccess + " success, " 
                         + metrics.browserFailed + " failed");
        System.out.println("  Fallbacks Used: " + metrics.fallbackCount);
        
        System.out.println("\n📊 Quota Status:");
        for (Map.Entry<String, QuotaTracker.QuotaStatus> entry : health.quotas.entrySet()) {
            System.out.println("  " + entry.getValue());
        }
        
        System.out.println("\n🌐 Browser Status:");
        System.out.println("  " + health.browserStats);
    }
    
    // ========== DATA CLASSES ==========
    
    public static class HybridResult {
        public boolean success;
        public String type;           // GitHub, Vercel, Firebase
        public String target;         // owner/repo or projectId
        public String dataSource;     // API or BROWSER
        public Object data;           // Actual data
        public String error;
        public boolean fallbackUsed;
        public Date timestamp;
        
        @Override
        public String toString() {
            return String.format("[%s] %s: %s via %s", 
                type, target, success ? "✅" : "❌", dataSource);
        }
    }
    
    public static class CollectorMetrics {
        public long apiSuccess = 0;
        public long apiFailed = 0;
        public long browserSuccess = 0;
        public long browserFailed = 0;
        public long fallbackCount = 0;
    }
    
    public static class CollectorHealth {
        public String status;         // HEALTHY, DEGRADED, CRITICAL
        public double successRate;
        public CollectorMetrics metrics;
        public Map<String, QuotaTracker.QuotaStatus> quotas;
        public BrowserDataCollector.BrowserStats browserStats;
    }
}
