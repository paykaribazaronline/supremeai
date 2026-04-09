package org.example.config;

import org.example.service.AIAPIService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Bean Configuration
 * Defines beans that require constructor parameters 
 */
@Configuration
public class BeanConfiguration {

    /**
     * Spring Bean definition for AIAPIService.
     * Initializes with default configuration and empty API keys.
     * API keys should be populated dynamically via the ProviderManagementService 
     * or admin dashboard.
     */
    @Bean
    public AIAPIService aiAPIService() {
        // Create with empty keys - they are loaded dynamically from Firebase
        return new AIAPIService(java.util.Map.of());
    }
}
