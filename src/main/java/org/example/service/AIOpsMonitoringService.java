package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AIOpsMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(AIOpsMonitoringService.class);

    @Autowired(required = false)
    private AIAPIService aiApiService;

    @Autowired(required = false)
    private QuotaService quotaService;

    @Autowired(required = false)
    private AlertingService alertingService;

    @Autowired(required = false)
    private WebSocketMetricsService webSocketMetricsService;

    @Autowired(required = false)
    private FirebaseService firebaseService;

    private final Map<String, Long> alertCooldown = new ConcurrentHashMap<>();
    private static final long ALERT_COOLDOWN_MS = 10 * 60 * 1000; // 10 minutes

    // Evaluate critical AI operations signals every minute.
    @Scheduled(fixedDelay = 60000, initialDelay = 45000)
    public void evaluateSignals() {
        if (aiApiService == null || alertingService == null) {
            return;
        }

        try {
            Map<String, Object> metrics = aiApiService.getOperationalMetrics();
            checkRateLimitPressure(metrics);
            checkCircuitPressure(metrics);
            checkQueuePressure(metrics);
            checkQuotaPressure();
        } catch (Exception ex) {
            logger.warn("AIOps monitoring evaluation failed: {}", ex.getMessage());
        }
    }

    // Drain dead-letter items from AIAPIService and persist to Firebase every 5 minutes.
    @Scheduled(fixedDelay = 300000, initialDelay = 120000)
    public void persistDeadLetterItems() {
        if (aiApiService == null || firebaseService == null) {
            return;
        }
        try {
            List<Map<String, Object>> items = aiApiService.drainDeadLetterItems();
            for (Map<String, Object> item : items) {
                firebaseService.saveDeadLetterItem(item);
            }
            if (!items.isEmpty()) {
                logger.info("Persisted {} dead-letter AI requests to Firebase.", items.size());
            }
        } catch (Exception ex) {
            logger.warn("Dead-letter persistence failed: {}", ex.getMessage());
        }
    }

    private void checkRateLimitPressure(Map<String, Object> metrics) {
        long total = asLong(metrics.get("totalRequests"));
        long rateLimit = asLong(metrics.get("rateLimitErrors"));
        if (total < 20) {
            return;
        }

        double ratio = (rateLimit * 100.0) / total;
        if (ratio >= 20.0) {
            sendCooldownAlert(
                "AI_RATE_LIMIT_PRESSURE",
                AlertingService.AlertSeverity.WARNING,
                "AI rate limit pressure high",
                String.format("Rate limit errors are %.2f%% of requests (%d/%d).", ratio, rateLimit, total)
            );
        }
    }

    private void checkCircuitPressure(Map<String, Object> metrics) {
        long skips = asLong(metrics.get("circuitOpenSkips"));
        if (skips >= 10) {
            sendCooldownAlert(
                "AI_CIRCUIT_OPEN",
                AlertingService.AlertSeverity.ERROR,
                "AI circuit breaker active",
                "Circuit breaker is skipping requests repeatedly. Fallback chain may be degraded."
            );
        }
    }

    private void checkQueuePressure(Map<String, Object> metrics) {
        long depth = asLong(metrics.get("queueDepth"));
        long drops = asLong(metrics.get("queueDrops"));

        if (depth >= 100) {
            sendCooldownAlert(
                "AI_QUEUE_DEPTH_HIGH",
                AlertingService.AlertSeverity.WARNING,
                "AI slow queue depth is high",
                "Slow queue depth exceeded 100. Consider reducing timeout or increasing free-provider diversity."
            );
        }

        if (drops > 0) {
            sendCooldownAlert(
                "AI_QUEUE_DROPS",
                AlertingService.AlertSeverity.ERROR,
                "AI slow queue drops detected",
                "Slow queue dropped requests. Increase queue capacity or lower incoming concurrency."
            );
        }
    }

    private void checkQuotaPressure() {
        if (quotaService == null) {
            return;
        }
        Map<String, Object> quotaSummary = quotaService.getQuotaSummary();
        int totalProviders = asInt(quotaSummary.get("totalProviders"));
        int criticalProviders = asInt(quotaSummary.get("criticalProviders"));
        int outOfQuotaProviders = asInt(quotaSummary.get("outOfQuotaProviders"));

        if (totalProviders <= 0) {
            return;
        }

        if (outOfQuotaProviders > 0) {
            sendCooldownAlert(
                "AI_PROVIDER_OUT_OF_QUOTA",
                AlertingService.AlertSeverity.CRITICAL,
                "One or more providers are out of quota",
                String.format("%d providers are out of quota.", outOfQuotaProviders)
            );
        }

        if ((criticalProviders * 100.0 / totalProviders) >= 50.0) {
            sendCooldownAlert(
                "AI_PROVIDER_QUOTA_80",
                AlertingService.AlertSeverity.WARNING,
                "Provider quota usage exceeded 80%",
                String.format("%d/%d providers are in CRITICAL quota state.", criticalProviders, totalProviders)
            );
        }
    }

    private void sendCooldownAlert(String key,
                                   AlertingService.AlertSeverity severity,
                                   String title,
                                   String message) {
        long now = System.currentTimeMillis();
        Long last = alertCooldown.get(key);
        if (last != null && now - last < ALERT_COOLDOWN_MS) {
            return;
        }

        alertCooldown.put(key, now);
        AlertingService.Alert alert = alertingService.createAlert(severity, title, message);
        if (webSocketMetricsService != null) {
            webSocketMetricsService.broadcastAlert(alert);
        }
    }

    private long asLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        return 0L;
    }

    private int asInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }
}
