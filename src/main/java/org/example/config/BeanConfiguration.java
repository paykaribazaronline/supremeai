package org.example.config;

import org.example.service.AIAPIService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring Bean Configuration
 * Defines beans that require constructor parameters
 * 
 * NOTE: Disabled for 'test' profile - uses TestBeansConfiguration instead
 */
@Configuration
@Profile("!test")
public class BeanConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(BeanConfiguration.class);

    /**
     * Spring Bean definition for AIAPIService.
     * Initializes with default configuration and empty API keys.
     * API keys should be populated dynamically via the ProviderManagementService 
     * or admin dashboard.
     */
    @Bean
    public AIAPIService aiAPIService() {
        try {
            logger.info("Initializing AIAPIService bean with default configuration");
            // Create with empty keys - they are loaded dynamically from Firebase
            AIAPIService service = new AIAPIService(java.util.Map.of());
            logger.info("✅ AIAPIService bean initialized successfully");
            return service;
        } catch (Exception e) {
            logger.error("❌ Failed to initialize AIAPIService bean", e);
            // Return null service with graceful degradation if needed
            throw new RuntimeException("AIAPIService initialization failed", e);
        }
    }
}
