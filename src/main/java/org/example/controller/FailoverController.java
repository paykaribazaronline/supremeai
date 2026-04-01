package org.example.controller;

import org.example.resilience.CircuitBreakerManager;
import org.example.resilience.FailoverProvider;
import org.example.resilience.FailoverRegistry;
import org.example.resilience.HealthCheckService;
import org.example.resilience.RetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Failover & Resilience REST Controller
 * Manages failover providers, circuit breakers, and health checks
 */
@RestController
@RequestMapping("/api/resilience")
public class FailoverController {
    private static final Logger logger = LoggerFactory.getLogger(FailoverController.class);
    
    @Autowired
    private FailoverRegistry failoverRegistry;
    
    @Autowired
    private CircuitBreakerManager circuitBreakerManager;
    
    @Autowired
    private RetryStrategy retryStrategy;
    
    @Autowired
    private HealthCheckService healthCheckService;
    
    /**
     * Register failover chain for service
     */
    @PostMapping("/failover-chain")
    public Map<String, Object> registerFailoverChain(
            @RequestParam String serviceId,
            @RequestBody List<FailoverProvider> providers) {
        logger.info("Registering failover chain for service: {} with {} providers", serviceId, providers.size());
        
        failoverRegistry.registerFailoverChain(serviceId, providers);
        
        return new LinkedHashMap<String, Object>() {{
            put("status", "success");
            put("service_id", serviceId);
            put("providers_registered", providers.size());
            put("providers", providers.stream().map(p -> new LinkedHashMap<String, Object>() {{
                put("id", p.getProviderId());
                put("name", p.getProviderName());
                put("priority", p.getPriority());
            }}).toList());
        }};
    }
    
    /**
     * Get next healthy provider
     */
    @GetMapping("/failover-chain/{serviceId}/next-provider")
    public Map<String, Object> getNextHealthyProvider(@PathVariable String serviceId) {
        logger.info("Getting next healthy provider for service: {}", serviceId);
        
        FailoverProvider provider = failoverRegistry.getNextHealthyProvider(serviceId);
        if (provider == null) {
            return new LinkedHashMap<String, Object>() {{
                put("error", "No healthy provider found");
                put("service_id", serviceId);
            }};
        }
        
        return new LinkedHashMap<String, Object>() {{
            put("provider_id", provider.getProviderId());
            put("provider_name", provider.getProviderName());
            put("endpoint", provider.getEndpoint());
            put("status", provider.getStatus());
            put("success_rate", String.format("%.1f%%", provider.getSuccessRate()));
            put("consecutive_failures", provider.getConsecutiveFailures());
        }};
    }
    
    /**
     * Get failover chain status
     */
    @GetMapping("/failover-chain/{serviceId}")
    public Map<String, Object> getFailoverChainStatus(@PathVariable String serviceId) {
        logger.info("Getting failover chain status for service: {}", serviceId);
        
        List<FailoverProvider> chain = failoverRegistry.getChain(serviceId);
        
        return new LinkedHashMap<String, Object>() {{
            put("service_id", serviceId);
            put("total_providers", chain.size());
            put("providers", chain.stream().map(p -> new LinkedHashMap<String, Object>() {{
                put("id", p.getProviderId());
                put("name", p.getProviderName());
                put("priority", p.getPriority());
                put("status", p.getStatus());
                put("success_rate", String.format("%.1f%%", p.getSuccessRate()));
                put("is_healthy", p.isHealthy());
            }}).toList());
        }};
    }
    
    /**
     * Get all circuit breakers
     */
    @GetMapping("/circuit-breakers")
    public Map<String, Object> getAllCircuitBreakers() {
        logger.info("Getting all circuit breakers");
        
        return new LinkedHashMap<String, Object>() {{
            put("circuit_breakers", circuitBreakerManager.getAllCircuitBreakers());
        }};
    }
    
