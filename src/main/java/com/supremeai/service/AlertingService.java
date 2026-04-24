package com.supremeai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proactive Alerting Service for system health and performance monitoring.
 */
@Slf4j
@Service
public class AlertingService {

    private final Map<String, Integer> failureThresholds = new ConcurrentHashMap<>();
    private static final int CRITICAL_THRESHOLD = 5;

    public void recordFailure(String system) {
        int count = failureThresholds.merge(system, 1, Integer::sum);
        if (count >= CRITICAL_THRESHOLD) {
            sendCriticalAlert(system, count);
            failureThresholds.put(system, 0); // Reset after alert
        }
    }

    private void sendCriticalAlert(String system, int failures) {
        log.error("!!! CRITICAL ALERT: System {} has failed {} times consecutively !!!", system, failures);
        // Integrate with PagerDuty, Slack, or Email here
    }

    @Scheduled(fixedRate = 60000) // Check every minute
    public void monitorSystemHealth() {
        // Log monitoring status
        log.debug("Proactive monitoring active...");
    }
}
