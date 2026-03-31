package org.example.config;

import org.example.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
