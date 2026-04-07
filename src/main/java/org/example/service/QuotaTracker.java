package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quota Tracker - Monitor free tier API limits
 * 
 * Quotas:
 * - GitHub API: 5000 req/hour (authenticated)
 * - Vercel API: 100 req/day
 * - Firebase API: 50K reads/day
 * - Puppeteer: 10 req/day (browser-based)
 */
public class QuotaTracker {
    
    // Quota definitions
    private static final Map<String, QuotaPolicy> QUOTAS = Map.ofEntries(
        Map.entry("github", new QuotaPolicy("GitHub API", 5000, QuotaWindow.HOURLY)),
        Map.entry("vercel", new QuotaPolicy("Vercel API", 100, QuotaWindow.DAILY)),
        Map.entry("firebase", new QuotaPolicy("Firebase API", 50000, QuotaWindow.DAILY)),
        Map.entry("puppeteer", new QuotaPolicy("Puppeteer Browser", 10, QuotaWindow.DAILY))
    );
    
    // Usage tracking per service
    private final Map<String, UsageWindow> usageTracking = new ConcurrentHashMap<>();
    
    // Firebase for persistence
    private final FirebaseService firebaseService;

    // Disk persistence
    private final LocalJsonStoreService jsonStore;
    private static final String QUOTA_USAGE_PATH = "quota-tracker/usage.json";
    
    public enum QuotaWindow {
        HOURLY, DAILY, MONTHLY
    }
    
    // ========== STRUCTURE ==========
    
    public static class QuotaPolicy {
        final String serviceName;
        final int limit;
        final QuotaWindow window;
        
        public QuotaPolicy(String serviceName, int limit, QuotaWindow window) {
            this.serviceName = serviceName;
            this.limit = limit;
            this.window = window;
        }
    }
    
    public static class UsageWindow {
        String service;
        LocalDateTime windowStart;
        int currentUsage;
        int quota;
        QuotaWindow window;
        
        public UsageWindow(String service, int quota, QuotaWindow window) {
            this.service = service;
            this.quota = quota;
            this.window = window;
            this.windowStart = LocalDateTime.now();
            this.currentUsage = 0;
        }
    }
    
    public QuotaTracker(FirebaseService firebase, LocalJsonStoreService jsonStore) {
        this.firebaseService = firebase;
        this.jsonStore = jsonStore;
        initializeTracking();
        restoreUsageFromDisk();
    }
    
    // ========== INITIALIZATION ==========
    
    private void initializeTracking() {
        for (Map.Entry<String, QuotaPolicy> entry : QUOTAS.entrySet()) {
            String service = entry.getKey();
            QuotaPolicy policy = entry.getValue();
            usageTracking.put(service, new UsageWindow(service, policy.limit, policy.window));
        }
        System.out.println("📊 Quota Tracker initialized for " + QUOTAS.size() + " services");
    }
    
    // ========== QUOTA CHECKING ==========
    
    /**
     * ✅ Can we use this service?
     */
    public boolean canUseService(String service) {
        UsageWindow usage = usageTracking.get(service);
        if (usage == null) return false;
        
        // Check if window expired
        if (isWindowExpired(usage)) {
            resetWindow(usage);
        }
        
        boolean allowed = usage.currentUsage < usage.quota;
        
        if (!allowed) {
            System.out.println("⚠️ " + service + " quota exceeded: " 
                             + usage.currentUsage + "/" + usage.quota);
        }
        
        return allowed;
    }
    
    /**
     * Get remaining quota
     */
    public int getRemainingQuota(String service) {
        UsageWindow usage = usageTracking.get(service);
        if (usage == null) return 0;
        
        if (isWindowExpired(usage)) {
            resetWindow(usage);
        }
        
        return Math.max(0, usage.quota - usage.currentUsage);
    }
    
