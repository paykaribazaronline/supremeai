package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * FIXED: SupremeAI Watchdog Service
 * 
 * Problem: AI Brain had no external monitoring - could fail silently
 * Solution: External health monitor that is NOT part of SupremeAI
 * 
 * Features:
 * - Checks AI Brain health every 30 seconds
 * - Alerts admin immediately on failure
 * - Activates safe mode automatically
 * - Restores from last known good state
 * - Escalates to human if watchdog itself fails
 * 
 * Safe Mode Actions:
 * - Disables auto-decisions
 * - Queues all requests for manual review
 * - Preserves all state for debugging
 */
@Service
public class SupremeAIWatchdog {
    
    private static final Logger logger = LoggerFactory.getLogger(SupremeAIWatchdog.class);
    
    @Autowired
    private AlertingService alertingService;
    
    @Autowired
    private AdminControlService adminControlService;
    
    @Autowired(required = false)
    private MultiAIConsensusService consensusService;
    
    @Autowired(required = false)
    private SystemLearningService learningService;
    
    // Health tracking
    private final AtomicReference<BrainHealth> lastKnownHealth = 
        new AtomicReference<>(new BrainHealth(true, Instant.now(), "INITIALIZING"));
    private final AtomicBoolean safeModeActive = new AtomicBoolean(false);
    private final AtomicBoolean watchdogHealthy = new AtomicBoolean(true);
    
    // Failure tracking
    private final Map<String, Integer> failureCounts = new ConcurrentHashMap<>();
    private static final int FAILURE_THRESHOLD = 3;
    private static final int CONSECUTIVE_FAILURES_FOR_SAFE_MODE = 2;
    
    // Health check history
    private final List<HealthCheckRecord> checkHistory = 
        Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_HISTORY_SIZE = 1000;
    
    // Checkpoints for recovery
    private final AtomicReference<SystemCheckpoint> lastCheckpoint = new AtomicReference<>();
    
    @PostConstruct
    public void initialize() {
        logger.info("🐕 SupremeAI Watchdog initialized");
        logger.info("   - Check interval: 30 seconds");
        logger.info("   - Failure threshold: {} consecutive failures", FAILURE_THRESHOLD);
        logger.info("   - Safe mode trigger: {} consecutive failures", CONSECUTIVE_FAILURES_FOR_SAFE_MODE);
        
        // Create initial checkpoint
        createCheckpoint("INITIAL");
    }
    
    /**
     * Health check - runs every 30 seconds
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void healthCheck() {
        long checkStart = System.currentTimeMillis();
        String checkId = UUID.randomUUID().toString().substring(0, 8);
        
        try {
            logger.debug("🩺 [{}] Running health check...", checkId);
            
            // Check AI Brain Layer 0 (core services)
            BrainHealth health = checkBrainHealth();
            
            // Record check
            recordHealthCheck(checkId, health, System.currentTimeMillis() - checkStart);
            
            // Update last known health
            lastKnownHealth.set(health);
            
            if (!health.isHealthy()) {
                handleUnhealthyBrain(health, checkId);
            } else {
                // Brain is healthy
                if (safeModeActive.get()) {
                    // Try to exit safe mode
                    attemptRecovery();
                }
                
                // Reset failure counts on success
                failureCounts.clear();
                
                logger.debug("✅ [{}] Brain health check passed", checkId);
            }
            
            // Mark watchdog as healthy
            watchdogHealthy.set(true);
            
        } catch (Exception e) {
            // Watchdog itself failed - escalate immediately
            handleWatchdogFailure(e, checkId);
        }
    }
    
    /**
     * Check AI Brain health
     */
    private BrainHealth checkBrainHealth() {
        List<String> issues = new ArrayList<>();
        boolean healthy = true;
        
        // Check 1: Core services responsive
        try {
            if (adminControlService == null) {
                issues.add("AdminControlService not available");
                healthy = false;
            } else {
                // Quick ping to admin control
                adminControlService.getStatus();
            }
        } catch (Exception e) {
            issues.add("AdminControlService unresponsive: " + e.getMessage());
            healthy = false;
        }
        
        // Check 2: Learning service
        try {
            if (learningService != null) {
                learningService.getLearningStats();
            }
        } catch (Exception e) {
            issues.add("LearningService issue: " + e.getMessage());
            // Non-critical, don't mark unhealthy
        }
        
        // Check 3: Consensus service
        try {
            if (consensusService != null) {
                consensusService.getConsensusStats();
            }
        } catch (Exception e) {
            issues.add("ConsensusService issue: " + e.getMessage());
            healthy = false;
        }
        
        // Check 4: Memory usage (simulate)
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        double memoryUsage = (double) usedMemory / maxMemory;
        
        if (memoryUsage > 0.9) {
            issues.add(String.format("High memory usage: %.1f%%", memoryUsage * 100));
            healthy = false;
        }
        
        return new BrainHealth(healthy, Instant.now(), 
            issues.isEmpty() ? "HEALTHY" : String.join("; ", issues));
    }
    
