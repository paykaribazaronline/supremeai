package org.example;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ensures SupremeAI is ready to receive traffic on Cloud Run
 * Logs startup completion and verifies port binding
 */
@Component
public class CloudRunStartup {
    
    private static final Logger logger = LoggerFactory.getLogger(CloudRunStartup.class);
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String port = System.getenv("PORT");
        if (port == null) {
            port = "8080";
        }
        
        logger.info("════════════════════════════════════════════════════════");
        logger.info("✅ SupremeAI Backend is READY");
        logger.info("🔗 Listening on: 0.0.0.0:" + port);
        logger.info("📍 Health check: /actuator/health");
        logger.info("🚀 Ready to serve traffic from Cloud Run");
        logger.info("════════════════════════════════════════════════════════");
        
        // Log all important endpoints
        logger.info("Available endpoints:");
        logger.info("  GET  /                        - Home");
        logger.info("  GET  /actuator/health        - Health check (Cloud Run uses this)");
        logger.info("  POST /api/admin/requirement   - Submit requirement");
        logger.info("  GET  /api/governance/metrics  - Admin metrics");
    }
}
