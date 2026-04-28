package com.supremeai.controller.webhook;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
public class GithubWebhookController {

    private final SimpMessagingTemplate messagingTemplate;
    
    @Value("${supremeai.webhook.secret:my-super-secret-key}")
    private String webhookSecret;

    public GithubWebhookController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/github")
    public ResponseEntity<String> handleGithubWebhook(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret,
            @RequestBody Map<String, Object> payload) {
        
        // Verify the secret token to prevent unauthorized access (Security check)
        if (webhookSecret != null && !webhookSecret.isEmpty() && !webhookSecret.equals(secret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid webhook secret!");
        }

        String status = (String) payload.getOrDefault("status", "unknown");
        String runId = (String) payload.getOrDefault("runId", "unknown");
        
        String message = status.equals("success") 
                ? "✅ New Deployment Successful! (Run ID: " + runId + ")"
                : "🚨 Deployment Pipeline FAILED! Please check logs. (Run ID: " + runId + ")";

        // Broadcast the notification to the frontend dashboard via WebSocket
        messagingTemplate.convertAndSend("/topic/notifications", Map.of(
                "type", "GITHUB_PIPELINE",
                "status", status,
                "message", message
        ));

        return ResponseEntity.ok("Notification broadcasted successfully!");
    }
}