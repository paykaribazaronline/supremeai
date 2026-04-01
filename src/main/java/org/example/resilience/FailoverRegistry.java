package org.example.resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Failover Registry Service
 * Manages backup providers and failover routes
 */
@Service
public class FailoverRegistry {
    private static final Logger logger = LoggerFactory.getLogger(FailoverRegistry.class);
    
    private final Map<String, List<FailoverProvider>> failoverChains = new ConcurrentHashMap<>();
    private final Map<String, FailoverProvider> providerCache = new ConcurrentHashMap<>();
    
    /**
     * Register failover chain for a service
     */
    public void registerFailoverChain(String serviceId, List<FailoverProvider> providers) {
        providers.sort(Comparator.comparingInt(FailoverProvider::getPriority));
        failoverChains.put(serviceId, new ArrayList<>(providers));
        
        for (FailoverProvider provider : providers) {
            providerCache.put(provider.getProviderId(), provider);
        }
        
        logger.info("✅ Failover chain registered for service: {} with {} providers", 
            serviceId, providers.size());
    }
    
    /**
     * Get next healthy provider in failover chain
     */
    public FailoverProvider getNextHealthyProvider(String serviceId) {
        List<FailoverProvider> chain = failoverChains.get(serviceId);
        if (chain == null || chain.isEmpty()) {
            logger.warn("⚠️ No failover chain defined for service: {}", serviceId);
            return null;
        }
        
        for (FailoverProvider provider : chain) {
            if (provider.isHealthy()) {
                logger.debug("✅ Selected healthy provider: {} (success rate: {:.1f}%)", 
                    provider.getProviderName(), provider.getSuccessRate());
                return provider;
            }
        }
        
        // Fallback to provider with highest success rate
        logger.warn("⚠️ No fully healthy provider found, using best available");
        return chain.stream()
            .max(Comparator.comparingDouble(FailoverProvider::getSuccessRate))
            .orElse(null);
    }
    
    /**
     * Record success for provider
     */
    public void recordProviderSuccess(String providerId) {
        FailoverProvider provider = providerCache.get(providerId);
        if (provider != null) {
            provider.recordSuccess();
            if (provider.getConsecutiveSuccesses() >= 5) {
                provider.setStatus("ACTIVE");
                logger.info("✅ Provider {} recovered to ACTIVE status", providerId);
            }
        }
    }
    
    /**
     * Record failure for provider
     */
    public void recordProviderFailure(String providerId) {
        FailoverProvider provider = providerCache.get(providerId);
        if (provider != null) {
            provider.recordFailure();
            if (provider.getConsecutiveFailures() >= 3) {
                provider.setStatus("DEGRADED");
                logger.error("❌ Provider {} marked as DEGRADED after {} consecutive failures", 
                    providerId, provider.getConsecutiveFailures());
            }
        }
    }
    
    /**
     * Get provider status
     */
    public FailoverProvider getProviderStatus(String providerId) {
        return providerCache.get(providerId);
    }
    
    /**
     * Get all providers in chain
     */
    public List<FailoverProvider> getChain(String serviceId) {
        return new ArrayList<>(failoverChains.getOrDefault(serviceId, new ArrayList<>()));
    }
    
    /**
     * Update provider status manually (admin action)
     */
    public void updateProviderStatus(String providerId, String status) {
        FailoverProvider provider = providerCache.get(providerId);
        if (provider != null) {
            provider.setStatus(status);
            logger.info("🔄 Provider {} status updated to: {}", providerId, status);
        }
    }
    
    /**
     * Get all failover chains
     */
    public Map<String, List<FailoverProvider>> getAllChains() {
        return new HashMap<>(failoverChains);
    }
    
    /**
     * Health check for all providers
     */
    public void performHealthCheck(String serviceId, java.util.function.Function<FailoverProvider, Boolean> healthChecker) {
        List<FailoverProvider> chain = failoverChains.get(serviceId);
        if (chain != null) {
            for (FailoverProvider provider : chain) {
                try {
                    boolean isHealthy = healthChecker.apply(provider);
                    if (isHealthy) {
                        recordProviderSuccess(provider.getProviderId());
                    } else {
                        recordProviderFailure(provider.getProviderId());
                    }
                    provider.setLastHealthCheck(System.currentTimeMillis());
                } catch (Exception e) {
                    logger.error("❌ Health check failed for provider: {}", provider.getProviderId(), e);
                    recordProviderFailure(provider.getProviderId());
                }
            }
        }
    }
}