    /**
     * Get circuit breaker status
     */
    @GetMapping("/circuit-breakers/{name}")
    public Map<String, Object> getCircuitBreakerStatus(@PathVariable String name) {
        logger.info("Getting circuit breaker status: {}", name);
        
        Map<String, Object> status = circuitBreakerManager.getCircuitBreakerStatus(name);
        if (status == null) {
            return new LinkedHashMap<String, Object>() {{
                put("error", "Circuit breaker not found");
                put("name", name);
            }};
        }
        
        return status;
    }
    
    /**
     * Reset circuit breaker
     */
    @PostMapping("/circuit-breakers/{name}/reset")
    public Map<String, Object> resetCircuitBreaker(@PathVariable String name) {
        logger.info("Resetting circuit breaker: {}", name);
        
        circuitBreakerManager.resetCircuitBreaker(name);
        
        return new LinkedHashMap<String, Object>() {{
            put("status", "success");
            put("action", "reset");
            put("circuit_breaker", name);
        }};
    }
    
    /**
     * Get retry statistics
     */
    @GetMapping("/retry-stats")
    public Map<String, Object> getRetryStats() {
        logger.info("Getting retry statistics");
        
        return new LinkedHashMap<String, Object>() {{
            put("retry_strategies", retryStrategy.getAllRetryStats());
        }};
    }
    
    /**
     * Get health check results
     */
    @GetMapping("/health-checks")
    public Map<String, Object> getHealthChecks() {
        logger.info("Getting health check results");
        
        return healthCheckService.getHealthSummary();
    }
    
    /**
     * Get health checks for service
     */
    @GetMapping("/health-checks/{serviceId}")
    public Map<String, Object> getHealthChecksByService(@PathVariable String serviceId) {
        logger.info("Getting health checks for service: {}", serviceId);
        
        List<HealthCheckService.HealthCheckResult> results = healthCheckService.getHealthChecksByService(serviceId);
        
        return new LinkedHashMap<String, Object>() {{
            put("service_id", serviceId);
            put("checks", results.stream().map(r -> new LinkedHashMap<String, Object>() {{
                put("provider_id", r.getProviderId());
                put("healthy", r.isHealthy());
                put("status", r.getStatus());
                put("check_time", r.getCheckTime());
                if (!r.getErrorMessage().isEmpty()) {
                    put("error", r.getErrorMessage());
                }
            }}).toList());
        }};
    }
    
    /**
     * Trigger health checks
     */
    @PostMapping("/health-checks/trigger/{serviceId}")
    public Map<String, Object> triggerHealthChecks(@PathVariable String serviceId) {
        logger.info("Triggering health checks for service: {}", serviceId);
        
        healthCheckService.performHealthCheck(serviceId);
        
        return new LinkedHashMap<String, Object>() {{
            put("status", "success");
            put("action", "health_check_triggered");
            put("service_id", serviceId);
        }};
    }
    
    /**
     * Update provider status (admin action)
     */
    @PutMapping("/providers/{providerId}/status")
    public Map<String, Object> updateProviderStatus(
            @PathVariable String providerId,
            @RequestParam String status) {
        logger.info("Updating provider status: {} -> {}", providerId, status);
        
        failoverRegistry.updateProviderStatus(providerId, status);
        
        return new LinkedHashMap<String, Object>() {{
            put("status", "success");
            put("provider_id", providerId);
            put("new_status", status);
        }};
    }
    
    /**
     * Get resilience summary
     */
    @GetMapping("/summary")
    public Map<String, Object> getResilienceSummary() {
        logger.info("Getting resilience summary");
        
        return new LinkedHashMap<String, Object>() {{
            put("circuit_breakers", circuitBreakerManager.getAllCircuitBreakers().size());
            put("retry_strategies", retryStrategy.getAllRetryStats().size());
            put("health_checks", healthCheckService.getAllHealthCheckResults().size());
            put("failover_chains", failoverRegistry.getAllChains().size());
        }};
    }
}
