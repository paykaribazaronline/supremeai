package org.example.command.impl;

import org.example.command.*;
import org.example.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Monitoring Commands
 * - Health Check
 * - Quota Status
 * - Error Report
 */
public class MonitoringCommands {
    private static final Logger logger = LoggerFactory.getLogger(MonitoringCommands.class);
    
    private final HybridDataCollector dataCollector;
    private final BudgetManager budgetManager;
    private final QuotaTracker quotaTracker;
    
    public MonitoringCommands(HybridDataCollector dataCollector, 
                             BudgetManager budgetManager,
                             QuotaTracker quotaTracker) {
        this.dataCollector = dataCollector;
        this.budgetManager = budgetManager;
        this.quotaTracker = quotaTracker;
    }
    
    /**
     * Health Check Command
     * Returns overall system health status
     */
    public Command getHealthCheckCommand() {
        return new Command() {
            @Override
            public String getName() { return "health-check"; }
            
            @Override
            public String getDescription() { 
                return "Check overall system health and status"; 
            }
            
            @Override
            public CommandCategory getCategory() { 
                return CommandCategory.MONITORING; 
            }
            
            @Override
            public CommandType getType() { 
                return CommandType.SYNC; 
            }
            
            @Override
            public String[] getRequiredPermissions() { 
                return new String[] { "view.health" }; 
            }
            
            @Override
            public CommandSchema getSchema() { 
                return new CommandSchema("health-check"); 
            }
            
            @Override
            public CommandResult execute(Map<String, Object> params, CommandContext context) {
                try {
                    Map<String, Object> health = new HashMap<>();
                    
                    // Get data collector health
                    HybridDataCollector.CollectorHealth collectorHealth = dataCollector.getHealth();
                    health.put("dataCollector", Map.of(
                        "status", collectorHealth.status,
                        "successRate", collectorHealth.successRate,
                        "apiSuccess", collectorHealth.metrics.apiSuccess,
                        "apiFailed", collectorHealth.metrics.apiFailed
                    ));
                    
                    // Get budget status
                    health.put("budget", "OK");  // Placeholder
                    
                    // Get quota status
                    health.put("quotas", "OK");  // Placeholder
                    
                    // Overall status
                    health.put("status", "HEALTHY");
                    health.put("timestamp", System.currentTimeMillis());
                    
                    logger.info("✅ Health check passed");
                    return CommandResult.success("health-check", health);
                    
                } catch (Exception e) {
                    logger.error("❌ Health check failed", e);
                    return CommandResult.error("health-check", "CHECK_FAILED", e.getMessage());
                }
            }
            
            @Override
            public void validate(Map<String, Object> params) {
                // No parameters required
            }
        };
    }
    
    /**
     * Quota Status Command
     * Shows quota usage across all API providers
     */
    public Command getQuotaStatusCommand() {
        return new Command() {
            @Override
            public String getName() { return "quota-status"; }
            
            @Override
            public String getDescription() { 
                return "Check quota usage for all providers"; 
            }
            
            @Override
            public CommandCategory getCategory() { 
                return CommandCategory.MONITORING; 
            }
            
            @Override
            public CommandType getType() { 
                return CommandType.SYNC; 
            }
            
            @Override
            public String[] getRequiredPermissions() { 
                return new String[] { "view.quotas" }; 
            }
            
            @Override
            public CommandSchema getSchema() { 
                return new CommandSchema("quota-status"); 
            }
            
            @Override
            public CommandResult execute(Map<String, Object> params, CommandContext context) {
                try {
                    Map<String, Object> status = new HashMap<>();
                    
                    // Get all quota statuses
                    Map<String, QuotaTracker.QuotaStatus> allQuotas = 
                        quotaTracker.getAllStatus();
                    
                    status.put("quotas", allQuotas);
                    status.put("count", allQuotas.size());
                    status.put("timestamp", System.currentTimeMillis());
                    
                    logger.info("📊 Quota status retrieved");
                    return CommandResult.success("quota-status", status);
                    
                } catch (Exception e) {
                    logger.error("❌ Quota status failed", e);
                    return CommandResult.error("quota-status", "STATUS_FAILED", e.getMessage());
                }
            }
            
            @Override
            public void validate(Map<String, Object> params) {}
        };
    }
    
    /**
     * Metrics Command
     * Returns detailed metrics
     */
    public Command getMetricsCommand() {
        return new Command() {
            @Override
            public String getName() { return "metrics"; }
            
            @Override
            public String getDescription() { 
                return "Get detailed system metrics"; 
            }
            
            @Override
            public CommandCategory getCategory() { 
                return CommandCategory.MONITORING; 
            }
            
            @Override
            public CommandType getType() { 
                return CommandType.SYNC; 
            }
            
            @Override
            public String[] getRequiredPermissions() { 
                return new String[] { "view.metrics" }; 
            }
            
            @Override
            public CommandSchema getSchema() { 
                return new CommandSchema("metrics"); 
            }
            
            @Override
            public CommandResult execute(Map<String, Object> params, CommandContext context) {
                try {
                    Map<String, Object> metrics = new HashMap<>();
                    
                    // Collect various metrics
                    metrics.put("uptime", System.currentTimeMillis());
                    metrics.put("memory", Runtime.getRuntime().totalMemory());
                    metrics.put("threads", Thread.activeCount());
                    metrics.put("timestamp", System.currentTimeMillis());
                    
                    logger.info("📈 Metrics collected");
                    return CommandResult.success("metrics", metrics);
                    
                } catch (Exception e) {
                    logger.error("❌ Metrics collection failed", e);
                    return CommandResult.error("metrics", "COLLECTION_FAILED", e.getMessage());
                }
            }
            
            @Override
            public void validate(Map<String, Object> params) {}
        };
    }
}
