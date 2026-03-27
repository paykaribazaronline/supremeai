package org.example.selfhealing;

import org.example.service.DataCollectorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

/**
 * Self-Healing Integration Example
 * 
 * Shows how to:
 * 1. Initialize self-healing on startup
 * 2. Register recovery handlers
 * 3. Wrap service methods with healing
 * 4. Monitor service health
 * 
 * Copy and adapt this for your services.
 */
@Component
public class SelfHealingIntegration {
    private static final Logger logger = LoggerFactory.getLogger(SelfHealingIntegration.class);
    
    @Autowired
    private SelfHealingService selfHealingService;
    
    @Autowired
    private DataCollectorService dataCollectorService;
    
    /**
     * Start self-healing system on application ready
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeSelfHealing() {
        logger.info("🔧 Initializing self-healing system");
        
        // Start the self-healing service
        selfHealingService.start();
        
        // Register recovery handlers for each service
        registerRecoveryHandlers();
        
        logger.info("✅ Self-healing system initialized");
    }
    
    /**
     * Register custom recovery handlers for services
     */
    private void registerRecoveryHandlers() {
        // Example: GitHub data collector recovery
        selfHealingService.registerRecoveryHandler("github-api", 
            new SelfHealingService.AutoRecoveryHandler() {
                @Override
                public boolean attemptRecovery() throws Exception {
                    logger.info("🔄 Attempting recovery for GitHub API collector");
                    
                    // Step 1: Clear circuit breaker if needed
                    CircuitBreaker breaker = selfHealingService.getOrCreateCircuitBreaker("github-api");
                    
                    // Step 2: Perform health check
                    HealthMonitor monitor = selfHealingService.getOrCreateHealthMonitor("github-api");
                    HealthMonitor.HealthMetrics metrics = monitor.getMetrics();
                    logger.info("  Current metrics: {}", metrics);
                    
                    // Step 3: Reset counters if recovery succeeds
                    logger.info("  Recovery completed for GitHub API");
                    return true;
                }
            });
        
        // Example: Vercel status collector recovery
        selfHealingService.registerRecoveryHandler("vercel-api",
            () -> {
                logger.info("🔄 Attempting recovery for Vercel API");
                // Similar recovery logic
                return true;
            });
        
        // Example: Firebase collector recovery
        selfHealingService.registerRecoveryHandler("firebase-db",
            () -> {
                logger.info("🔄 Attempting recovery for Firebase");
                // Reconnect to Firebase, refresh tokens, etc.
                return true;
            });
    }
    
    /**
     * Example: Wrap a service method with self-healing
     * 
     * Usage in DataCollectorService:
     * 
     * @Autowired
     * private SelfHealingService selfHealing;
     * 
     * public Map<String, Object> getGitHubDataWithHealing(String owner, String repo) 
     *         throws Exception {
     *     return selfHealing.executeWithHealing(
     *         "github-api",
     *         "fetch-" + owner + "-" + repo,
     *         () -> getGitHubData(owner, repo)
     *     );
     * }
     */
    public void showExampleUsage() {
        // Example 1: Simple operation with healing
        try {
            Map<String, Object> githubData = selfHealingService.executeWithHealing(
                "github-api",
                "fetch-repo-info",
                () -> {
                    // Your actual operation here
                    return dataCollectorService.getGitHubData("supremeai", "core");
                }
            );
            logger.info("✅ Successfully fetched GitHub data with healing");
        } catch (Exception e) {
            logger.error("❌ Failed to fetch GitHub data", e);
        }
        
        // Example 2: Get service diagnostics
        SelfHealingService.ServiceDiagnostics diag = 
            selfHealingService.getServiceDiagnostics("github-api");
        logger.info("Service diagnostics:\n{}", diag);
        
        // Example 3: Get system health report
        SelfHealingService.SystemHealthReport report = 
            selfHealingService.getSystemHealthReport();
        logger.info("System health:\n{}", report);
    }
}
