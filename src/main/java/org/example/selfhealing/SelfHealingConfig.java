package org.example.selfhealing;

/**
 * Configuration for Self-Healing System
 * 
 * Defines constants, thresholds, and policies for:
 * - Circuit breaker behavior
 * - Retry strategies
 * - Health check intervals
 * - Recovery procedures
 * - Monitoring thresholds
 */
public class SelfHealingConfig {
    
    // ===== CIRCUIT BREAKER CONFIG =====
    public static final int CIRCUIT_BREAKER_FAILURE_THRESHOLD = 5;  // failures before opening
    public static final long CIRCUIT_BREAKER_TIMEOUT_MS = 30_000;   // 30 seconds before retry
    public static final int CIRCUIT_BREAKER_SUCCESS_THRESHOLD = 2;  // successes to close
    
    // ===== RETRY CONFIG =====
    public static final int MAX_RETRY_ATTEMPTS = 3;
    public static final long INITIAL_RETRY_DELAY_MS = 100;          // start with 100ms
    public static final double RETRY_BACKOFF_MULTIPLIER = 2.0;      // exponential: 100ms, 200ms, 400ms
    public static final long MAX_RETRY_DELAY_MS = 5_000;            // cap at 5 seconds
    public static final double RETRY_JITTER_FACTOR = 0.1;           // 10% randomness
    
    // ===== HEALTH CHECK CONFIG =====
    public static final long HEALTH_CHECK_INTERVAL_MS = 10_000;     // every 10 seconds
    public static final long HEALTH_CHECK_TIMEOUT_MS = 5_000;       // 5 second timeout
    public static final int HEALTH_CHECK_FAILURE_THRESHOLD = 3;     // 3 failures = degraded
    public static final int HEALTH_CHECK_CRITICAL_THRESHOLD = 5;    // 5 failures = critical
    
    // ===== AUTO-RECOVERY CONFIG =====
    public static final long AUTO_RECOVERY_CHECK_INTERVAL_MS = 30_000;  // check every 30 seconds
    public static final long RECOVERY_ATTEMPT_TIMEOUT_MS = 15_000;      // 15 seconds per attempt
    public static final int MAX_RECOVERY_ATTEMPTS = 5;                  // max 5 auto-recovery attempts
    
    // ===== CACHE RECOVERY CONFIG =====
    public static final long CACHE_STALE_THRESHOLD_MS = 60_000;     // 60 seconds = stale
    public static final long CACHE_CORRUPTION_DETECTION_INTERVAL_MS = 5_000;
    public static final double CACHE_CORRUPTION_RATIO_THRESHOLD = 0.2;  // 20% invalid entries
    
    // ===== MONITORING THRESHOLDS =====
    public static final double ERROR_RATE_THRESHOLD = 0.1;          // 10% error rate = alert
    public static final long RESPONSE_TIME_THRESHOLD_MS = 2_000;    // 2 seconds = slow
    public static final int QUEUE_SIZE_THRESHOLD = 1000;            // queue backlog threshold
    public static final double MEMORY_USAGE_THRESHOLD = 0.85;       // 85% memory = alert
    
    // ===== SERVICE-SPECIFIC THRESHOLDS =====
    public static final int GITHUB_API_RATE_LIMIT_THRESHOLD = 100;  // warn if < 100 remaining
    public static final int VERCEL_DEPLOYMENT_TIMEOUT_MS = 10_000;
    public static final int FIREBASE_CONNECTION_TIMEOUT_MS = 5_000;
    
    // ===== LOGGING LEVELS =====
    public static final String DEBUG_PREFIX = "🔧";
    public static final String RECOVERY_PREFIX = "🔄";
    public static final String WARNING_PREFIX = "⚠️";
    public static final String ALERT_PREFIX = "🚨";
    
    private SelfHealingConfig() {
        // Utility class - prevent instantiation
    }
}
