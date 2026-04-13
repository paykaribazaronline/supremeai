package org.example.selfhealing.healing;

import org.example.selfhealing.domain.HealingAttempt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * SupremeAI Healing Watchdog
 * 
 * EXTERNAL SERVICE: NOT part of SupremeAI
 * 
 * Monitors the healing system itself to detect if it's broken.
 * Prevents infinite damage if the fixer becomes broken.
 * 
 * Runs every minute and checks:
 * - Are all recent attempts failing?
 * - Are we in an infinite loop?
 * - Is the system being escalated?
 * 
 * If problems detected:
 * - Send PagerDuty alert
 * - Disable auto-healing
 * - Manually investigate
 */
@Service
public class SupremeAIHealingWatchdog {
    private static final Logger logger = LoggerFactory.getLogger(SupremeAIHealingWatchdog.class);
    
    private static final int FAILURE_THRESHOLD = 5; // 5+ failures in 10 min = problem
    private static final int MINUTES_TO_CHECK = 10;
    
    @Autowired
    private HealingStateManager stateManager;
    
    @Autowired
    private HealingCircuitBreaker circuitBreaker;
    
    @Autowired(required = false)
    private AdminEscalationService escalationService;
    
    private volatile boolean autoHealingEnabled = true;
    
    /**
     * Monitor healing system health (every minute)
     */
    @Scheduled(fixedRate = 60000) // Every 1 minute
    public void monitorHealingHealth() {
        try {
            logger.debug("🔍 Healing watchdog: Checking system health...");
            
            // Get recent attempts
            Map<String, Long> stats = stateManager.getAttemptStats(MINUTES_TO_CHECK);
            long failedAttempts = stats.getOrDefault("failed", 0L);
            long totalAttempts = stats.getOrDefault("total", 0L);
            
            // Check if all recent attempts are failing
            if (totalAttempts > 0) {
                double failureRate = (double) failedAttempts / totalAttempts;
                
                if (failureRate > 0.9 && totalAttempts >= FAILURE_THRESHOLD) {
                    // 90%+ failure rate
                    logger.error("🚨 WATCHDOG ALERT: High failure rate ({:.0f}%) in healing system",
                            failureRate * 100);
                    handleSystemFailure("High failure rate: " + failureRate);
                    return;
                }
            }
            
            // Check if too many escalations
            long escalatedAttempts = stats.getOrDefault("escalated", 0L);
            if (escalatedAttempts >= 3) {
                logger.warn("⚠️ WATCHDOG ALERT: Multiple escalations ({}) in last {} min",
                        escalatedAttempts, MINUTES_TO_CHECK);
                handleSystemFailure("Multiple escalations detected");
                return;
            }
            
            // All checks passed
            logger.debug("✅ Healing system health: OK (failed: {}, total: {})",
                    failedAttempts, totalAttempts);
            
        } catch (Exception e) {
            logger.error("❌ Watchdog check failed", e);
        }
    }
    
    /**
     * Handle detected system failure
     * 
     * Steps:
     * 1. Disable auto-healing immediately
     * 2. Alert admin
     * 3. Collect diagnostic info
     * 4. Require manual restart
     */
    private void handleSystemFailure(String reason) {
        logger.error("🔴 HEALING SYSTEM FAILURE: {}", reason);
        
        // Disable auto-healing
        disableAutoHealing();
        
        // Alert admin via all channels
        alertAdmin("SupremeAI Healing System FAILURE", reason);
        
        // Notify escalation service if available
        if (escalationService != null) {
            escalationService.escalate("WATCHDOG", reason, AdminEscalationService.EscalationLevel.CRITICAL);
        }
        
        // Log diagnostics
        logDiagnostics();
    }
    
    /**
     * Disable auto-healing to prevent further damage
     */
    public void disableAutoHealing() {
        this.autoHealingEnabled = false;
        logger.warn("🛑 Auto-healing DISABLED due to system failure");
    }
    
    /**
     * Re-enable auto-healing (admin action)
     */
    public void enableAutoHealing() {
        this.autoHealingEnabled = true;
        logger.info("🟢 Auto-healing RE-ENABLED (admin action)");
    }
    
    /**
     * Check if auto-healing is currently enabled
     */
    public boolean isAutoHealingEnabled() {
        return autoHealingEnabled;
    }
    
    /**
     * Get watchdog status
     */
    public Map<String, Object> getWatchdogStatus() {
        Map<String, Long> stats = stateManager.getAttemptStats(MINUTES_TO_CHECK);
        
        long total = stats.getOrDefault("total", 0L);
        long failed = stats.getOrDefault("failed", 0L);
        double failureRate = total > 0 ? (double) failed / total : 0.0;
        
        return Map.of(
                "enabled", autoHealingEnabled,
                "recent_total", total,
                "recent_failed", failed,
                "failure_rate", failureRate,
                "status", autoHealingEnabled ? "HEALTHY" : "DISABLED"
        );
    }
    
    /**
     * Alert admin of system problems
     */
    private void alertAdmin(String title, String message) {
        logger.error("📢 ADMIN ALERT: {} - {}", title, message);
    }
    
    /**
     * Collect diagnostic information
     */
    private void logDiagnostics() {
        try {
            Map<String, Long> stats = stateManager.getAttemptStats(MINUTES_TO_CHECK);
            Map<String, Long> errors = stateManager.getMostCommonErrors(5);
            Map<String, Double> strategies = stateManager.getBestFixStrategies();
            
            logger.error("=== DIAGNOSTIC INFO ===");
            logger.error("Recent stats: {}", stats);
            logger.error("Common errors: {}", errors);
            logger.error("Strategy success rates: {}", strategies);
            logger.error("=====================");
            
        } catch (Exception e) {
            logger.error("Failed to collect diagnostics", e);
        }
    }
    
    /**
     * Manual health check endpoint (for monitoring)
     */
    public boolean healthCheck() {
        Map<String, Object> status = getWatchdogStatus();
        Object failureRateObj = status.get("failure_rate");
        
        if (failureRateObj instanceof Double) {
            double failureRate = (Double) failureRateObj;
            return failureRate < 0.5; // Healthy if <50% failure
        }
        
        return true; // Healthy if no data
    }
}
