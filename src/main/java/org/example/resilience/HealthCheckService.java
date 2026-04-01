package org.example.resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Health Check Service
 * Performs periodic health checks on failover providers and external services
 */
@Service
@EnableScheduling
public class HealthCheckService {
    private static final Logger logger = LoggerFactory.getLogger(HealthCheckService.class);
    
    @Autowired
    private FailoverRegistry failoverRegistry;
    
    private final Map<String, HealthCheckResult> healthCheckResults = new ConcurrentHashMap<>();
    private final List<HealthCheckListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<String, Long> lastCheckTime = new ConcurrentHashMap<>();
    
    /**
     * Health check interval trigger - runs every 30 seconds
     */
    @Scheduled(fixedDelay = 30000)
    public void performScheduledHealthChecks() {
        logger.debug("🏥 Starting scheduled health checks...");
        
        Map<String, List<FailoverProvider>> allChains = failoverRegistry.getAllChains();
        for (String serviceId : allChains.keySet()) {
            performHealthCheck(serviceId);
        }
    }
    
    /**
     * Perform health check for service
     */
    @Async
    public void performHealthCheck(String serviceId) {
        List<FailoverProvider> chain = failoverRegistry.getChain(serviceId);
        
        for (FailoverProvider provider : chain) {
            try {
                boolean isHealthy = checkProviderHealth(provider);
                if (isHealthy) {
                    failoverRegistry.recordProviderSuccess(provider.getProviderId());
                } else {
                    failoverRegistry.recordProviderFailure(provider.getProviderId());
                }
                
                HealthCheckResult result = new HealthCheckResult(
                    provider.getProviderId(),
                    isHealthy,
                    System.currentTimeMillis(),
                    "",
                    provider.getStatus()
                );
                healthCheckResults.put(provider.getProviderId(), result);
                lastCheckTime.put(provider.getProviderId(), System.currentTimeMillis());
                
                notifyListeners(result);
            } catch (Exception e) {
                logger.error("❌ Health check error for provider {}: {}", provider.getProviderId(), e.getMessage());
                failoverRegistry.recordProviderFailure(provider.getProviderId());
            }
        }
    }
    
    /**
     * Check provider health by attempting connection
     */
    private boolean checkProviderHealth(FailoverProvider provider) {
        try {
            // Attempt simple HTTP GET to provider endpoint
            if (provider.getEndpoint() != null && !provider.getEndpoint().isEmpty()) {
                // In real implementation, would make actual HTTP request
                // For now, simulate with random success based on failure count
                return provider.getConsecutiveFailures() < 3;
            }
            return true;
        } catch (Exception e) {
            logger.error("❌ Provider health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get health check result
     */
    public HealthCheckResult getHealthCheckResult(String providerId) {
        return healthCheckResults.get(providerId);
    }
    
    /**
     * Get all health check results
     */
    public Collection<HealthCheckResult> getAllHealthCheckResults() {
        return healthCheckResults.values();
    }
    
    /**
     * Get health check results by service
     */
    public List<HealthCheckResult> getHealthChecksByService(String serviceId) {
        List<FailoverProvider> chain = failoverRegistry.getChain(serviceId);
        List<HealthCheckResult> results = new ArrayList<>();
        
        for (FailoverProvider provider : chain) {
            HealthCheckResult result = healthCheckResults.get(provider.getProviderId());
            if (result != null) {
                results.add(result);
            }
        }
        
        return results;
    }
    
    /**
     * Register health check listener
     */
    public void addHealthCheckListener(HealthCheckListener listener) {
        listeners.add(listener);
        logger.debug("✅ Health check listener registered");
    }
    
    /**
     * Notify all listeners of health check result
     */
    private void notifyListeners(HealthCheckResult result) {
        for (HealthCheckListener listener : listeners) {
            try {
                listener.onHealthCheckResult(result);
            } catch (Exception e) {
                logger.error("❌ Error notifying health check listener: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Get summary health status
     */
    public Map<String, Object> getHealthSummary() {
        return new LinkedHashMap<String, Object>() {{
            put("total_checks", healthCheckResults.size());
            put("healthy_providers", healthCheckResults.values().stream()
                .filter(HealthCheckResult::isHealthy)
                .count());
            put("degraded_providers", healthCheckResults.values().stream()
                .filter(r -> !r.isHealthy())
                .count());
            put("all_providers", new ArrayList<>(healthCheckResults.values()));
        }};
    }
    
    // Inner class for health check results
    public static class HealthCheckResult {
        private final String providerId;
        private final boolean healthy;
        private final long checkTime;
        private final String errorMessage;
        private final String status;
        
        public HealthCheckResult(String providerId, boolean healthy, long checkTime, 
                                String errorMessage, String status) {
            this.providerId = providerId;
            this.healthy = healthy;
            this.checkTime = checkTime;
            this.errorMessage = errorMessage;
            this.status = status;
        }
        
        public String getProviderId() { return providerId; }
        public boolean isHealthy() { return healthy; }
        public long getCheckTime() { return checkTime; }
        public String getErrorMessage() { return errorMessage; }
        public String getStatus() { return status; }
    }
    
    // Listener interface
    public interface HealthCheckListener {
        void onHealthCheckResult(HealthCheckResult result);
    }
}
