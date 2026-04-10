package org.example.config;

import org.example.service.AIAPIService;
import org.example.service.ProviderRegistryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring Bean Configuration
 * Defines beans that require constructor parameters
 * 
 * NOTE: Uses @ConditionalOnMissingBean to allow test overrides
 */
@Configuration
public class BeanConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(BeanConfiguration.class);

    @Autowired(required = false)
    private ProviderRegistryService providerRegistryService;

    /**
     * Spring Bean definition for AIAPIService.
     * Initializes with default configuration and empty API keys.
     * API keys should be populated dynamically via the ProviderManagementService 
     * or admin dashboard.
     */
    @Bean
    @ConditionalOnMissingBean
    public AIAPIService aiAPIService() {
        try {
            logger.info("Initializing AIAPIService bean with default configuration");
            // Create with empty keys - they are loaded dynamically from Firebase
            AIAPIService service = new AIAPIService(java.util.Map.of());
            // Inject ProviderRegistryService so AIAPIService can look up API keys from DB
            if (providerRegistryService != null) {
                service.setProviderRegistryService(providerRegistryService);
                logger.info("✅ ProviderRegistryService injected into AIAPIService");
            } else {
                logger.warn("⚠️ ProviderRegistryService not available - DB key lookup disabled");
            }
            logger.info("✅ AIAPIService bean initialized successfully");
            return service;
        } catch (Exception e) {
            logger.error("❌ Failed to initialize AIAPIService bean", e);
            // Return null service with graceful degradation if needed
            throw new RuntimeException("AIAPIService initialization failed", e);
        }
    }
}
