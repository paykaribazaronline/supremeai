package org.example.config;

import org.example.security.EncryptionService;
import org.example.security.RateLimitingService;
import org.example.validation.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Security Configuration
 * Configures all security-related beans
 */
@Configuration
public class SecurityConfig {
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    
    @Value("${supremeai.ratelimit.requests-per-minute:1000}")
    private int requestsPerMinute;
    
    @Value("${supremeai.ratelimit.requests-per-hour:10000}")
    private int requestsPerHour;
    
    @Value("${supremeai.encryption.key:}")
    private String encryptionKey;
    
    /**
     * Rate limiting service bean
     */
    @Bean
    public RateLimitingService rateLimitingService() {
        logger.info("Initializing RateLimitingService: {} req/min, {} req/hour",
                   requestsPerMinute, requestsPerHour);
        return new RateLimitingService(requestsPerMinute, requestsPerHour);
    }
    
    /**
     * Encryption service bean
     */
    @Bean
    public EncryptionService encryptionService() {
        logger.info("Initializing EncryptionService");
        return new EncryptionService(encryptionKey);
    }
    
    /**
     * Input sanitizer bean
     */
    @Bean
    public InputSanitizer inputSanitizer() {
        logger.info("Initializing InputSanitizer");
        return new InputSanitizer();
    }
}
