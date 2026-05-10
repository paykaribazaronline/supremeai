package com.supremeai.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Alerting service for circuit breaker trips and high error rates.
 * Integrates with monitoring systems for real-time notifications.
 */
@Slf4j
@Service
public class AlertingService {

    private final MeterRegistry meterRegistry;
    private final Counter circuitBreakerTrips;
    private final Counter highErrorRateAlerts;
    private final Timer alertProcessingTimer;
    
    // Track recent alerts to avoid spam
    private final ConcurrentHashMap<String, Instant> recentAlerts = new ConcurrentHashMap<>();
    
    @Value("${alerting.circuit-breaker.enabled:true}")
    private boolean circuitBreakerAlertingEnabled;
    
    @Value("${alerting.error-rate.threshold:0.05}")
    private double errorRateThreshold;
    
    @Value("${alerting.cooldown.minutes:5}")
    private long alertCooldownMinutes;

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
        if (!circuitBreakerAlertingEnabled) {
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
        if (errorRate < errorRateThreshold) {
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
        
        return Instant.now().minusSeconds(alertCooldownMinutes * 60).isAfter(lastAlert);
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

    @lombok.Builder
    @lombok.Data
    public static class AlertStats {
        private long circuitBreakerTrips;
        private long highErrorRateAlerts;
        private int recentAlertCount;
    }
}
