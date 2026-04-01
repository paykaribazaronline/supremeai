package org.example.config;

import org.example.service.AIAPIService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring Bean Configuration for Test/Development
 * Provides stubs for dependencies needed for testing
 */
@Configuration
public class TestBeansConfiguration {
    
    /**
     * Provide AIAPIService bean for dependency injection
     */
    @Bean
    public AIAPIService aiAPIService() {
        Map<String, String> emptyKeys = new HashMap<>();
        return new AIAPIService(emptyKeys);
    }
}
