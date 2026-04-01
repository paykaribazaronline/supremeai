package org.example.resilience;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.*;

/**
 * Failover Provider Configuration
 * Represents a backup provider for failover scenarios
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FailoverProvider {
    private String providerId;
    private String providerName;
    private String endpoint;
    private String apiKey;
    private int priority;  // Lower = higher priority
    private String status; // ACTIVE, INACTIVE, DEGRADED
    private long lastHealthCheck;
    private int consecutiveFailures = 0;
    private int consecutiveSuccesses = 0;
    private double successRate = 100.0;
    private Map<String, Object> metadata = new HashMap<>();
    
    public boolean isHealthy() {
        return "ACTIVE".equals(status) && consecutiveFailures < 3;
    }
    
    public void recordSuccess() {
        this.consecutiveSuccesses++;
        this.consecutiveFailures = 0;
        this.successRate = (this.successRate * 0.9) + (100 * 0.1);
    }
    
    public void recordFailure() {
        this.consecutiveFailures++;
        this.consecutiveSuccesses = 0;
        this.successRate = (this.successRate * 0.9) + (0 * 0.1);
    }
}
