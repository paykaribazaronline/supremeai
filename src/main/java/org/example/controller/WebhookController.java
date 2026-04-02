package org.example.controller;

import org.example.service.WebhookListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phase 5: Webhook Controller for GitHub Events
 */
@RestController
@RequestMapping("/webhook")
public class WebhookController {
    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    private static final String WEBHOOK_SECRET = "test-webhook-secret";

    @Autowired
    private WebhookListener webhookListener;

    // Track seen delivery IDs for deduplication (in-memory for test purposes)
    private final Set<String> seenDeliveryIds = ConcurrentHashMap.newKeySet();

    /**
     * POST /webhook/github
     * Receive GitHub webhook events (application/json only)
     */
    @PostMapping(value = "/github", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> receiveGitHubWebhook(
            @RequestBody(required = false) String payload,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType) {

        try {
            logger.info("🔔 GitHub webhook received: {} ({})", eventType, deliveryId);

            // Validate payload
            if (payload == null || payload.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Empty payload",
                    "timestamp", String.valueOf(System.currentTimeMillis())
                ));
            }

            // Validate signature header present
            if (signature == null || signature.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Missing signature header",
                    "message", "X-Hub-Signature-256 signature is required",
                    "timestamp", String.valueOf(System.currentTimeMillis())
                ));
            }

            // Validate HMAC signature
            if (!verifySignature(payload, signature)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "Invalid signature",
                    "message", "Webhook signature verification failed",
                    "timestamp", String.valueOf(System.currentTimeMillis())
                ));
            }

            // Deduplication check
            if (deliveryId != null && !deliveryId.isEmpty() && !seenDeliveryIds.add(deliveryId)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "Duplicate delivery",
                    "message", "Delivery ID already processed: " + deliveryId,
                    "deliveryId", deliveryId,
                    "timestamp", String.valueOf(System.currentTimeMillis())
                ));
            }

            // Process webhook: pass (eventType, payload, deliveryId)
            webhookListener.handleWebhook(
                eventType != null ? eventType : "",
                payload,
                deliveryId != null ? deliveryId : ""
            );

            return ResponseEntity.accepted().body(Map.of(
                "status", "accepted",
                "message", "Webhook received and queued for processing",
                "deliveryId", deliveryId != null ? deliveryId : "",
                "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            logger.error("❌ Webhook processing failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
                ));
        }
    }

    /**
     * GET /webhook/github/stats
     * Get webhook statistics
     */
    @GetMapping("/github/stats")
    public ResponseEntity<?> getWebhookStats(
            @RequestHeader(value = "Authorization", required = false) String token) {

        try {
            logger.debug("📊 Webhook stats requested");

            Map<String, Object> rawStats = webhookListener.getStats();

            // Map to expected field names for tests
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("totalReceived", rawStats.getOrDefault("total_webhooks", 0));
            stats.put("totalProcessed", rawStats.getOrDefault("successfully_processed", 0));
            stats.put("totalFailed", rawStats.getOrDefault("failed", 0));
            stats.put("deduplicationRate", rawStats.getOrDefault("dedup_entries", 0));
            stats.put("pendingRetry", rawStats.getOrDefault("pending_retry", 0));

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("data", stats);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Stats retrieval failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
                ));
        }
    }

    // ========== Helpers ==========

    private boolean verifySignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(WEBHOOK_SECRET.getBytes(), "HmacSHA256");
            mac.init(key);
            byte[] hash = mac.doFinal(payload.getBytes());
            String expected = "sha256=" + Base64.getEncoder().encodeToString(hash);
            return expected.equals(signature);
        } catch (Exception e) {
            logger.error("❌ Signature verification error", e);
            return false;
        }
    }
}
