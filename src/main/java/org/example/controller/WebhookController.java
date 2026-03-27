package org.example.controller;

import org.example.service.WebhookListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Phase 5: Webhook Controller for GitHub Events
 * 
 * Receives GitHub webhook events and processes them
 * Verifies HMAC signatures for security
 * 
 * Base URL: /webhook
 * 
 * Endpoints:
 * POST /webhook/github - Receive GitHub events
 */
@RestController
@RequestMapping("/webhook")
public class WebhookController {
    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);
    
    @Autowired
    private WebhookListener webhookListener;
    
    /**
     * POST /webhook/github
     * Receive GitHub webhook events
     * 
     * Headers:
     * - X-Hub-Signature-256: sha256=<hash>
     * - X-GitHub-Delivery: <delivery-id>
     * - X-GitHub-Event: <event-type>
     * 
     * Body: JSON payload from GitHub
     */
    @PostMapping("/github")
    public ResponseEntity<?> receiveGitHubWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType) {
        
        try {
            logger.info("🔔 GitHub webhook received: {} ({})", eventType, deliveryId);
            
            // Process webhook asynchronously
            webhookListener.handleWebhook(payload, signature != null ? signature : "", deliveryId != null ? deliveryId : "");
            
            // Respond immediately with 202 Accepted
            // GitHub expects response within 30 seconds
            return ResponseEntity.accepted().body(Map.of(
                "status", "accepted",
                "message", "Webhook queued for processing",
                "deliveryId", deliveryId,
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
     * Get webhook statistics (admin only)
     */
    @GetMapping("/github/stats")
    public ResponseEntity<?> getWebhookStats(
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        try {
            logger.debug("📊 Webhook stats requested");
            
            Map<String, Object> stats = webhookListener.getStats();
            
            return ResponseEntity.ok(Map.of(
                "webhook_stats", stats,
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            logger.error("❌ Stats retrieval failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
                ));
        }
    }
}