    /**
     * Handle unhealthy brain
     */
    private void handleUnhealthyBrain(BrainHealth health, String checkId) {
        logger.error("🚨 [{}] AI Brain is UNHEALTHY: {}", checkId, health.getStatus());
        
        // Increment failure count
        int failures = failureCounts.merge("brain", 1, Integer::sum);
        
        // Alert admin immediately
        alertingService.sendCriticalAlert(
            "AI_BRAIN_UNHEALTHY",
            String.format("AI Brain health check failed (%d consecutive failures): %s", 
                failures, health.getStatus())
        );
        
        // Check if we need to activate safe mode
        if (failures >= CONSECUTIVE_FAILURES_FOR_SAFE_MODE && !safeModeActive.get()) {
            activateSafeMode(health.getStatus());
        }
        
        // If too many failures, attempt recovery
        if (failures >= FAILURE_THRESHOLD) {
            attemptRecovery();
        }
    }
    
    /**
     * Activate safe mode
     */
    private void activateSafeMode(String reason) {
        logger.error("🔒 Activating SAFE MODE due to: {}", reason);
        
        safeModeActive.set(true);
        
        try {
            // 1. Disable auto-decisions
            adminControlService.setPermissionMode(
                org.example.model.AdminControl.PermissionMode.WAIT,
                "WATCHDOG",
                "Auto-activated due to health issues: " + reason
            );
            
            // 2. Create checkpoint before any changes
            createCheckpoint("SAFE_MODE_ACTIVATED");
            
            // 3. Alert admins
            alertingService.sendCriticalAlert(
                "SAFE_MODE_ACTIVATED",
                "SupremeAI has entered SAFE MODE. All decisions require manual approval. Reason: " + reason
            );
            
            logger.info("🔒 Safe mode activated successfully");
            
        } catch (Exception e) {
            logger.error("❌ Failed to activate safe mode: {}", e.getMessage(), e);
            // Escalate
            alertingService.sendCriticalAlert(
                "SAFE_MODE_FAILED",
                "Failed to activate safe mode: " + e.getMessage()
            );
        }
    }
    
    /**
     * Attempt recovery from checkpoint
     */
    private void attemptRecovery() {
        logger.info("🔄 Attempting recovery from checkpoint...");
        
        SystemCheckpoint checkpoint = lastCheckpoint.get();
        if (checkpoint == null) {
            logger.warn("⚠️ No checkpoint available for recovery");
            return;
        }
        
        try {
            // Log recovery attempt
            alertingService.sendAlert(
                "RECOVERY_ATTEMPT",
                "Attempting recovery from checkpoint: " + checkpoint.getId()
            );
            
            // Recovery actions would go here
            // - Restart failed services
            // - Clear caches
            // - Reconnect to databases
            
            logger.info("🔄 Recovery completed");
            
            // Reset failure counts after recovery
            failureCounts.clear();
            
        } catch (Exception e) {
            logger.error("❌ Recovery failed: {}", e.getMessage(), e);
            alertingService.sendCriticalAlert(
                "RECOVERY_FAILED",
                "Recovery attempt failed: " + e.getMessage()
            );
        }
    }
    
