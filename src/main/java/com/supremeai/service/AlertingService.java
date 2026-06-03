package com.supremeai.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Alerting service for circuit breaker trips and high error rates.
 * Integrates with monitoring systems for real-time notifications.
 */
@Service
public class AlertingService {
    public AlertingService(ConfigService configService, boolean circuitBreakerAlertingEnabled, double errorRateThreshold, long alertCooldownMinutes) {
        this.configService = configService;
        this.circuitBreakerAlertingEnabled = circuitBreakerAlertingEnabled;
        this.errorRateThreshold = errorRateThreshold;
        this.alertCooldownMinutes = alertCooldownMinutes;
    }


    private static final Logger log = LoggerFactory.getLogger(AlertingService.class);
    private final MeterRegistry meterRegistry;
    private final Counter circuitBreakerTrips;
    private final Counter highErrorRateAlerts;
    private final Timer alertProcessingTimer;
    
    // Track recent alerts to avoid spam
    private final ConcurrentHashMap<String, Instant> recentAlerts = new ConcurrentHashMap<>();
    

    
    

    // Helper methods for dynamic settings with robust parsing/conversion
    private boolean isCircuitBreakerAlertingEnabled() {
        if (configService == null) {
            return circuitBreakerAlertingEnabled;
        }
        Object val = configService.getEffectiveSetting("alerting.circuit-breaker.enabled", circuitBreakerAlertingEnabled);
        if (val instanceof Boolean) {
            return (Boolean) val;
        } else if (val instanceof String) {
            return Boolean.parseBoolean((String) val);
        }
        return circuitBreakerAlertingEnabled;
    }

    private double getErrorRateThreshold() {
        if (configService == null) {
            return errorRateThreshold;
        }
        Object val = configService.getEffectiveSetting("alerting.error-rate.threshold", errorRateThreshold);
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        } else if (val instanceof String) {
            try {
                return Double.parseDouble((String) val);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return errorRateThreshold;
    }

    private long getAlertCooldownMinutes() {
        if (configService == null) {
            return alertCooldownMinutes;
        }
        Object val = configService.getEffectiveSetting("alerting.cooldown.minutes", alertCooldownMinutes);
        if (val instanceof Number) {
            return ((Number) val).longValue();
        } else if (val instanceof String) {
            try {
                return Long.parseLong((String) val);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return alertCooldownMinutes;
    }

    public AlertingService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.circuitBreakerTrips = Counter.builder("alerts.circuitbreaker.trips")
                .description("Number of circuit breaker trip alerts")
                .register(meterRegistry);
        this.highErrorRateAlerts = Counter.builder("alerts.errorrate.high")
                .description("Number of high error rate alerts")
                .register(meterRegistry);
        this.alertProcessingTimer = Timer.builder("alerts.processing.duration")
                .description("Time taken to process alerts")
                .register(meterRegistry);
    }

    /**
     * Send alert for circuit breaker trip.
     */
    public void sendCircuitBreakerAlert(String provider, String state, int failureCount) {
        if (!isCircuitBreakerAlertingEnabled()) {
            return;
        }
        
        String alertKey = "cb-" + provider + "-" + state;
        if (shouldSendAlert(alertKey)) {
            log.warn("CIRCUIT BREAKER ALERT: Provider {} is in {} state (failures: {})", 
                    provider, state, failureCount);
            
            circuitBreakerTrips.increment();
            recordAlert(alertKey);
            
            // In production, integrate with:
            // - Slack webhook
            // - PagerDuty
            // - Email notifications
            // - Opsgenie
        }
    }

    /**
     * Send alert for high error rate.
     */
    public void sendHighErrorRateAlert(String endpoint, double errorRate, int totalRequests) {
        if (errorRate < getErrorRateThreshold()) {
            return;
        }
        
        String alertKey = "errorrate-" + endpoint;
        if (shouldSendAlert(alertKey)) {
            log.error("HIGH ERROR RATE ALERT: Endpoint {} has error rate {}% ({} requests)", 
                    endpoint, String.format("%.2f", errorRate * 100), totalRequests);
            
            highErrorRateAlerts.increment();
            recordAlert(alertKey);
        }
    }

    /**
     * Check if alert should be sent based on cooldown.
     */
    private boolean shouldSendAlert(String alertKey) {
        Instant lastAlert = recentAlerts.get(alertKey);
        if (lastAlert == null) {
            return true;
        }
        
        return Instant.now().minusSeconds(getAlertCooldownMinutes() * 60).isAfter(lastAlert);
    }

    /**
     * Record alert timestamp.
     */
    private void recordAlert(String alertKey) {
        recentAlerts.put(alertKey, Instant.now());
    }

    /**
     * Get current alert statistics.
     */
    public AlertStats getAlertStats() {
        return AlertStats.builder()
                .circuitBreakerTrips((long) circuitBreakerTrips.count())
                .highErrorRateAlerts((long) highErrorRateAlerts.count())
                .recentAlertCount(recentAlerts.size())
                .build();
    }

    public static class AlertStats {
        private long circuitBreakerTrips;
        private long highErrorRateAlerts;
        private int recentAlertCount;

        public AlertStats() {}

        public AlertStats(long circuitBreakerTrips, long highErrorRateAlerts, int recentAlertCount) {
            this.circuitBreakerTrips = circuitBreakerTrips;
            this.highErrorRateAlerts = highErrorRateAlerts;
            this.recentAlertCount = recentAlertCount;
        }

        public long getCircuitBreakerTrips() { return circuitBreakerTrips; }
        public void setCircuitBreakerTrips(long circuitBreakerTrips) { this.circuitBreakerTrips = circuitBreakerTrips; }

        public long getHighErrorRateAlerts() { return highErrorRateAlerts; }
        public void setHighErrorRateAlerts(long highErrorRateAlerts) { this.highErrorRateAlerts = highErrorRateAlerts; }

        public int getRecentAlertCount() { return recentAlertCount; }
        public void setRecentAlertCount(int recentAlertCount) { this.recentAlertCount = recentAlertCount; }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private long circuitBreakerTrips;
            private long highErrorRateAlerts;
            private int recentAlertCount;

            public Builder circuitBreakerTrips(long val) { this.circuitBreakerTrips = val; return this; }
            public Builder highErrorRateAlerts(long val) { this.highErrorRateAlerts = val; return this; }
            public Builder recentAlertCount(int val) { this.recentAlertCount = val; return this; }

            public AlertStats build() {
                return new AlertStats(circuitBreakerTrips, highErrorRateAlerts, recentAlertCount);
            }
        }
    }
}