    /**
     * Get usage percentage (0-100)
     */
    public double getUsagePercentage(String service) {
        UsageWindow usage = usageTracking.get(service);
        if (usage == null || usage.quota == 0) return 0;
        
        if (isWindowExpired(usage)) {
            resetWindow(usage);
        }
        
        return ((double) usage.currentUsage / usage.quota) * 100;
    }
    
    /**
     * Record API call
     */
    public void recordUsage(String service, int count) {
        UsageWindow usage = usageTracking.get(service);
        if (usage == null) return;
        
        if (isWindowExpired(usage)) {
            resetWindow(usage);
        }
        
        usage.currentUsage += count;
        System.out.println("📈 " + service + ": " + usage.currentUsage + "/" + usage.quota);
        
        // Save to Firebase for persistence
        saveUsageToFirebase(usage);
        
        // Check alerts
        checkQuotaAlerts(usage);
    }
    
    /**
     * Get status for all services
     */
    public Map<String, QuotaStatus> getAllStatus() {
        Map<String, QuotaStatus> status = new LinkedHashMap<>();
        
        for (Map.Entry<String, UsageWindow> entry : usageTracking.entrySet()) {
            UsageWindow usage = entry.getValue();
            
            if (isWindowExpired(usage)) {
                resetWindow(usage);
            }
            
            QuotaStatus qs = new QuotaStatus();
            qs.service = entry.getKey();
            qs.limit = usage.quota;
            qs.used = usage.currentUsage;
            qs.remaining = getRemainingQuota(entry.getKey());
            qs.percentage = getUsagePercentage(entry.getKey());
            qs.window = usage.window.toString();
            qs.windowStart = usage.windowStart;
            
            status.put(entry.getKey(), qs);
        }
        
        return status;
    }
    
    // ========== HELPER METHODS ==========
    
    private boolean isWindowExpired(UsageWindow usage) {
        LocalDateTime now = LocalDateTime.now();
        
        switch (usage.window) {
            case HOURLY:
                return now.minusHours(1).isAfter(usage.windowStart);
            case DAILY:
                return now.minusDays(1).isAfter(usage.windowStart);
            case MONTHLY:
                return now.minusMonths(1).isAfter(usage.windowStart);
            default:
                return false;
        }
    }
    
    private void resetWindow(UsageWindow usage) {
        System.out.println("🔄 Resetting " + usage.service + " quota window");
        usage.windowStart = LocalDateTime.now();
        usage.currentUsage = 0;
    }
    
    private void saveUsageToFirebase(UsageWindow usage) {
        persistUsageToDisk();
    }

    private void persistUsageToDisk() {
        try {
            Map<String, Map<String, Object>> data = new LinkedHashMap<>();
            for (Map.Entry<String, UsageWindow> entry : usageTracking.entrySet()) {
                UsageWindow uw = entry.getValue();
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("service", uw.service);
                m.put("currentUsage", uw.currentUsage);
                m.put("quota", uw.quota);
                m.put("window", uw.window.name());
                m.put("windowStart", uw.windowStart.toString());
                data.put(entry.getKey(), m);
            }
            if (jsonStore != null) jsonStore.write(QUOTA_USAGE_PATH, data);
        } catch (Exception e) {
            System.out.println("\u26a0\ufe0f Failed to persist quota usage: " + e.getMessage());
        }
    }

    private void restoreUsageFromDisk() {
        try {
            if (jsonStore == null) return;
            Map<String, Map<String, Object>> saved = jsonStore.read(
                    QUOTA_USAGE_PATH,
                    new TypeReference<Map<String, Map<String, Object>>>() {},
                    Map.of());
            for (Map.Entry<String, Map<String, Object>> entry : saved.entrySet()) {
                UsageWindow uw = usageTracking.get(entry.getKey());
                if (uw != null) {
                    Map<String, Object> m = entry.getValue();
                    uw.currentUsage = m.get("currentUsage") != null ? ((Number) m.get("currentUsage")).intValue() : 0;
                    if (m.get("windowStart") != null) {
                        try {
                            uw.windowStart = LocalDateTime.parse(String.valueOf(m.get("windowStart")));
                        } catch (Exception ignored) {}
                    }
                    // Check if window expired — if so, reset
                    if (isWindowExpired(uw)) {
                        resetWindow(uw);
                    }
                }
            }
            System.out.println("\u2705 QuotaTracker restored usage from disk for " + saved.size() + " services");
        } catch (Exception e) {
            System.out.println("\u26a0\ufe0f Could not restore quota usage: " + e.getMessage());
        }
    }
    
