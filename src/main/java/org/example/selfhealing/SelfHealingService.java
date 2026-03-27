package org.example.selfhealing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Self-Healing Service Orchestrator
 * 
 * Central hub for all self-healing capabilities:
 * 1. Manages circuit breakers for external services
 * 2. Applies retry strategies with exponential backoff
 * 3. Monitors health of all services
 * 4. Triggers automatic recovery procedures
 * 5. Provides comprehensive diagnostics
 */
@Service
public class SelfHealingService {
    private static final Logger logger = LoggerFactory.getLogger(SelfHealingService.class);
    
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    private final Map<String, RetryStrategy> retryStrategies = new ConcurrentHashMap<>();
    private final Map<String, HealthMonitor> healthMonitors = new ConcurrentHashMap<>();
    private final Map<String, AutoRecoveryHandler> recoveryHandlers = new ConcurrentHashMap<>();
    
    private final ScheduledExecutorService healthCheckExecutor = Executors.newScheduledThreadPool(2);
    private final ScheduledExecutorService recoveryExecutor = Executors.newScheduledThreadPool(2);
    
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private volatile long lastComprehensiveCheckTime = 0;
    
    public SelfHealingService() {
        logger.info("🔧 Self-Healing Service initialized");
    }
    
    /**
     * Start self-healing services
     */
    public void start() {
        if (isRunning.getAndSet(true)) {
            logger.warn("Self-healing already running");
            return;
        }
        
        logger.info("{}Starting self-healing system", SelfHealingConfig.RECOVERY_PREFIX);
        
        // Schedule periodic health checks
        healthCheckExecutor.scheduleAtFixedRate(
            this::performHealthChecks,
            SelfHealingConfig.HEALTH_CHECK_INTERVAL_MS,
            SelfHealingConfig.HEALTH_CHECK_INTERVAL_MS,
            TimeUnit.MILLISECONDS);
        
        // Schedule auto-recovery checks
        recoveryExecutor.scheduleAtFixedRate(
            this::performAutoRecovery,
            SelfHealingConfig.AUTO_RECOVERY_CHECK_INTERVAL_MS,
            SelfHealingConfig.AUTO_RECOVERY_CHECK_INTERVAL_MS,
            TimeUnit.MILLISECONDS);
        
        logger.info("✅ Self-healing system started");
    }
    
    /**
     * Stop self-healing services
     */
    public void stop() {
        isRunning.set(false);
        healthCheckExecutor.shutdown();
        recoveryExecutor.shutdown();
        logger.info("🛑 Self-healing system stopped");
    }
    
