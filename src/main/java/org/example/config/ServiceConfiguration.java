package org.example.config;

import org.example.model.SystemConfig;
import org.example.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * Phase 5: Spring Configuration for Services
 * 
 * Configures service beans and their dependencies
 * Enables dependency injection throughout REST layer
 * 
 * Initializes:
 * - Firebase (cloud connection)
 * - HybridDataCollector (Phase 3)
 * - DataCollectorService (Phase 4)
 * - WebhookListener (Phase 4)
 * - AdminMessagePusher (Phase 4)
 */
@Configuration
public class ServiceConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(ServiceConfiguration.class);
    
    /**
     * Initialize Firebase Service
     */
    @Bean
    public FirebaseService firebaseService() {
        logger.info("🔧 Initializing Firebase Service...");
        return new FirebaseService();
    }
    
    /**
     * Initialize System Configuration
     * Provides default system settings for Agent Orchestration
     */
    @Bean
    public SystemConfig systemConfig() {
        logger.info("🔧 Initializing System Configuration...");
        SystemConfig config = new SystemConfig();
        config.setAgentCount(5);
        config.setConsensusThreshold(0.6);
        config.setRotationEnabled(true);
        config.setVpnEnabled(false);
        return config;
    }
    
    /**
     * Initialize API Keys Map
     * Provides default empty map for API key management
     */
    @Bean
    public Map<String, String> apiKeys() {
        logger.info("🔧 Initializing API Keys Map...");
        return new HashMap<>();
    }
    
    /**
     * Initialize Quota Tracker
     */
    @Bean
    public QuotaTracker quotaTracker(FirebaseService firebase) {
        logger.info("🔧 Initializing Quota Tracker...");
        return new QuotaTracker(firebase);
    }
    
    /**
     * Initialize API Data Collector
     */
    @Bean
    public APIDataCollector apiDataCollector(QuotaTracker quota, FirebaseService firebase) {
        logger.info("🔧 Initializing API Data Collector...");
        return new APIDataCollector(quota, firebase);
    }
    
    /**
     * Initialize Browser Data Collector (Puppeteer fallback)
     */
    @Bean
    public BrowserDataCollector browserDataCollector(QuotaTracker quota, FirebaseService firebase) {
        logger.info("🔧 Initializing Browser Data Collector...");
        return new BrowserDataCollector(quota, firebase);
    }
    
    /**
     * Initialize Hybrid Data Collector
     * Combines API-first with browser fallback
     */
    @Bean
    public HybridDataCollector hybridDataCollector(
            APIDataCollector apiCollector,
            BrowserDataCollector browserCollector,
            QuotaTracker quota,
            FirebaseService firebase) {
        
        logger.info("🔧 Initializing Hybrid Data Collector...");
        return new HybridDataCollector(apiCollector, browserCollector, quota, firebase);
    }
    
    /**
     * Initialize Data Collector Service (REST API Layer)
     */
    @Bean
    public DataCollectorService dataCollectorService(HybridDataCollector hybridCollector) {
        logger.info("🔧 Initializing Data Collector Service...");
        return new DataCollectorService(hybridCollector);
    }
    
    /**
     * Initialize Webhook Listener (GitHub webhooks)
     */
    @Bean
    public WebhookListener webhookListener(DataCollectorService collectorService) {
        logger.info("🔧 Initializing Webhook Listener...");
        return new WebhookListener(collectorService);
    }
    
    /**
     * Initialize Admin Message Pusher (real-time updates to dashboard)
     */
    @Bean
    public AdminMessagePusher adminMessagePusher() {
        logger.info("🔧 Initializing Admin Message Pusher...");
        return new AdminMessagePusher();
    }
}
