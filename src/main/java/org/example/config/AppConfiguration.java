package org.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.util.*;

/**
 * Application configuration management
 * Load from application.properties with sensible defaults
 * Can be overridden by environment variables
 */
public class AppConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(AppConfiguration.class);
    private static final String CONFIG_FILE = "application.properties";
    private final Properties properties = new Properties();
    
    private static AppConfiguration instance;
    
    // Configuration keys with defaults
    private double consensusThreshold = 0.60;
    private int mediumApprovalTimeoutMin = 10;
    private int maxRetries = 3;
    private long initialBackoffMs = 500;
    private double backoffMultiplier = 2.0;
    private int apiTimeoutSeconds = 30;
    private int circuitBreakerFailureThreshold = 50;
    private int rateLimitPerMinute = 100;
    private boolean enableMetrics = true;
    private boolean enableAuditLog = true;
    private String logLevel = "INFO";
    
    private AppConfiguration() {
        loadConfiguration();
    }
    
    public static synchronized AppConfiguration getInstance() {
        if (instance == null) {
            instance = new AppConfiguration();
        }
        return instance;
    }
    
    /**
     * Load configuration from properties file and environment variables
     */
    private void loadConfiguration() {
        try {
            // Load from classpath
            InputStream input = getClass().getClassLoader()
                    .getResourceAsStream(CONFIG_FILE);
            if (input != null) {
                properties.load(input);
                input.close();
                logger.info("Loaded configuration from {}", CONFIG_FILE);
            } else {
                logger.warn("Configuration file not found: {}", CONFIG_FILE);
            }
        } catch (IOException e) {
            logger.error("Failed to load configuration file", e);
        }
        
        // Load all properties, allowing environment variable overrides
        loadProperty("consensus.threshold", Double.class, this::setConsensusThreshold);
        loadProperty("medium.approval.timeout.min", Integer.class, this::setMediumApprovalTimeoutMin);
        loadProperty("max.retries", Integer.class, this::setMaxRetries);
        loadProperty("initial.backoff.ms", Long.class, (v) -> setInitialBackoffMs(((Number)v).longValue()));
        loadProperty("backoff.multiplier", Double.class, this::setBackoffMultiplier);
        loadProperty("api.timeout.seconds", Integer.class, this::setApiTimeoutSeconds);
        loadProperty("circuit.breaker.failure.threshold", Integer.class, this::setCircuitBreakerFailureThreshold);
        loadProperty("rate.limit.per.minute", Integer.class, this::setRateLimitPerMinute);
        loadProperty("enable.metrics", Boolean.class, this::setEnableMetrics);
        loadProperty("enable.audit.log", Boolean.class, this::setEnableAuditLog);
        loadProperty("log.level", String.class, this::setLogLevel);
        
        logger.info("Configuration loaded: threshold={}, timeout={}min, retries={}, logLevel={}",
                consensusThreshold, mediumApprovalTimeoutMin, maxRetries, logLevel);
    }
    
    /**
     * Load a single property with environment variable override
     */
    private <T> void loadProperty(String key, Class<T> type, java.util.function.Consumer<T> setter) {
        try {
            // Try environment variable first (uppercase with underscores)
            String envKey = key.toUpperCase().replace(".", "_");
            String envValue = System.getenv(envKey);
            
            if (envValue != null && !envValue.isEmpty()) {
                logger.debug("Loading {} from environment: {}", key, envKey);
                T value = convertValue(envValue, type);
                setter.accept(value);
            } else if (properties.containsKey(key)) {
                logger.debug("Loading {} from properties file", key);
                T value = convertValue(properties.getProperty(key), type);
                setter.accept(value);
            }
        } catch (Exception e) {
            logger.warn("Failed to load property {}: {}", key, e.getMessage());
        }
    }
    
    /**
     * Convert string value to specified type
     */
    @SuppressWarnings("unchecked")
    private <T> T convertValue(String value, Class<T> type) {
        if (type == Double.class) {
            return (T) Double.valueOf(value);
        } else if (type == Integer.class) {
            return (T) Integer.valueOf(value);
        } else if (type == Long.class) {
            return (T) Long.valueOf(value);
        } else if (type == Boolean.class) {
            return (T) Boolean.valueOf(value);
        } else if (type == String.class) {
            return (T) value;
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }
    
    // ========== GETTERS & SETTERS ==========
    
    public double getConsensusThreshold() {
        return consensusThreshold;
    }
    
    public void setConsensusThreshold(double consensusThreshold) {
        this.consensusThreshold = consensusThreshold;
    }
    
    public int getMediumApprovalTimeoutMin() {
        return mediumApprovalTimeoutMin;
    }
    
    public void setMediumApprovalTimeoutMin(int mediumApprovalTimeoutMin) {
        this.mediumApprovalTimeoutMin = mediumApprovalTimeoutMin;
    }
    
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    public long getInitialBackoffMs() {
        return initialBackoffMs;
    }
    
    public void setInitialBackoffMs(long initialBackoffMs) {
        this.initialBackoffMs = initialBackoffMs;
    }
    
    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }
    
    public void setBackoffMultiplier(double backoffMultiplier) {
        this.backoffMultiplier = backoffMultiplier;
    }
    
    public int getApiTimeoutSeconds() {
        return apiTimeoutSeconds;
    }
    
    public void setApiTimeoutSeconds(int apiTimeoutSeconds) {
        this.apiTimeoutSeconds = apiTimeoutSeconds;
    }
    
    public int getCircuitBreakerFailureThreshold() {
        return circuitBreakerFailureThreshold;
    }
    
    public void setCircuitBreakerFailureThreshold(int circuitBreakerFailureThreshold) {
        this.circuitBreakerFailureThreshold = circuitBreakerFailureThreshold;
    }
    
    public int getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }
    
    public void setRateLimitPerMinute(int rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }
    
    public boolean isEnableMetrics() {
        return enableMetrics;
    }
    
    public void setEnableMetrics(boolean enableMetrics) {
        this.enableMetrics = enableMetrics;
    }
    
    public boolean isEnableAuditLog() {
        return enableAuditLog;
    }
    
    public void setEnableAuditLog(boolean enableAuditLog) {
        this.enableAuditLog = enableAuditLog;
    }
    
    public String getLogLevel() {
        return logLevel;
    }
    
    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }
}