    /**
     * Execute operation with full self-healing protection
     * Applies: Circuit breaker + Retry + Health monitoring
     */
    public <T> T executeWithHealing(String serviceName, String operationName, Supplier<T> operation) throws Exception {
        long startTime = System.currentTimeMillis();
        String fullName = serviceName + "." + operationName;
        
        // Get or create monitoring components
        CircuitBreaker breaker = getOrCreateCircuitBreaker(serviceName);
        RetryStrategy retry = getOrCreateRetryStrategy(fullName);
        HealthMonitor monitor = getOrCreateHealthMonitor(serviceName);
        
        try {
            // First check circuit breaker
            if (breaker.isOpen()) {
                throw new CircuitBreaker.CircuitBreakerOpenException(
                    serviceName + " circuit breaker is open");
            }
            
            // Execute with retry and circuit breaker protection
            T result = breaker.execute(new java.util.function.Supplier<T>() {
                @Override
                public T get() {
                    try {
                        return retry.execute(operation);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            
            long responseTime = System.currentTimeMillis() - startTime;
            monitor.recordSuccess(responseTime);
            
            logger.debug("✅ {} succeeded in {}ms", fullName, responseTime);
            return result;
            
        } catch (RuntimeException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            monitor.recordFailure(responseTime);
            
            // Unwrap if it's a wrapped exception
            if (e.getCause() instanceof Exception) {
                logger.error("❌ {} failed: {}", fullName, e.getCause().getMessage());
                throw (Exception) e.getCause();
            }
            
            logger.error("❌ {} failed: {}", fullName, e.getMessage());
            throw e;
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            monitor.recordFailure(responseTime);
            
            logger.error("❌ {} failed: {}", fullName, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Perform periodic health checks
     */
    private void performHealthChecks() {
        try {
            logger.debug("🔍 Performing health checks ({}services)", healthMonitors.size());
            
            for (HealthMonitor monitor : healthMonitors.values()) {
                HealthMonitor.HealthMetrics metrics = monitor.getMetrics();
                
                if (metrics.state == HealthMonitor.HealthState.CRITICAL) {
                    logger.warn("{}CRITICAL health detected for {}: {}",
                        SelfHealingConfig.ALERT_PREFIX, 
                        metrics.serviceName, metrics);
                    
                    // Trigger recovery for critical services
                    triggerRecovery(metrics.serviceName);
                } else if (metrics.state == HealthMonitor.HealthState.DEGRADED) {
                    logger.info("{}Degraded health for {}: {}",
                        SelfHealingConfig.WARNING_PREFIX,
                        metrics.serviceName, metrics);
                }
            }
            
            lastComprehensiveCheckTime = System.currentTimeMillis();
            
        } catch (Exception e) {
            logger.error("Error during health checks", e);
        }
    }
    
    /**
     * Perform automatic recovery attempts
     */
    private void performAutoRecovery() {
        try {
            for (Map.Entry<String, HealthMonitor> entry : healthMonitors.entrySet()) {
                String serviceName = entry.getKey();
                HealthMonitor monitor = entry.getValue();
                
                if (monitor.needsIntervention() || monitor.isRecovering()) {
                    triggerRecovery(serviceName);
                }
            }
        } catch (Exception e) {
            logger.error("Error during auto-recovery", e);
        }
    }
    
    /**
     * Trigger recovery for a service
     */
    private void triggerRecovery(String serviceName) {
        AutoRecoveryHandler handler = recoveryHandlers.get(serviceName);
        if (handler == null) {
            logger.warn("No recovery handler registered for {}", serviceName);
            return;
        }
        
        logger.info("{}Triggering recovery for {}", SelfHealingConfig.RECOVERY_PREFIX, serviceName);
        
        recoveryExecutor.submit(() -> {
            try {
                boolean success = handler.attemptRecovery();
                if (success) {
                    logger.info("✅ Recovery successful for {}", serviceName);
                    HealthMonitor monitor = healthMonitors.get(serviceName);
                    if (monitor != null) {
                        monitor.reset();
                    }
                } else {
                    logger.warn("{}Recovery failed for {}", SelfHealingConfig.WARNING_PREFIX, serviceName);
                }
            } catch (Exception e) {
                logger.error("{}Recovery error for {}: {}", SelfHealingConfig.ALERT_PREFIX, serviceName, e.getMessage());
            }
        });
    }
    
    // ===== Registration Methods =====
    
    public CircuitBreaker getOrCreateCircuitBreaker(String serviceName) {
        return circuitBreakers.computeIfAbsent(serviceName, k -> new CircuitBreaker(k));
    }
    
    public RetryStrategy getOrCreateRetryStrategy(String operationName) {
        return retryStrategies.computeIfAbsent(operationName, k -> new RetryStrategy(k));
    }
    
    public HealthMonitor getOrCreateHealthMonitor(String serviceName) {
        return healthMonitors.computeIfAbsent(serviceName, k -> new HealthMonitor(k));
    }
    
    /**
     * Register a custom recovery handler for a service
     */
    public void registerRecoveryHandler(String serviceName, AutoRecoveryHandler handler) {
        recoveryHandlers.put(serviceName, handler);
        logger.debug("Registered recovery handler for {}", serviceName);
    }
    
    // ===== Diagnostics =====
    
    /**
     * Get comprehensive system health report
     */
    public SystemHealthReport getSystemHealthReport() {
        SystemHealthReport report = new SystemHealthReport();
        
        for (CircuitBreaker breaker : circuitBreakers.values()) {
            report.circuitBreakerStates.put(breaker.getName(), breaker.getState().toString());
        }
        
        for (HealthMonitor monitor : healthMonitors.values()) {
            report.serviceMetrics.put(monitor.getServiceName(), monitor.getMetrics());
        }
        
        report.isRunning = isRunning.get();
        report.generatedAt = System.currentTimeMillis();
        
        return report;
    }
    
    /**
     * Get diagnostics for a specific service
     */
    public ServiceDiagnostics getServiceDiagnostics(String serviceName) {
        CircuitBreaker breaker = circuitBreakers.get(serviceName);
        HealthMonitor monitor = healthMonitors.get(serviceName);
        
        return new ServiceDiagnostics(
            serviceName,
            breaker != null ? breaker.getState().toString() : "NO_BREAKER",
            monitor != null ? monitor.getMetrics() : null,
            monitor != null ? monitor.getHistory() : List.of()
        );
    }
    
    // ===== Inner Classes =====
    
    /**
     * Handler for custom recovery logic
     */
    public interface AutoRecoveryHandler {
        /**
         * Attempt to recover the service
         * @return true if recovery successful, false otherwise
         */
        boolean attemptRecovery() throws Exception;
    }
    
    /**
     * System-wide health report
     */
    public static class SystemHealthReport {
        public final Map<String, String> circuitBreakerStates = new LinkedHashMap<>();
        public final Map<String, HealthMonitor.HealthMetrics> serviceMetrics = new LinkedHashMap<>();
        public boolean isRunning = false;
        public long generatedAt = 0;
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== SYSTEM HEALTH REPORT ===\n");
            sb.append("Running: ").append(isRunning).append("\n");
            sb.append("\nCircuit Breakers:\n");
            for (Map.Entry<String, String> entry : circuitBreakerStates.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            sb.append("\nService Metrics:\n");
            for (Map.Entry<String, HealthMonitor.HealthMetrics> entry : serviceMetrics.entrySet()) {
                sb.append("  ").append(entry.getValue()).append("\n");
            }
            return sb.toString();
        }
    }
    
    /**
     * Diagnostics for a specific service
     */
    public static class ServiceDiagnostics {
        public final String serviceName;
        public final String circuitBreakerState;
        public final HealthMonitor.HealthMetrics metrics;
        public final List<HealthMonitor.HealthEvent> history;
        
        public ServiceDiagnostics(String serviceName, String circuitBreakerState,
                                 HealthMonitor.HealthMetrics metrics,
                                 List<HealthMonitor.HealthEvent> history) {
            this.serviceName = serviceName;
            this.circuitBreakerState = circuitBreakerState;
            this.metrics = metrics;
            this.history = history;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Diagnostics for ").append(serviceName).append(" ===\n");
            sb.append("Circuit Breaker: ").append(circuitBreakerState).append("\n");
            if (metrics != null) {
                sb.append("Metrics: ").append(metrics).append("\n");
                sb.append("\nRecent History:\n");
                for (HealthMonitor.HealthEvent event : history) {
                    sb.append("  ").append(event).append("\n");
                }
            }
            return sb.toString();
        }
    }
    
    // ===== Getters =====
    
    public boolean isRunning() {
        return isRunning.get();
    }
    
    public long getLastComprehensiveCheckTime() {
        return lastComprehensiveCheckTime;
    }
}
