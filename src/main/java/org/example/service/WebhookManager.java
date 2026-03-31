package org.example.service;

import org.example.model.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Webhook Manager Service
 * Handles webhook registration, triggering, and retry logic
 */
@Service
public class WebhookManager {
    private static final Logger logger = LoggerFactory.getLogger(WebhookManager.class);
    
    private final Map<String, Webhook> webhooks = new ConcurrentHashMap<>();
    private final HttpClient httpClient;
    private final ScheduledExecutorService retryExecutor;
    
    // Retry configuration
    private static final int MAX_RETRIES = 5;
    private static final long[] RETRY_DELAYS_MS = {1000, 2000, 4000, 8000, 16000}; // Exponential backoff
    private static final int TIMEOUT_SECONDS = 10;
    
    public WebhookManager() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .build();
        this.retryExecutor = Executors.newScheduledThreadPool(4);
        logger.info("WebhookManager initialized");
    }
    
    /**
     * Register a webhook for an event
     */
    public String registerWebhook(String projectId, String url, String[] events, String secretKey) {
        String webhookId = UUID.randomUUID().toString();
        
        Webhook webhook = new Webhook();
        webhook.setId(webhookId);
        webhook.setProjectId(projectId);
        webhook.setUrl(url);
        webhook.setEvents(events);
        webhook.setSecretKey(secretKey);
        webhook.setCreatedAt(Instant.now());
        webhook.setActive(true);
        webhook.setRetryAttempts(0);
        
        webhooks.put(webhookId, webhook);
        logger.info("Webhook registered: {} for project: {} with events: {}",
                  webhookId, projectId, String.join(",", events));
        
        return webhookId;
    }
    
    /**
     * Send webhook event with automatic retry
     */
    public void triggerWebhook(String webhookId, String eventType, Map<String, Object> payload) {
        Webhook webhook = webhooks.get(webhookId);
        
        if (webhook == null) {
            logger.warn("Webhook not found: {}", webhookId);
            return;
        }
        
        if (!webhook.isActive()) {
            logger.debug("Webhook inactive: {}", webhookId);
            return;
        }
        
        // Check if webhook is subscribed to this event
        if (!Arrays.asList(webhook.getEvents()).contains(eventType)) {
            logger.debug("Webhook {} not subscribed to event: {}", webhookId, eventType);
            return;
        }
        
        // Send webhook with retry logic
        sendWebhookWithRetry(webhook, eventType, payload, 0);
    }
    
    /**
     * Send webhook with exponential backoff retry
     */
    private void sendWebhookWithRetry(Webhook webhook, String eventType,
                                      Map<String, Object> payload, int attemptNumber) {
        try {
            boolean success = sendWebhookRequest(webhook, eventType, payload);
            
            if (success) {
                logger.info("Webhook delivery successful: {} attempt: {}",
                           webhook.getId(), attemptNumber + 1);
                recordWebhookDelivery(webhook, eventType, true, null);
            } else if (attemptNumber < MAX_RETRIES) {
                // Schedule retry with exponential backoff
                long delayMs = RETRY_DELAYS_MS[attemptNumber];
                logger.warn("Webhook delivery failed, retrying in {} ms: {}",
                           delayMs, webhook.getId());
                
                retryExecutor.schedule(
                    () -> sendWebhookWithRetry(webhook, eventType, payload, attemptNumber + 1),
                    delayMs,
                    TimeUnit.MILLISECONDS
                );
            } else {
                logger.error("Webhook delivery failed after {} attempts: {}",
                            MAX_RETRIES, webhook.getId());
                recordWebhookDelivery(webhook, eventType, false, "Max retries exceeded");
            }
        } catch (Exception e) {
            logger.error("Error sending webhook: " + webhook.getId(), e);
            if (attemptNumber < MAX_RETRIES) {
                long delayMs = RETRY_DELAYS_MS[attemptNumber];
                retryExecutor.schedule(
                    () -> sendWebhookWithRetry(webhook, eventType, payload, attemptNumber + 1),
                    delayMs,
                    TimeUnit.MILLISECONDS
                );
            }
        }
    }
    
    /**
     * Send HTTP request to webhook URL
     */
    private boolean sendWebhookRequest(Webhook webhook, String eventType,
                                       Map<String, Object> payload) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("event", eventType);
        body.put("timestamp", Instant.now().toString());
        body.put("webhookId", webhook.getId());
        body.put("payload", payload);
        
        String jsonBody = convertToJson(body);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(webhook.getUrl()))
            .header("Content-Type", "application/json")
            .header("X-Webhook-ID", webhook.getId())
            .header("X-Event-Type", eventType)
            .header("X-Timestamp", Instant.now().toString())
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }
    
    /**
     * Test webhook with test payload
     */
    public void testWebhook(String webhookId) {
        Webhook webhook = webhooks.get(webhookId);
        
        if (webhook == null) {
            logger.warn("Webhook not found for testing: {}", webhookId);
            return;
        }
        
        Map<String, Object> testPayload = Map.of(
            "test", true,
            "timestamp", Instant.now().toString()
        );
        
        triggerWebhook(webhookId, "test.event", testPayload);
        logger.info("Test webhook sent: {}", webhookId);
    }
    
    /**
     * Get webhook
     */
    public Webhook getWebhook(String webhookId) {
        return webhooks.get(webhookId);
    }
    
    /**
     * List webhooks for project
     */
    public List<Webhook> listWebhooks(String projectId) {
        return webhooks.values().stream()
            .filter(w -> w.getProjectId().equals(projectId))
            .toList();
    }
    
    /**
     * Delete webhook
     */
    public void deleteWebhook(String webhookId) {
        webhooks.remove(webhookId);
        logger.info("Webhook deleted: {}", webhookId);
    }
    
    /**
     * Deactivate webhook
     */
    public void deactivateWebhook(String webhookId) {
        Webhook webhook = webhooks.get(webhookId);
        if (webhook != null) {
            webhook.setActive(false);
            logger.info("Webhook deactivated: {}", webhookId);
        }
    }
    
    /**
     * Record webhook delivery attempt
     */
    private void recordWebhookDelivery(Webhook webhook, String eventType,
                                      boolean success, String error) {
        webhook.setLastDeliveryAt(Instant.now());
        webhook.setLastDeliveryStatus(success ? "success" : "failed");
        webhook.setRetryAttempts(webhook.getRetryAttempts() + 1);
        
        if (success) {
            webhook.setSuccessfulDeliveries(webhook.getSuccessfulDeliveries() + 1);
        } else {
            webhook.setFailedDeliveries(webhook.getFailedDeliveries() + 1);
        }
        
        logger.debug("Webhook delivery recorded: {} status: {} error: {}",
                    webhook.getId(), success ? "success" : "failed", error);
    }
    
    /**
     * Convert object to JSON string (simplified)
     */
    private String convertToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else if (value instanceof Map) {
                sb.append(convertToJson((Map<String, Object>) value));
            } else {
                sb.append(value);
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