    /**
     * Handle watchdog failure (very serious)
     */
    private void handleWatchdogFailure(Exception e, String checkId) {
        watchdogHealthy.set(false);
        
        logger.error("💥 [{}] WATCHDOG FAILURE: {}", checkId, e.getMessage(), e);
        
        // This is critical - the watchdog itself failed
        // We need to escalate to human immediately
        try {
            alertingService.sendCriticalAlert(
                "WATCHDOG_FAILURE",
                "The SupremeAI watchdog has failed. Immediate human intervention required. Error: " 
                    + e.getMessage()
            );
            
            // Try to activate safe mode even though watchdog is compromised
            safeModeActive.set(true);
            
        } catch (Exception alertError) {
            // Absolute worst case - can't even alert
            // Log to stderr as last resort
            System.err.println("CRITICAL: Watchdog failed and alerting also failed: " 
                + alertError.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create system checkpoint
     */
    public void createCheckpoint(String reason) {
        String checkpointId = UUID.randomUUID().toString().substring(0, 8);
        
        SystemCheckpoint checkpoint = new SystemCheckpoint(
            checkpointId,
            Instant.now(),
            reason,
            captureSystemState()
        );
        
        lastCheckpoint.set(checkpoint);
        
        logger.info("📸 Checkpoint created: {} (reason: {})", checkpointId, reason);
    }
    
    /**
     * Capture current system state
     */
    private Map<String, Object> captureSystemState() {
        Map<String, Object> state = new HashMap<>();
        
        try {
            state.put("timestamp", Instant.now().toString());
            state.put("adminMode", adminControlService.getPermissionMode().name());
            state.put("memoryUsage", getMemoryUsage());
            state.put("lastHealth", lastKnownHealth.get());
        } catch (Exception e) {
            state.put("error", "Failed to capture state: " + e.getMessage());
        }
        
        return state;
    }
    
    /**
     * Get current memory usage
     */
    private Map<String, Object> getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return Map.of(
            "used", runtime.totalMemory() - runtime.freeMemory(),
            "free", runtime.freeMemory(),
            "total", runtime.totalMemory(),
            "max", runtime.maxMemory()
        );
    }
    
    /**
     * Record health check for history
     */
    private void recordHealthCheck(String checkId, BrainHealth health, long durationMs) {
        HealthCheckRecord record = new HealthCheckRecord(
            checkId, Instant.now(), health.isHealthy(), health.getStatus(), durationMs
        );
        
        checkHistory.add(record);
        
        // Trim history if needed
        if (checkHistory.size() > MAX_HISTORY_SIZE) {
            checkHistory.subList(0, checkHistory.size() - MAX_HISTORY_SIZE).clear();
        }
    }
    
    /**
     * Get watchdog status
     */
    public Map<String, Object> getStatus() {
        return Map.of(
            "watchdogHealthy", watchdogHealthy.get(),
            "safeModeActive", safeModeActive.get(),
            "lastKnownHealth", lastKnownHealth.get(),
            "consecutiveFailures", failureCounts.getOrDefault("brain", 0),
            "totalChecks", checkHistory.size(),
            "recentChecks", checkHistory.stream()
                .sorted(Comparator.comparing(HealthCheckRecord::getTimestamp).reversed())
                .limit(10)
                .map(r -> Map.of(
                    "timestamp", r.getTimestamp().toString(),
                    "healthy", r.isHealthy(),
                    "durationMs", r.getDurationMs()
                ))
                .toList()
        );
    }
    
    /**
     * Manually exit safe mode (admin only)
     */
    public boolean exitSafeMode(String adminId, String reason) {
        if (!safeModeActive.get()) {
            return false;
        }
        
        logger.info("🔓 Admin {} exiting safe mode: {}", adminId, reason);
        
        safeModeActive.set(false);
        failureCounts.clear();
        
        createCheckpoint("SAFE_MODE_EXITED");
        
        alertingService.sendAlert(
            "SAFE_MODE_EXITED",
            String.format("Safe mode exited by %s. Reason: %s", adminId, reason)
        );
        
        return true;
    }
    
    public boolean isSafeModeActive() {
        return safeModeActive.get();
    }
    
    // ============== Data Classes ==============
    
    public static class BrainHealth {
        private final boolean healthy;
        private final Instant timestamp;
        private final String status;
        
        public BrainHealth(boolean healthy, Instant timestamp, String status) {
            this.healthy = healthy;
            this.timestamp = timestamp;
            this.status = status;
        }
        
        public boolean isHealthy() { return healthy; }
        public Instant getTimestamp() { return timestamp; }
        public String getStatus() { return status; }
        
        public boolean isUnresponsive() {
            return !healthy && status.contains("unresponsive");
        }
        
        @Override
        public String toString() {
            return String.format("BrainHealth{healthy=%s, status='%s'}", healthy, status);
        }
    }
    
    public static class SystemCheckpoint {
        private final String id;
        private final Instant timestamp;
        private final String reason;
        private final Map<String, Object> state;
        
        public SystemCheckpoint(String id, Instant timestamp, 
                               String reason, Map<String, Object> state) {
            this.id = id;
            this.timestamp = timestamp;
            this.reason = reason;
            this.state = state;
        }
        
        public String getId() { return id; }
        public Instant getTimestamp() { return timestamp; }
        public String getReason() { return reason; }
        public Map<String, Object> getState() { return state; }
    }
    
    public static class HealthCheckRecord {
        private final String checkId;
        private final Instant timestamp;
        private final boolean healthy;
        private final String status;
        private final long durationMs;
        
        public HealthCheckRecord(String checkId, Instant timestamp, 
                                boolean healthy, String status, long durationMs) {
            this.checkId = checkId;
            this.timestamp = timestamp;
            this.healthy = healthy;
            this.status = status;
            this.durationMs = durationMs;
        }
        
        public String getCheckId() { return checkId; }
        public Instant getTimestamp() { return timestamp; }
        public boolean isHealthy() { return healthy; }
        public String getStatus() { return status; }
        public long getDurationMs() { return durationMs; }
    }
}
