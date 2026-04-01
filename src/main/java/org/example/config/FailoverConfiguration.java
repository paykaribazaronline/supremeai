package org.example.config;

import org.example.resilience.FailoverProvider;
import org.example.resilience.FailoverRegistry;
import org.example.resilience.CircuitBreakerManager;
import org.example.resilience.HealthCheckService;
import org.example.resilience.RetryStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Failover Configuration - Enterprise Resilience Setup
 * Manages backend providers, circuit breakers, and recovery mechanisms
 * এন্টারপ্রাইজ-লেভেল রেসিলিয়েন্স সেটআপ
 */
@Configuration
public class FailoverConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(FailoverConfiguration.class);
    
    @Autowired(required = false)
    private FailoverRegistry failoverRegistry;
    
    @Autowired(required = false)
    private CircuitBreakerManager circuitBreakerManager;
    
    @Autowired(required = false)
    private HealthCheckService healthCheckService;
    
    @Autowired(required = false)
    private RetryStrategy retryStrategy;
    
    /**
     * Failover provider management service
     */
    @Bean
    public FailoverService failoverService() {
        return new FailoverService();
    }
    
    /**
     * Service for managing failover strategies
     */
    public static class FailoverService {
        private static final Logger logger = LoggerFactory.getLogger(FailoverService.class);
        
        private final ConcurrentHashMap<String, FailoverGroup> failoverGroups = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, EndpointHealth> endpointHealth = new ConcurrentHashMap<>();
        
        /**
         * Register failover group (e.g., multiple AI API providers)
         */
        public void registerFailoverGroup(String groupName, List<String> endpoints) {
            List<Endpoint> endpointList = new ArrayList<>();
            for (String url : endpoints) {
                endpointList.add(new Endpoint(url, true));
                endpointHealth.put(url, new EndpointHealth(url));
            }
            
            failoverGroups.put(groupName, new FailoverGroup(groupName, endpointList));
            logger.info("✅ Failover group registered: {} with {} endpoints", groupName, endpoints.size());
        }
        
        /**
         * Get next healthy endpoint from failover group
         */
        public String getHealthyEndpoint(String groupName) {
            FailoverGroup group = failoverGroups.get(groupName);
            if (group == null) {
                logger.error("Failover group not found: {}", groupName);
                return null;
            }
            
            // Try to find a healthy endpoint
            for (Endpoint endpoint : group.endpoints) {
                EndpointHealth health = endpointHealth.get(endpoint.url);
                
                if (health != null && health.isHealthy()) {
                    logger.debug("Selected healthy endpoint: {}", endpoint.url);
                    return endpoint.url;
                }
            }
            
            // If no healthy endpoint found, return the first one (will trigger recovery)
            if (!group.endpoints.isEmpty()) {
                logger.warn("No healthy endpoints found in group: {}, using first endpoint", groupName);
                return group.endpoints.get(0).url;
            }
            
            logger.error("No endpoints available in failover group: {}", groupName);
            return null;
        }
        
        /**
         * Mark endpoint as healthy
         */
        public void markEndpointHealthy(String endpoint) {
            EndpointHealth health = endpointHealth.get(endpoint);
            if (health != null) {
                health.markHealthy();
                logger.info("✅ Endpoint marked healthy: {}", endpoint);
            }
        }
        
        /**
         * Mark endpoint as unhealthy
         */
        public void markEndpointUnhealthy(String endpoint, String reason) {
            EndpointHealth health = endpointHealth.get(endpoint);
            if (health != null) {
                health.markUnhealthy();
                logger.warn("⚠️ Endpoint marked unhealthy: {} - {}", endpoint, reason);
            }
        }
        
        /**
         * Get failover group status
         */
        public GroupStatus getGroupStatus(String groupName) {
            FailoverGroup group = failoverGroups.get(groupName);
            if (group == null) {
                return null;
            }
            
            int healthy = 0;
            int unhealthy = 0;
            
            for (Endpoint endpoint : group.endpoints) {
                EndpointHealth health = endpointHealth.get(endpoint.url);
                if (health != null && health.isHealthy()) {
                    healthy++;
                } else {
                    unhealthy++;
                }
            }
            
            return new GroupStatus(groupName, healthy, unhealthy, group.endpoints.size());
        }
        
        /**
         * Get all endpoint statuses
         */
        public Map<String, EndpointStatus> getAllEndpointStatuses() {
            Map<String, EndpointStatus> statuses = new HashMap<>();
            
            for (Map.Entry<String, EndpointHealth> entry : endpointHealth.entrySet()) {
                EndpointHealth health = entry.getValue();
                statuses.put(entry.getKey(), new EndpointStatus(
                    entry.getKey(),
                    health.isHealthy(),
                    health.failureCount,
                    health.successCount,
                    health.lastCheckTime
                ));
            }
            
            return statuses;
        }
    }
    
    /**
     * Failover group managing multiple endpoints
     */
    public static class FailoverGroup {
        public String name;
        public List<Endpoint> endpoints;
        public AtomicInteger currentIndex = new AtomicInteger(0);
        
        public FailoverGroup(String name, List<Endpoint> endpoints) {
            this.name = name;
            this.endpoints = endpoints;
        }
    }
    
    /**
     * Individual endpoint in failover group
     */
    public static class Endpoint {
        public String url;
        public boolean active;
        
        public Endpoint(String url, boolean active) {
            this.url = url;
            this.active = active;
        }
    }
    
    /**
     * Health monitoring for endpoints
     */
    public static class EndpointHealth {
        private final String url;
        private volatile boolean healthy = true;
        private volatile long lastHealthCheck = System.currentTimeMillis();
        public long failureCount = 0;
        public long successCount = 0;
        
        public EndpointHealth(String url) {
            this.url = url;
        }
        
        public synchronized void markHealthy() {
            this.healthy = true;
            this.lastHealthCheck = System.currentTimeMillis();
            this.successCount++;
        }
        
        public synchronized void markUnhealthy() {
            this.healthy = false;
            this.lastHealthCheck = System.currentTimeMillis();
            this.failureCount++;
        }
        
        public boolean isHealthy() {
            // Automatic recovery check after 30 seconds
            if (!healthy && System.currentTimeMillis() - lastHealthCheck > 30000) {
                return true; // Allow retry
            }
            return healthy;
        }
        
        public long getFailureCount() {
            return failureCount;
        }
        
        public long getSuccessCount() {
            return successCount;
        }
        
        public long getLastHealthcheck() {
            return lastHealthCheck;
        }
    }
    
    /**
     * Status DTO for failover group
     */
    public static class GroupStatus {
        public String groupName;
        public int healthyCount;
        public int unhealthyCount;
        public int totalCount;
        
        public GroupStatus(String groupName, int healthyCount, int unhealthyCount, int totalCount) {
            this.groupName = groupName;
            this.healthyCount = healthyCount;
            this.unhealthyCount = unhealthyCount;
            this.totalCount = totalCount;
        }
    }
    
    /**
     * Status DTO for individual endpoint
     */
    public static class EndpointStatus {
        public String url;
        public boolean healthy;
        public long failureCount;
        public long successCount;
        public long lastCheckTime;
        
        public EndpointStatus(String url, boolean healthy, long failureCount, long successCount, long lastCheckTime) {
            this.url = url;
            this.healthy = healthy;
            this.failureCount = failureCount;
            this.successCount = successCount;
            this.lastCheckTime = lastCheckTime;
        }
    }
}