    private void checkQuotaAlerts(UsageWindow usage) {
        double percentage = getUsagePercentage(usage.service);
        
        if (percentage >= 95) {
            alertCritical(usage);
        } else if (percentage >= 80) {
            alertWarning(usage);
        }
    }
    
    private void alertWarning(UsageWindow usage) {
        System.out.println("⚠️ WARNING: " + usage.service + " at " 
                         + String.format("%.0f%%", getUsagePercentage(usage.service)) 
                         + " quota");
        
        // TODO: Send admin notification
        // emailService.sendAlert("admin@supremeai.com", 
        //     "Quota Warning: " + usage.service,
        //     "Service " + usage.service + " is at " + getUsagePercentage(usage.service) + "% quota");
    }
    
    private void alertCritical(UsageWindow usage) {
        System.out.println("🚨 CRITICAL: " + usage.service + " at " 
                         + String.format("%.0f%%", getUsagePercentage(usage.service)) 
                         + " quota");
        
        // TODO: Send urgent admin notification
        // emailService.sendUrgentAlert("admin@supremeai.com",
        //     "CRITICAL: " + usage.service + " Quota Exhausted!",
        //     "Service " + usage.service + " has reached critical quota levels");
    }
    
    // ========== PUPPETEER SPECIAL HANDLING ==========
    
    /**
     * Should we fallback to Puppeteer (browser)?
     * Only if:
     * 1. API failed
     * 2. Puppeteer quota available
     * 3. Not in critical state
     */
    public boolean shouldFallbackToBrowser() {
        if (!canUseService("puppeteer")) {
            System.out.println("🚫 Puppeteer quota exhausted");
            return false;
        }
        
        // Check if we're in warning/critical state
        double percentage = getUsagePercentage("puppeteer");
        if (percentage >= 80) {
            System.out.println("⚠️ Puppeteer near limit (" + String.format("%.0f%%", percentage) + ") - use sparingly");
            // Can still use, but log warning
        }
        
        return true;
    }
    
    /**
     * Try API first, fallback to browser if needed
     */
    public boolean tryAPIThenBrowser(String primaryService) {
        // Try primary service first
        if (canUseService(primaryService)) {
            return true;
        }
        
        // Primary failed, try fallback
        System.out.println("🔄 " + primaryService + " unavailable, checking browser fallback...");
        return shouldFallbackToBrowser();
    }
    
    // ========== REPORTING ==========
    
    public static class QuotaStatus {
        public String service;
        public int limit;
        public int used;
        public int remaining;
        public double percentage;
        public String window;
        public LocalDateTime windowStart;
        
        @Override
        public String toString() {
            return String.format("%s: %d/%d (%.0f%%) [%s]",
                service, used, limit, percentage, window);
        }
    }
    
    public void printQuotaReport() {
        System.out.println("\n📊 ===== QUOTA REPORT =====\n");
        
        for (Map.Entry<String, QuotaStatus> entry : getAllStatus().entrySet()) {
            QuotaStatus status = entry.getValue();
            System.out.println(status);
            
            // Visual representation
            int barLength = 20;
            int filled = (int) (status.percentage / 100 * barLength);
            String bar = "█".repeat(filled) + "░".repeat(barLength - filled);
            System.out.println("  [" + bar + "]");
            System.out.println();
        }
    }
}
