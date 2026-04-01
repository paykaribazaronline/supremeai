# Self-Healing Framework Documentation

## Overview

The Self-Healing system is a comprehensive framework for automatic error recovery, service monitoring, and resilience patterns in SupremeAI. It enables the system to:

1. **Automatically recover from failures** (retries with exponential backoff)
2. **Prevent cascading failures** (circuit breaker pattern)
3. **Monitor service health** (continuous health tracking)
4. **Trigger automatic recovery** (self-diagnosing and auto-remediation)
5. **Manage cache integrity** (detect and recover corrupted data)

## Architecture

### Core Components

#### 1. CircuitBreaker

Pattern: **Fail-fast with automatic recovery**

States:

- **CLOSED**: Normal operation, passes requests through
- **OPEN**: Fails fast, prevents overwhelming failed service
- **HALF_OPEN**: Testing if service recovered

Configuration:

```properties
circuit.breaker.failure.threshold=5       # Failures before opening
circuit.breaker.timeout.seconds=30        # Duration before retry
circuit.breaker.success.threshold=2       # Successes to close
```

Usage:

```java
CircuitBreaker breaker = selfHealingService.getOrCreateCircuitBreaker("github-api");
T result = breaker.execute(() -> callGitHubAPI());
```

#### 2. RetryStrategy

Pattern: **Exponential backoff with jitter**

Retry sequence: 100ms, 200ms, 400ms, 800ms (capped at 5s)
Jitter: ±10% randomness to prevent thundering herd

Configuration:

```properties
retry.max.attempts=3
retry.initial.delay.ms=100
retry.backoff.multiplier=2.0
retry.max.delay.ms=5000
retry.jitter.factor=0.1
```

Selective retry logic:

- ✅ Retries: timeouts, connection errors, temporarily unavailable
- ❌ Doesn't retry: authentication errors, not found, invalid arguments

Usage:

```java
RetryStrategy retry = selfHealingService.getOrCreateRetryStrategy("api-call");
T result = retry.execute(() -> callAPI());
```

#### 3. HealthMonitor

Tracks service health by:

- Error rates
- Response times
- Consecutive failures
- State transitions

Health states:

- **HEALTHY**: Normal operation (<10% error rate)
- **DEGRADED**: Issues detected (10-20% error rate)
- **CRITICAL**: Severe issues (>20% error rate or 5+ failures)
- **RECOVERING**: In recovery process

Configuration:

```properties
health.check.interval.seconds=10
health.failure.threshold=3        # Failures for degraded state
health.critical.threshold=5       # Failures for critical state
error.rate.threshold=0.1          # 10% error rate threshold
response.time.threshold.ms=2000   # Slow response threshold
```

Usage:

```java
HealthMonitor monitor = selfHealingService.getOrCreateHealthMonitor("data-collector");
monitor.recordSuccess(responseTimeMs);
monitor.recordFailure(responseTimeMs);

HealthMonitor.HealthMetrics metrics = monitor.getMetrics();
// Returns: state, error rate, avg response time, etc.
```

#### 4. SelfHealingService (Orchestrator)

Central hub that coordinates all components:

- Manages circuit breakers, retry strategies, health monitors
- Executes operations with full protection: `executeWithHealing()`
- Performs periodic health checks
- Triggers automatic recovery
- Provides diagnostics

Configuration:

```properties
auto.recovery.check.interval.seconds=30
recovery.attempt.timeout.seconds=15
max.recovery.attempts=5
```

Usage:

```java
@Autowired
private SelfHealingService selfHealing;

// Execute with full protection
Map<String, Object> data = selfHealing.executeWithHealing(
    "data-collector",
    "fetch-github",
    () -> dataCollectorService.getGitHubData(owner, repo)
);
```

#### 5. CacheRecoveryManager

Manages cache layer health:

- Detect stale cache entries (TTL exceeded)
- Detect corrupted data (validation)
- Automatic cleanup and invalidation
- Cache statistics and corruption ratio

Configuration:

```properties
cache.ttl.seconds=1800               # 30 minutes default TTL
cache.stale.threshold.seconds=60     # Additional grace period
cache.corruption.ratio.threshold=0.2 # 20% = alert
```

Usage:

```java
CacheRecoveryManager cacheRecovery = new CacheRecoveryManager("github-cache", 30_000);
cacheRecovery.put("owner:repo", data);
data = cacheRecovery.get("owner:repo");

CacheRecoveryManager.CacheHealth health = cacheRecovery.performCleanup();
```

## Integration Guide

### Step 1: Initialize Self-Healing in Application Startup

```java
@Component
public class ApplicationStartup {
    @Autowired
    private SelfHealingService selfHealingService;
    
    @EventListener(ApplicationReadyEvent.class)
    public void startSelfHealing() {
        selfHealingService.start();
    }
}
```

### Step 2: Register Recovery Handlers for Services

```java
@Configuration
public class SelfHealingConfig {
    @Autowired
    private SelfHealingService selfHealingService;
    
    @Bean
    public void registerRecoveryHandlers() {
        // Github collector recovery
        selfHealingService.registerRecoveryHandler("github-collector", 
            () -> {
                // Attempt recovery: clear cache, reset rate limits
                // Return true if successful
                return attemptGitHubRecovery();
            });
        
        // Firebase recovery
        selfHealingService.registerRecoveryHandler("firebase-collector",
            () -> {
                // Attempt recovery: reconnect, refresh tokens
                return attemptFirebaseRecovery();
            });
    }
}
```

### Step 3: Wrap Service Methods with Self-Healing

**Before:**

```java
public Map<String, Object> getGitHubData(String owner, String repo) {
    HybridDataCollector.HybridResult result = 
        hybridDataCollector.collectGitHubData(owner, repo);
    return (Map<String, Object>) result.data;
}
```

**After:**

```java
public Map<String, Object> getGitHubData(String owner, String repo) {
    return selfHealingService.executeWithHealing(
        "github-collector",
        "fetch-" + owner + "-" + repo,
        () -> {
            HybridDataCollector.HybridResult result = 
                hybridDataCollector.collectGitHubData(owner, repo);
            return (Map<String, Object>) result.data;
        }
    );
}
```

## API Endpoints

### System Health

```http
GET /api/v1/self-healing/system-health
```

Returns comprehensive system health report including all circuit breakers and service metrics.

### Service Diagnostics

```http
GET /api/v1/self-healing/service/{serviceName}
```

Get detailed diagnostics for a specific service including:

- Circuit breaker state
- Health metrics
- Event history

### Circuit Breaker Status

```http
GET /api/v1/self-healing/circuit-breaker/{serviceName}
```

Get live circuit breaker state and failure counts.

### Start/Stop Self-Healing

```http
POST /api/v1/self-healing/start
POST /api/v1/self-healing/stop
```

### Trigger Recovery

```http
POST /api/v1/self-healing/recover/{serviceName}
```

Manually trigger recovery for a service.

### Self-Healing Health Check

```http
GET /api/v1/self-healing/health
```

## Configuration Reference

### SelfHealingConfig.java

All configuration constants in one place:

```java
// Circuit Breaker
CIRCUIT_BREAKER_FAILURE_THRESHOLD = 5
CIRCUIT_BREAKER_TIMEOUT_MS = 30_000
CIRCUIT_BREAKER_SUCCESS_THRESHOLD = 2

// Retry
MAX_RETRY_ATTEMPTS = 3
INITIAL_RETRY_DELAY_MS = 100
RETRY_BACKOFF_MULTIPLIER = 2.0
MAX_RETRY_DELAY_MS = 5_000
RETRY_JITTER_FACTOR = 0.1

// Health Check
HEALTH_CHECK_INTERVAL_MS = 10_000
HEALTH_CHECK_FAILURE_THRESHOLD = 3
HEALTH_CHECK_CRITICAL_THRESHOLD = 5

// Auto-Recovery
AUTO_RECOVERY_CHECK_INTERVAL_MS = 30_000
RECOVERY_ATTEMPT_TIMEOUT_MS = 15_000
MAX_RECOVERY_ATTEMPTS = 5
```

## Monitoring & Diagnostics

### System Health Report

```json
{
  "status": "success",
  "isRunning": true,
  "circuitBreakers": {
    "github-collector": "CLOSED",
    "firebase-collector": "HALF_OPEN",
    "vercel-collector": "CLOSED"
  },
  "serviceMetrics": {
    "github-collector": {
      "state": "HEALTHY",
      "totalRequests": 245,
      "failedRequests": 5,
      "errorRate": "2.04%",
      "avgResponseTimeMs": "523"
    }
  }
}
```

### Service Diagnostics

```json
{
  "serviceName": "github-collector",
  "circuitBreaker": "CLOSED",
  "metrics": {
    "state": "HEALTHY",
    "totalRequests": 245,
    "failedRequests": 5
  },
  "history": [
    {
      "timestamp": 1616000000000,
      "message": "State transition: HEALTHY → HEALTHY"
    }
  ]
}
```

## Best Practices

### 1. Choose Right Thresholds

- Lower thresholds = faster recovery but more false positives
- Higher thresholds = slower recovery but fewer interruptions

### 2. Register All Recovery Handlers

Each service should have a recovery handler that knows how to recover it:

```java
selfHealingService.registerRecoveryHandler("service-name", 
    () -> {
        // Refresh connections
        // Clear caches
        // Reset counters
        // Return success status
        return true;
    });
```

### 3. Monitor Circuit Breakers

High OPEN/HALF_OPEN states indicate problems. Investigate root causes.

### 4. Use executeWithHealing() for External Services

Wrap calls to:

- External APIs (GitHub, Vercel, Firebase)
- Databases
- Message queues
- Any remote service

### 5. Cache Recovery Integration

```java
CacheRecoveryManager cacheRecovery = new CacheRecoveryManager("service-cache", TTL_MS);

// Periodic cleanup (schedule this)
CacheRecoveryManager.CacheHealth health = cacheRecovery.performCleanup();
if (health.corruptionRatio > 0.2) {
    logger.alert("High cache corruption detected");
}
```

## Logging

All self-healing events use emoji prefixes for quick identification:

- 🔧 `DEBUG_PREFIX` - Debug information
- 🔄 `RECOVERY_PREFIX` - Recovery actions
- ⚠️ `WARNING_PREFIX` - Warnings
- 🚨 `ALERT_PREFIX` - Critical alerts

Example log flow:

```
🔧 Created health monitor for service: github-collector
⚠️ Health state changed for github-collector: HEALTHY → DEGRADED
🔄 Triggering recovery for github-collector
✅ Recovery successful for github-collector
```

## Performance Impact

- **Circuit Breaker**: ~0.1ms per check
- **Retry Logic**: Adds time only on failures
- **Health Monitor**: ~0.5ms per operation (negligible overhead)
- **Periodic Checks**: Runs on separate thread pool
- **Overall**: <1% performance overhead in normal operation

## Testing Self-Healing

### Unit Tests

```java
@Test
public void testCircuitBreakerOpensAfterFailures() {
    CircuitBreaker breaker = new CircuitBreaker("test", 3, 1000, 2);
    
    for (int i = 0; i < 3; i++) {
        assertThrows(Exception.class, 
            () -> breaker.execute(() -> { throw new Exception(); }));
    }
    
    assertEquals(CircuitBreaker.State.OPEN, breaker.getState());
}
```

### Integration Tests

```java
@Test
public void testSelfHealingExecution() {
    Map<String, Object> data = selfHealingService.executeWithHealing(
        "test-service",
        "test-operation",
        () -> Map.of("status", "success")
    );
    
    assertNotNull(data);
    assertEquals("success", data.get("status"));
}
```

## Future Enhancements

1. **Machine Learning-based thresholds** - Auto-tune based on patterns
2. **Distributed tracing** - Track requests across services
3. **Predictive recovery** - Anticipate failures before they happen
4. **Custom recovery strategies** - Plugin architecture for domain-specific recovery
5. **Metrics export** - Prometheus/Grafana integration
